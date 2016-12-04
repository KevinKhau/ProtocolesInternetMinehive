package network;

import static util.Message.validArguments;
import static util.PlayersManager.getPlayersFromXML;
import static util.PlayersManager.writePlayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import data.HostData;
import data.Player;
import util.Message;
import util.Params;
import util.PlayersManager;
import util.StringUtil;
import util.TFServerSocket;
import util.TFSocket;

/**
 * Reste attentif à la connexion de nouveaux clients ou hôtes
 */
public class Server extends Entity {

	public static final String NAME = "Server";

	InetAddress serverIP;
	/** Port de réception pour les clients */
	final int serverPort_Client = 5555;
	/** Port de réception pour les hôtes */
	final int serverPort_Host = 7777;

	public static final String ALL = "ALL";
	public static final int MAX_ONLINE = 110;

	Map<String, Player> users = getPlayersFromXML();
	Map<Player, ClientHandler> available = new ConcurrentHashMap<>();

	Map<String, HostData> hostsDataHelper = new ConcurrentHashMap<>();
	Map<HostData, SenderHandler> hosts = new ConcurrentHashMap<>();

	Map<Player, HostData> inGame = new ConcurrentHashMap<>();
	
	Map<String, Player> kickedHelper = new ConcurrentHashMap<>();
	
	public static final int ACTIVE_DELAY = 300000;

	public static void main(String[] args) {
		new Server();
	}

	public Server() {
		super(NAME);
		try {
			serverIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			System.err.println("IP du serveur invalide.");
			System.exit(1);
		}
		new Thread(new ClientListener(serverIP, serverPort_Client)).start();
		new Thread(new HostListener(serverIP, serverPort_Host)).start();
	}

	public class ClientListener extends Listener {
		public ClientListener(InetAddress IP, int port) {
			super(IP, port);
		}
		@Override
		protected void listen(TFServerSocket serverSocket) throws IOException {
			new Thread(new ClientHandler(serverSocket.accept())).start();
		}
	}
	public class HostListener extends Listener {
		public HostListener(InetAddress IP, int port) {
			super(IP, port);
		}
		@Override
		protected void listen(TFServerSocket serverSocket) throws IOException {
			new Thread(new HostHandler(serverSocket.accept())).start();
		}
	}

	public boolean addAvailable(Player player, ClientHandler handler) {
		if (!isFull() || available.containsKey(player)) {
			ClientHandler h = available.get(player);
			if (h != null) {
				h.kick("You logged in from another client!");
			}
			available.put(player, handler);
			return true;
		}
		return false;
	}

	public void addInGame(Player player, HostData hostData) {
		inGame.put(player, hostData);
	}

	public boolean isFull() {
		return available.size() + inGame.size() >= MAX_ONLINE;
	}

	/**
	 * Obtenir le Handler gérant la connexion d'un client disponible à partir
	 * d'un nom d'utilisateur
	 * 
	 * @param username
	 */
	public ClientHandler getHandler(String username) {
		if (username == null) {
			System.err.println("Message anormal reçu. Nom de joueur requis.");
			return null;
		}
		Player p = users.get(username);
		if (p == null) {
			System.err.println("Utilisateur '" + username + "'inexistant.");
			return null;
		}
		ClientHandler ch = available.get(p);
		if (ch == null) {
			System.err.println("Utilisateur '" + username + "' non connecté.");
			return null;
		}
		return ch;
	}

	/** Server <- Client */
	private class ClientHandler extends SenderHandler {

		/**
		 * Correspond à l'entityData casté. Initialisé lors de identification()
		 */
		Player player;

		public ClientHandler(TFSocket socket) {
			super(socket, Player.NAME);
		}

