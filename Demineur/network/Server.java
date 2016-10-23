package network;

import static util.Message.validArguments;
import static util.PlayersManager.getPlayersFromXML;
import static util.PlayersManager.writePlayer;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.Player;
import util.Message;
import util.MyBufferedReader;
import util.MyPrintWriter;

/**
 * Reste attentif à la connexion de nouveaux clients ou hôtes
 */
public class Server {

	final int connectionPort = 5555;

	Map<String, Player> users = getPlayersFromXML();

	public static final int MAX_ONLINE = 110;
	Map<Player, Handler> available = new HashMap<>();
	Map<Player, HostData> inGame = new HashMap<>();
	List<HostData> hostsData = new ArrayList<>();

	public static final int ACTIVE_DELAY = 30000;
	public static final int CONNECTED_DELAY = 3000;

	public static void main(String[] args) {
		Server server = new Server();
		server.launch();
	}

	public Server() {
		testArea();
	}

	/** Série d'initialisations destinées à tester le programme */
	private void testArea() {
		HostData empty = new HostData("Partie_1", "ChocoboLand", 7777);
		addInGame(users.get("Valloris"), empty);
		addInGame(users.get("Somaya"), empty);
	}

	public boolean addAvailable(Player player, Handler handler) {
		if (!isFull()) {
			Handler h = available.get(player);
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

	public void launch() {
		try (ServerSocket server = new ServerSocket(connectionPort)) {
			System.out
					.println("Lancement serveur : IP=" + InetAddress.getLocalHost() + ", port=" + connectionPort + ".");
			while (true) {
				new Thread(new Handler(server.accept())).start();
				;
			}
		} catch (BindException e) {
			System.err.println("Socket serveur déjà en cours d'utilisation.");
		} catch (IllegalArgumentException e) {
			System.err.println("Valeur de port invalide, doit être entre 0 et 65535.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Problème de traitement de la socket : port " + connectionPort + ".");
			e.printStackTrace();
		}
	}

	public boolean isFull() {
		return available.size() + inGame.size() >= MAX_ONLINE;
	}

	/**
	 * Gère un seul client
	 *
	 */
	class Handler implements Runnable {

		Socket socket;

		MyPrintWriter out;
		MyBufferedReader in;

		Player player;

		public Handler(Socket socket) {
			super();
			System.out.println("Nouvelle connexion : " + socket.getRemoteSocketAddress());
			try {
				this.socket = socket;
				this.out = new MyPrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
				this.in = new MyBufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (IOException e) {
				System.err.println("Pas de réponse de la socket client : " + socket.getRemoteSocketAddress() + ".");
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				player = identification();
				System.out.println(
						"Utilisateur '" + player.username + "' connecté depuis " + socket.getRemoteSocketAddress());
				addAvailable(player, this);
				while (socket.isBound() && socket.isConnected() && !socket.isClosed()) {
					Thread.sleep(20000);
					System.out.println(socket.getRemoteSocketAddress() + ", le serveur reste à l'écoute");
				}
				System.out.println(socket.getRemoteSocketAddress() + " : Handler end");
			} catch (SocketTimeoutException e) {
				System.err.println("Le client n'a pas répondu à temps.");
			} catch (BindException e) {
				System.err.println("Socket serveur déjà en cours d'utilisation.");
			} catch (IllegalArgumentException e) {
				System.err.println("Valeur de port invalide, doit être entre 0 et 65535.");
			} catch (SocketException e) {
				String name = "";
				if (player != null) {
					name = "Utilisateur '" + player.username + "' ; ";
				}
				System.err.println(name + "Connexion perdue avec le client : " + socket.getRemoteSocketAddress() + ".");
			} catch (IOException e) {
				System.err.println("Pas de réponse de la socket client : " + socket.getRemoteSocketAddress() + ".");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		/**
		 * Pour REGI : identifie un joueur déjà existant, l'enregistre, ou invte
		 * à réessayer jusqu'à validation. Gère les réponses
		 * {@link Message#IDOK}, {@link Message#IDNO}, {@link Message#IDIG}.
		 * 
		 * @return Un joueur identifié ou créé avec succès, ou tentatives récursives sinon.
		 * @throws IOException
		 */
		public Player identification() throws IOException {
			Message message = in.receive();
			/* Serveur saturé */
			if (isFull()) {
				out.send(Message.IDNO, null, "Le serveur est plein. Réessayez ultérieurement.");
				return identification();
			}

			/* Anomalies */
			if (message == null) {
				System.err.println("Le client ne semble pas répondre");
				return identification();
			}
			if (!message.getType().equals(Message.REGI)) {
				out.send(Message.IDNO, null, "Vous devez d'abord vous connecter : REGI Username Password");
				return identification();
			}

			/* Arguments et identification */
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
				System.out.println("reconnexion");
				out.send(Message.IDOK, null, "Bon retour " + username + " !");
				return p;
			}

			/* Déjà en partie */
			HostData hd = inGame.get(p);
			if (hd != null) {
				out.send(Message.IDIG, new String[] { hd.IP, String.valueOf(hd.port) },
						"Finissez votre partie en cours !");
				return identification();
			}

			/* Connexion classique */
			out.send(Message.IDOK, null, "Bonjour " + username + " !");
			return p;
		}

		/**
		 * Le serveur interrompt la connexion avec ce client. Player ne doit pas
		 * être null, et donc la connexion doit déjà avoir été établie.
		 */
		public void kick() {
			out.send(Message.KICK);
			System.out
					.println("Fermeture de la socket de '" + player.username + "', " + socket.getRemoteSocketAddress());
			try {
				socket.close();
			} catch (IOException e) {
				System.err.println("Impossible de fermer la socket du joueur '" + player.username + "'. Déjà fermée ?");
			}
		}

	}
}
