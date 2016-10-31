package network;

import static util.Message.validArguments;
import static util.PlayersManager.getPlayersFromXML;
import static util.PlayersManager.writePlayer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import data.HostData;
import data.Player;
import util.Message;
import util.MyBufferedReader;
import util.MyPrintWriter;

/**
 * Reste attentif à la connexion de nouveaux clients ou hôtes
 */
public class Server {

	InetAddress serverIP;
	final int serverPort = 5555;

	Map<String, Player> users = getPlayersFromXML();

	public static final String ALL = "ALL";
	public static final int MAX_ONLINE = 110;
	Map<Player, ClientHandler> available = new ConcurrentHashMap<>();
	Map<Player, HostData> inGame = new ConcurrentHashMap<>();
	List<HostData> hostsData = new ArrayList<>();

	public static final int ACTIVE_DELAY = 30000;
	public static final int CONNECTED_DELAY = 3000;

	public static void main(String[] args) {
		new Server();
	}

	public Server() {
		try (ServerSocket server = new ServerSocket(serverPort)) {
			serverIP = InetAddress.getLocalHost();
			System.out.println("Lancement serveur : IP=" + serverIP + ", port=" + serverPort + ".");
			while (true) {
				new Thread(new ClientHandler(server.accept())).start();
			}
		} catch (BindException e) {
			System.err.println("Socket serveur déjà en cours d'utilisation.");
		} catch (IllegalArgumentException e) {
			System.err.println("Valeur de port invalide, doit être entre 0 et 65535.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Problème de traitement de la socket : port " + serverPort + ".");
			System.err.println("Port occupé ?");
			e.printStackTrace();
		}
	}