		/**
		 * À REGI : identifie un joueur déjà existant, l'enregistre, ou invite à
		 * réessayer jusqu'à validation. Gère les réponses {@link Message#IDOK},
		 * {@link Message#IDNO}, {@link Message#IDIG}.
		 * 
		 * @return Un joueur identifié ou créé avec succès, ou tentatives
		 *         récursives sinon.
		 * @throws IOException
		 */
		@Override
		public void identification() throws IOException {
			if (!running) {
				disconnect();
				return;
			}
			Message message = socket.receive();
			if (message.getType().equals(Message.LEAV)) {
				System.out.println("Fin de la connexion avec " + socket.getRemoteSocketAddress() + ".");
				disconnect();
				return;
			}
			/* Serveur saturé */
			if (isFull()) {
				socket.send(Message.IDNO, null, "Le serveur est plein. Réessayez ultérieurement.");
				identification();
				return;
			}
			/* Anomalies */
			if (!message.getType().equals(Message.REGI)) {
				socket.send(Message.IDKS, null, "Vous devez d'abord vous identifier : REGI Username Password");
				identification();
				return;
			}
			/* Mauvais nombre d'arguments */
			if (!validArguments(message)) {
				socket.send(Message.IDNO, null, "Identifiant et/ou Mot de passe manquant");
				identification();
				return;
			}
			String username = message.getArg(0);
			String password = message.getArg(1);
			senderData = users.get(username);
			/* Première fois */
			if (senderData == null) {
				player = new Player(username, password, Player.INITIAL_POINTS);
				users.put(username, player);
				writePlayer(player);
				senderName = username;
				socket.send(Message.IDOK, null, "Bienvenue " + username + " !");
				return;
			}
			player = (Player) senderData;
			/* Mauvais mot de passe */
			if (!player.password.equals(password)) {
				socket.send(Message.IDNO, null, "Mauvais mot de passe");
				identification();
				return;
			}
			/* Déjà connecté */
			if (available.containsKey(player)) {
				System.out.println("Connection override de " + player.username + ".");
				socket.send(Message.IDOK, null, "Bon retour " + username + " !");
				return;
			}
			/* Déjà en partie */
			HostData hd = inGame.get(player);
			if (hd != null) {
				socket.send(Message.IDIG, new String[] { hd.getIP(), String.valueOf(hd.getPort()) },
						"Finissez votre partie en cours !");
				kick("Finish your on-going match!");
				return;
			}
			/* Connexion classique */
			senderName = username;
			socket.send(Message.IDOK, null, "Bonjour " + username + " !");
			return;
		}

		@Override
		protected void addEntityData() {
			addAvailable(player, this);
			System.out.println(
					"Utilisateur '" + player.username + "' identifié depuis " + socket.getRemoteSocketAddress() + ".");
		}

		@Override
		protected void handleMessage(Message reception) {
			switch (reception.getType()) {
			case Message.REGI:
				socket.send(Message.IDKS, null, "Vous êtes déjà connecté !");
				break;
			case Message.LSMA:
				socket.send(Message.LMNB, new String[] { String.valueOf(hosts.size()) });
				hosts.values().forEach(handler -> handler.socket.send(Message.RQDT, new String[] { player.username }));
				break;
			case Message.LSAV:
				sendAvailable(reception);
				break;
			case Message.LSUS:
				sendUsers(reception);
				break;
			case Message.NWMA:
				createMatch(reception);
				break;
			case Message.LEAV:
				System.out.println(
						"Déconnexion de l'utilisateur " + player.username + ", " + socket.getRemoteSocketAddress());
				disconnect();
				break;
			case Message.IDKC:
				System.out.println(senderName + " n'a pas reconnu une commande.");
			default:
				unknownMessage();
				break;
			}
		}

		private synchronized void sendAvailable(Message msg) {
			socket.send(Message.LANB, new String[] { String.valueOf(available.size()) });
			available.keySet().forEach(
					p -> socket.send(Message.AVAI, new String[] { p.username, String.valueOf(p.totalPoints) }));
		}

		private synchronized void sendUsers(Message msg) {
			socket.send(Message.LUNB, new String[] { String.valueOf(users.size()) });
			users.values().forEach(p -> {
				String comment = null;
				if (!p.permission) {
					comment = "Created a still on-going match.";
				}
				socket.send(Message.USER, new String[] { p.username, String.valueOf(p.totalPoints) }, comment);
			});
		}

