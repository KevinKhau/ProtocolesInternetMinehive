package network;

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

import util.Message;
import util.MyBufferedReader;
import util.MyPrintWriter;

import static util.Message.*;

/**
 * Reste attentif à la connexion de nouveaux clients ou hôtes
 */
public class Server {

	final int connectionPort = 5555;
	Manager manager = new Manager();

	Map<String, Player> users = new HashMap<>();

	public static final int MAX_ONLINE = 110;
	Map<String, Player> available = new HashMap<>();
	Map<Player, HostData> inGame = new HashMap<>();
	List<HostData> hostsData = new ArrayList<>();

	public static final int RUOK_DELAY = 300000;
	public static final int KICK_DELAY = 60000;

	public static void main(String[] args) {
		Server server = new Server();
		server.launch();
	}

	public Server() {
		testArea();
	}

	/** Série d'initialisations destinées à tester le programme */
	private void testArea() {
		Player adil = new Player("Adil", "Champion", 10);
		Player aylin = new Player("Aylin", "Kocoglu", 90);
		Player christophe = new Player("Christophe", "Lam", 20);
		Player alhassane = new Player("AlHassane", "Megningta", 80);
		Player valloris = new Player("Valloris", "Cylly", 30);
		Player somaya = new Player("Somaya", "Benkhemis", 70);

		addUser(adil);
		addUser(aylin);
		addUser(christophe);
		addUser(alhassane);
		addUser(valloris);
		addUser(somaya);

		addAvailable(christophe);
		addAvailable(alhassane);

		HostData empty = new HostData("Partie_1", "ChocoboLand", 7777);
		addInGame(valloris, empty);
		addInGame(somaya, empty);
	}

	public void addUser(Player player) {
		users.put(player.username, player);
	}

	public boolean addAvailable(Player player) {
		if ((available.size() + inGame.size()) <= MAX_ONLINE) {
			available.put(player.username, player);
			return true;
		}
		return false;
	}

	public void addInGame(Player player, HostData hostData) {
		inGame.put(player, hostData);
	}

	public void launch() {
		try (ServerSocket server = new ServerSocket(connectionPort)) {
			System.out.println("Lancement serveur : IP=" + server.getInetAddress() + ", port=" + connectionPort + ".");
			while (true) {
				new Handler(server.accept());
			}
		} catch (SocketTimeoutException e) {
			System.err.println("Le client n'a pas répondu à temps.");
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

	/**
	 * Gère l'ensemble des connexions clients
	 *
	 */
	class Manager extends Thread {

		public void addConnection(Socket socket) {

		}

	}

	/**
	 * Gère un seul client
	 *
	 */
	class Handler extends Thread {

		MyPrintWriter out;
		MyBufferedReader in;

		Player player;

		public Handler(Socket socket) {
			super();
			InetAddress addr = socket.getInetAddress();
			int port = socket.getPort();
			System.out.println("Nouvelle connexion : IP=" + addr + ", port=" + port);
			try {
				this.out = new MyPrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
				this.in = new MyBufferedReader(new InputStreamReader(socket.getInputStream()));
				execute();
			} catch (SocketTimeoutException e) {
				System.err.println("Le client n'a pas répondu à temps.");
			} catch (BindException e) {
				System.err.println("Socket serveur déjà en cours d'utilisation.");
			} catch (IllegalArgumentException e) {
				System.err.println("Valeur de port invalide, doit être entre 0 et 65535.");
			} catch (SocketException e) {
				System.err.println("Connexion perdue avec le client : IP=" + addr + ", port=" + port + ".");
			} catch (IOException e) {
				System.err.println("Pas de réponse de la socket client : IP=" + addr + ", port=" + port + ".");
				e.printStackTrace();
			}
		}

		public void execute() throws IOException {
			Message msg;
			do {
				msg = in.receive();
				player = identification(msg);
			} while (player == null);
			System.out.println("Utilisateur connecté.");
			addAvailable(player);
		}

		/**
		 * Pour REGI : connecte un joueur déjà existant, ou l'enregistre. Gère
		 * l'envoi du IDOK ou IDNO.
		 * 
		 * @param Message
		 *            de type REGI avec username et password du client
		 * @return Un joueur identifié ou créé avec succès, ou null sinon.
		 */
		public Player identification(Message message) {
			/* Anomalies */
			if (message == null) {
				System.err.println("Le client ne semble pas répondre");
				return null;
			}
			if (!message.getType().equals(Message.REGI)) {
				System.err.println("identification : Message de type autre que " + Message.REGI + " reçu");
				return null;
			}

			/* Arguments et identification */
			if (!validArguments(message)) {
				out.send(Message.IDNO, null, "Identifiant et/ou Mot de passe manquant");
				return null;
			}
			String username = message.getArg(0);
			String password = message.getArg(1);
			Player p = users.get(username);
			/* Première fois */
			if (p == null) {
				p = new Player(username, password, Player.INITIAL_POINTS);
				out.send(Message.IDOK, null, "Bienvenue " + username + " !");
				return p;
			}
			/* Mauvais mot de passe */
			if (!p.password.equals(password)) {
				out.send(Message.IDNO, null, "Mauvais mot de passe");
				return null;
			}

			/* Déjà connecté */
			if (available.get(username) != null) {
				System.out.println("reconnexion");
				out.send(Message.IDOK, null, "Bon retour " + username + " !");
				return p;
			}

			/* Déjà en partie */
			HostData hd = inGame.get(p);
			if (hd != null) {
				out.send(Message.IDIG, new String[] { hd.IP, String.valueOf(hd.port) },
						"Finissez votre partie en cours !");
				return null;
			}

			/* Connexion classique */
			out.send(Message.IDOK, null, "Bonjour " + username + " !");
			return p;
		}

	}
}