	public boolean addAvailable(Player player, ClientHandler handler) {
		if (!isFull()) {
			ClientHandler h = available.get(player);
			if (h != null) {
				h.kick();
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
	 * Gère un seul client.
	 *
	 */
	class ClientHandler implements Runnable, Closeable {

		Socket socket;
		MyPrintWriter out;
		MyBufferedReader in;
		
		ConnectionChecker cc;
		volatile boolean running = true;

		Player player;

		public ClientHandler(Socket socket) {
			super();
			System.out.println("Nouvelle connexion : " + socket.getRemoteSocketAddress());
			try {
				this.socket = socket;
				this.out = new MyPrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
				this.in = new MyBufferedReader(new InputStreamReader(socket.getInputStream()));
				new Thread(new ConnectionChecker(socket)).start();
			} catch (IOException e) {
				System.err.println("Pas de réponse de la socket client : " + socket.getRemoteSocketAddress() + ".");
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				player = identification();
				if (player != null) {
					addAvailable(player, this);
					System.out.println("Utilisateur '" + player.username + "' connecté depuis "
							+ socket.getRemoteSocketAddress());
				}
				while (running) {
					handle();
				}
			} catch (InterruptedException e) {
				System.err.println("Interruption de Thread.");
			} catch (SocketTimeoutException e) {
				System.err.println("Le client n'a pas répondu à temps.");
			} catch (BindException e) {
				System.err.println("Socket serveur déjà en cours d'utilisation.");
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				System.out.println("Interruption de la communication avec le client");
				close();
			} catch (SocketException e) {
				if (e.getMessage() == null) {
					e.printStackTrace();
				}
				String name = "";
				if (player != null) {
					name = "Utilisateur '" + player.username + "'";
				}
				if (e.getMessage().equals("Socket closed")) {
					System.out.println("Fin de la communication : " + name + socket.getRemoteSocketAddress() + ".");
				} else {
					System.err.println(e.getMessage() + ", client : " + name + socket.getRemoteSocketAddress() + ".");
				}
			} catch (IOException e) {
				System.err
						.println("Communication impossible avec le client : " + socket.getRemoteSocketAddress() + ".");
				e.printStackTrace();
			}
		}

		/**
		 * À REGI : identifie un joueur déjà existant, l'enregistre, ou invite à
		 * réessayer jusqu'à validation. Gère les réponses {@link Message#IDOK},
		 * {@link Message#IDNO}, {@link Message#IDIG}.
		 * 
		 * @return Un joueur identifié ou créé avec succès, ou tentatives
		 *         récursives sinon.
		 * @throws IOException
		 * @throws InterruptedException
		 */
		public Player identification() throws IOException, InterruptedException {
			if (!running) {
				throw new InterruptedException();
			}

			Message message = in.receive();

			if (message.getType().equals(Message.LEAV)) {
				System.out.println("Fin de la connexion avec " + socket.getRemoteSocketAddress() + ".");
				close();
				return null;
			}
			
			/* Serveur saturé */
			if (isFull()) {
				out.send(Message.IDNO, null, "Le serveur est plein. Réessayez ultérieurement.");
				return identification();
			}

			/* Anomalies */
			if (!message.getType().equals(Message.REGI)) {
				out.send(Message.IDNO, null, "Vous devez d'abord vous connecter : REGI Username Password");
				return identification();
			}

			/* Mauvais nombre d'arguments */
			if (!validArguments(message)) {
				out.send(Message.IDNO, null, "Identifiant et/ou Mot de passe manquant");
				return identification();
			}
			String username = message.getArg(0);
			String password = message.getArg(1);
			Player p = users.get(username);

			/* Première fois */
			if (p == null) {
				p = new Player(username, password, Player.INITIAL_POINTS);
				users.put(p.username, p);
				writePlayer(p);
				out.send(Message.IDOK, null, "Bienvenue " + username + " !");
				return p;
			}

			/* Mauvais mot de passe */
			if (!p.password.equals(password)) {
				out.send(Message.IDNO, null, "Mauvais mot de passe");
				return identification();
			}

			/* Déjà connecté */
			if (available.containsKey(p)) {
				System.out.println("Connection override de " + p.username);
				out.send(Message.IDOK, null, "Bon retour " + username + " !");
				return p;
			}

			/* Déjà en partie */
			HostData hd = inGame.get(p);
			if (hd != null) {
				out.send(Message.IDIG, new String[] { hd.getIP().toString(), String.valueOf(hd.getPort()) },
						"Finissez votre partie en cours !");
				return identification();
			}

			/* Connexion classique */
			out.send(Message.IDOK, null, "Bonjour " + username + " !");
			return p;
		}

		/**
		 * Gère toutes les requêtes possibles du client après son identification.
		 * 
		 * @throws InterruptedException
		 * @throws IOException
		 */
		private void handle() throws InterruptedException, IOException { // TODO Finish messages
			if (!running) {
				throw new InterruptedException();
			}
			Message msg = in.receive();
			switch (msg.getType()) {
			case Message.LSMA:
				out.send(Message.IDKS);
				break;
			case Message.LSAV:
				sendAvailable(msg);
				break;
			case Message.LSUS:
				sendUsers(msg);
				break;
			case Message.NWMA:
				createMatch(msg);
				break;
			case Message.LEAV:
				System.out.println(
						"Déconnexion de l'utilisateur " + player.username + ", " + socket.getRemoteSocketAddress());
				close();
				break;
			default:
				out.send(Message.IDKS, null, "Commande inconnue ou pas encore implémentée");
				break;
			}
		}

		private void sendAvailable(Message msg) {
			out.send(Message.LANB, new String[]{String.valueOf(available.size())});
			for (Player p: available.keySet()) {
				out.send(Message.AVAI, new String[]{p.username, String.valueOf(p.points)});
			}
		}
		
		private void sendUsers(Message msg) {
			out.send(Message.LUNB, new String[]{String.valueOf(users.size())});
			for (Player p: users.values()) {
				out.send(Message.USER, new String[]{p.username, String.valueOf(p.points)});
			}
		}

		private void createMatch(Message msg) {
			if (hostsData.size() >= 10) {
				out.send(Message.FULL, null, "Trop de parties en cours. Réessayez ultérieurement.");
				return;
			}
			HostData hd = null;
			try {
				hd = new HostData();
			} catch (IOException e) {
				out.send(Message.NWNO, null, e.getMessage());
			}
			// Runtime -> java [Host path] serverIP serverPort hd.getName()
			// hd.getIP() hd.getPort() // FUTURE Lancer programme externe
			String[] sendArgs = new String[] { hd.getIP().toString(), String.valueOf(hd.getPort()) };
			out.send(Message.NWOK, sendArgs, "Votre partie a été créée. Mais n'y allez pas encore (jeu à implémenter) !"); // FUTURE corriger après dev future

			/* Aucun invité */
			if (msg.getArgs() == null) {
				return;
			}

			/* ALL : inviter tous les joueurs disponibles */
			String arg1 = msg.getArg(0);
			if (arg1 != null && arg1.equals(ALL)) {
				for (ClientHandler h : available.values()) {
					h.out.send(Message.NWOK, sendArgs, player.username + " vous défie !");
				}
			}

			/* Liste spécifique de joueurs invités */
			for (String playerName : msg.getArgs()) {
				Player p = users.get(playerName);
				ClientHandler h = available.get(p);
				if (h != null) {
					h.out.send(Message.NWOK, sendArgs, player.username + " vous défie !");
				}
			}
		}

		/**
		 * Le serveur interrompt la connexion avec ce client. Player ne doit pas
		 * être null, et donc la connexion doit déjà avoir été établie.
		 */
		public void kick() {
			out.send(Message.KICK);
			close();
		}

		/**
		 * Autorise la Thread à s'arrêter, enlève le Player correspondant de
		 * Thread s'il existe ferme la socket et les streams associés
		 */
		@Override
		public void close() {
			running = false;
			if (player != null) {
				available.remove(player);
			}
			try {
				out.close();
				// in.close() bloquant, donc socket fermée pour
				// SocketException()
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		class ConnectionChecker implements Runnable {
			
			public static final int frequency = 5000;
			
			private Socket socket;
			private MyPrintWriter out;
			
			public ConnectionChecker(Socket socketArg) {
				this.socket = socketArg;
				try {
					this.out = new MyPrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
				} catch (IOException e) {
					System.err.println("Connexion interrompue. Envoi de RUOK impossible.");
				}
				System.out.println("established");
			}

			@Override
			public void run() {
				while (running) {
					try {
						Thread.sleep(frequency);
					} catch (InterruptedException e) {
						System.err.println("Interruption du Thread pendant sleep()");
					}
					out.send(Message.RUOK);
				}
			}
			
		}
		
	}

}