		private void createMatch(Message msg) {
			if (hosts.size() >= 10) {
				socket.send(Message.FULL, null, "Trop de parties en cours. Réessayez ultérieurement.");
				return;
			}
			HostData hd = null;
			try {
				hd = new HostData(player.username, StringUtil.randomPassword());
			} catch (IOException e) {
				socket.send(Message.NWNO, null, e.getMessage());
				return;
			}
			if (!player.permission) {
				socket.send(Message.NWNO, null, "Vous avez déjà créé une partie. Veuillez la terminer.");
				return;
			}
			System.out.println(hd);
			hostsDataHelper.put(hd.name, hd);
			try {
				launchHost(hd);
			} catch (Exception e) {
				e.printStackTrace();
				socket.send(Message.NWNO, null, "Problème technique : Le serveur n'a pas pu lancer un hôte de partie ; " + e.getMessage());
				hostsDataHelper.remove(hd.name);
				return;
			}
			/* TODO Envoyer NWOK seulement après identification de l'hôte */
			String[] sendArgs = new String[] { hd.getIP(), String.valueOf(hd.getPort()) };
			socket.send(Message.NWOK, sendArgs, "Votre partie a été créée.");
			player.permission = false;

			/* Aucun invité */
			if (msg.getArgs() == null) {
				return;
			}

			/* ALL : inviter tous les joueurs disponibles */
			String arg1 = msg.getArg(0);
			if (arg1 != null && arg1.equals(ALL)) {
				available.values().forEach(h -> h.socket.send(Message.NWOK, sendArgs, player.username + " vous défie !"));
				return;
			}

			/* Liste spécifique de joueurs invités */
			Arrays.asList(msg.getArgs()).forEach(playerName -> {
				Player p = users.get(playerName);
				if (p != null) {
					ClientHandler h = available.get(p);
					if (h != null) {
						h.socket.send(Message.NWOK, sendArgs, player.username + " vous défie !");
					}
				}
			});
		}

		/**
		 * Lance un nouveau processus Hôte indépendant.
		 * @param hostData
		 * @throws IOException 
		 */
		private void launchHost(HostData hostData) throws NullPointerException, IOException {
			String dirPath = null;
			URL[] dirPaths = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
			if (dirPaths == null || dirPaths.length == 0) {
				dirPath = Params.BIN.toString();
			} else {
				dirPath = dirPaths[0].getPath();
				dirPath = dirPath.substring(1, dirPath.length());
			}
			Path hostJarPath;
			String hJPString = null;
			try {
				hostJarPath = Paths.get(dirPath, Host.JAR_NAME);
				hJPString = hostJarPath.toString();
				if (!hostJarPath.toFile().exists()) {
					throw new FileNotFoundException("Unresolved Host path : " + hJPString); 
				}
			} catch (InvalidPathException e) {
				File f = new File(Params.BIN.toString() + "/" + Host.JAR_NAME);
				hJPString = f.getAbsolutePath();
				if (!f.exists()) {
					throw new FileNotFoundException("Unresolved Host path : " + hJPString);
				}
			}
			String args = String.join(" ", serverIP.getHostAddress(), String.valueOf(serverPort_Host),
					hostData.name, hostData.IP.getHostAddress(), String.valueOf(hostData.port), hostData.password);
			String cmd = "java -jar " + hJPString + " " + args;
			System.out.println(cmd);
			if (!Params.DEBUG_HOST) {
				ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
				pb.redirectInput(hostData.inLog.toFile());
				pb.redirectOutput(hostData.outLog.toFile());
				pb.redirectError(hostData.errorLog.toFile());
				pb.start();
			} else {
				System.out.println(hJPString);
				System.out.println(args);
				System.out.println(cmd);
				System.out.println("DEBUG HOST MODE. Please launch the host manually with the above information.");
			}
			System.out.println("Host should be launched.");
		}

		/**
		 * Le serveur interrompt la connexion avec ce client. Player ne doit pas
		 * être null, et donc la connexion doit déjà avoir été établie.
		 */
		public void kick(String comment) {
			socket.send(Message.KICK, null, comment);
			disconnect();
			kickedHelper.put(senderData.name, (Player) senderData);
		}

		@Override
		protected void removeEntityData() {
			if (senderData != null) {
				if (kickedHelper.remove(senderData.name) == null) {
					available.remove(senderData);
				}
			}
		}

		@Override
		protected void unknownMessage() {
			socket.send(Message.IDKS, null, "Commande inconnue ou pas encore implémentée");
		}
	}

