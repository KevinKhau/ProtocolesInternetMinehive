package network;

import static util.Message.validArguments;
import static util.PlayersManager.getPlayersFromXML;
import static util.PlayersManager.writePlayer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import data.HostData;
import data.Player;
import util.Message;
import util.Params;
import util.PlayersManager;
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
				h.kick();
			}
			/* TODO Thread-safe. Risque de remove par SenderHandler#disconnect() après ce put */
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
				identification();
				return;
			}
			/* Connexion classique */
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
			users.values().forEach(
					p -> socket.send(Message.USER, new String[] { p.username, String.valueOf(p.totalPoints) }));
		}

		private void createMatch(Message msg) {
			if (hosts.size() >= 10) {
				socket.send(Message.FULL, null, "Trop de parties en cours. Réessayez ultérieurement.");
				return;
			}
			HostData hd = null;
			try {
				hd = new HostData(player.name);
			} catch (IOException e) {
				socket.send(Message.NWNO, null, e.getMessage());
			}
			if (!senderData.permission) {
				socket.send(Message.NWNO, null, "Vous avez déjà créé une partie. Veuillez attendre qu'elle se termine.");
			}
			hostsDataHelper.put(hd.name, hd);
			try {
				if (!Params.DEBUG_HOST) {
					launchHost(hd);
				}
			} catch (Exception e) {
				e.printStackTrace();
				socket.send(Message.NWNO, null, "Problème technique : Le serveur n'a pas pu lancer un hôte de partie");
				hostsDataHelper.remove(hd.name);
				return;
			}
			String[] sendArgs = new String[] { hd.getIP(), String.valueOf(hd.getPort()) };
			socket.send(Message.NWOK, sendArgs, "Votre partie a été créée.");
			senderData.permission = false;

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
		 */
		private void launchHost(HostData hostData) throws NullPointerException {
			try {
				String dirPath = null;
				URL[] dirPaths = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
				if (dirPaths == null || dirPaths.length == 0) {
					dirPath = "DIR_BIN";
				} else {
					dirPath = dirPaths[0].getPath();
					dirPath = dirPath.substring(1, dirPath.length());
				}
				Path hostJarPath = Paths.get(dirPath, Host.JAR_NAME);
				String args = String.join(" ", serverIP.getHostAddress(), String.valueOf(serverPort_Host),
						hostData.name, hostData.IP.getHostAddress(), String.valueOf(hostData.port));
				System.out.println(args);
				Runtime.getRuntime().exec("java -jar " + hostJarPath.toString() + " " + args);
				System.out.println("Host should be launched.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Le serveur interrompt la connexion avec ce client. Player ne doit pas
		 * être null, et donc la connexion doit déjà avoir été établie.
		 */
		public void kick() {
			socket.send(Message.KICK);
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
			if (senderData == null || !Params.DEBUG_HOST) {
				socket.send(Message.IDNO, null, "Nom de partie inconnu.");
				disconnect();
				return;
			} else {
				socket.send(Message.IDOK, null, "C'est parti, " + senderData.name + " !");
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
					ch.disconnect();
				}
				break;
			case Message.SCPS:
				Player p2 = getPlayer(reception.getArg(0));
				inGame.remove(p2);
				p2.totalPoints = reception.getArgAsInt(1);
				PlayersManager.writePlayer(p2);
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
				hosts.remove(senderData);
				hostsDataHelper.remove(senderData.name);
			}
		}

		@Override
		protected void unknownMessage() {
			socket.send(Message.IDKS, null, "Commande inconnue ou pas encore implémentée");
		}
		
		@Override
		protected void disconnect() {
			if (senderData != null) {
				hostsDataHelper.remove(senderData.name);
				hosts.remove(senderData);
				String playerCreator = ((HostData) senderData).creator; 
				Player creator = users.get(playerCreator);
				if (creator != null) {
					creator.permission = true;
				}
				
			}
			running = false;
			removeEntityData();
			socket.close();
		}

	}

}