	/** Server <- Host */
	private class HostHandler extends SenderHandler {

		public HostHandler(TFSocket socket) {
			super(socket, HostData.NAME);
		}

		@Override
		protected void identification() throws IOException {
			if (!running) {
				disconnect();
				return;
			}

			Message message = socket.receive();
			if (!message.getType().equals(Message.LOGI)) {
				unknownMessage();
				disconnect();
				return;
			}
			String matchName = message.getArg(0);
			senderData = hostsDataHelper.get(matchName);
			if (senderData == null) {
				socket.send(Message.IDNO, null, "Unresolved host name.");
				disconnect();
				return;
			} else {
				String password = message.getArg(1);
				if (password == null) {
					socket.send(Message.IDOK, null,
							"No password provided. You are so LUCKY I am merciful enough to let your obsolete host pass through!");
					/* FUTURE Refuse connections which do not provide password */
//					socket.send(Message.IDNO, null, "No password provided.");
				} else if (!password.equals(((HostData) senderData).password)) {
					socket.send(Message.IDNO, null, "Wrong password");
				} else {
					socket.send(Message.IDOK, null, "C'est parti, " + senderData.name + " !");
				}
				return;
			}
		}

		@Override
		protected void handleMessage(Message reception) {
			ClientHandler ch;
			switch (reception.getType()) {
			case Message.LOGI:
				socket.send("IDKS", null, "Vous êtes déjà connecté !");
				break;
			case Message.SDDT:
				ch = getHandler(reception.getArg(0));
				if (ch != null) {
					String[] args = reception.getArgs();
					String[] transfer = Arrays.copyOfRange(args, 1, args.length);
					ch.socket.send(Message.MATC, transfer);
				}
				break;
			case Message.PLIN:
				String username = reception.getArg(1);
				if (username == null) {
					System.err.println("Message anormale de l'hôte : Nom d'utilisateur non défini.");
					break;
				}
				String password = reception.getArg(2);
				if (password == null) {
					System.err.println(
							"Message anormale de l'hôte : Mot de passe non défini. Rappel : PLIN#MatchName#Username#Password");
					break;
				}
				Player p = getPlayer(username);
				if (p == null) {
					break;
				}
				if (!p.password.equals(password)) {
					socket.send(Message.PLNO, new String[] { username }, "Mauvais mot de passe.");
					break;
				}
				if (inGame.containsKey(p)) {
					socket.send(Message.PLNO, new String[] { username },
							username + " a déjà une partie en cours, qu'il doit finir !");
					break;
				}
				socket.send(Message.PLOK, new String[] { username, String.valueOf(p.totalPoints) });
				inGame.put(p, (HostData) senderData);
				ch = available.remove(p);
				if (ch != null) {
					ch.kick("Have fun playing Minehive. Come back later if you still want to play!");
				}
				break;
			case Message.SCPS:
				Player p2 = getPlayer(reception.getArg(0));
				if (p2 != null) {
					inGame.remove(p2);
					p2.totalPoints = reception.getArgAsInt(1);
					PlayersManager.writePlayer(p2);
				}
				break;
			case Message.ENDS:
				disconnect();
				break;
			case Message.IDKH:
				System.out.println(senderName + " n'a pas reconnu une commande.");
				break;
			default:
				unknownMessage();
				break;
			}
		}

		private Player getPlayer(String username) {
			if (username == null) {
				System.err.println("Message anormale de l'hôte : Nom d'utilisateur non défini.");
				return null;
			}
			Player p = users.get(username);
			if (p == null) {
				socket.send(Message.PLNO, new String[] { username }, "Utilisateur inexistant.");
				return null;
			}
			return p;
		}

		@Override
		protected void addEntityData() {
			hosts.put((HostData) senderData, this);
		}

		@Override
		protected void removeEntityData() {
			if (senderData != null) {
				hostsDataHelper.remove(senderData.name);
				hosts.remove(senderData);
				String playerCreator = ((HostData) senderData).creator; 
				Player creator = users.get(playerCreator);
				if (creator != null) {
					creator.permission = true;
				}
			}
		}

		@Override
		protected void unknownMessage() {
			socket.send(Message.IDKS, null, "Commande inconnue ou pas encore implémentée");
		}
		
	}

}
