package network;

import static java.lang.String.valueOf;
import static util.Message.validArguments;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import data.Player;
import game.Board;
import util.Message;
import util.MyBufferedReader;
import util.MyPrintWriter;

/**
 * Hôte lancé par le serveur //FUTURE Programmer lancement par serveur au lieu
 * de manuel une fois dév achevée
 */
public class Host {

	public static final int MAX_PLAYERS = 10;

	public static final int ACTIVE_DELAY = 30000;
	public static final int CONNECTED_DELAY = 10000;

	InetAddress serverIP;
	int serverPort;

	String name;
	InetAddress IP;
	int port;

	Board board = new Board();

	volatile static Map<String, InGamePlayer> inGamePlayers = new ConcurrentHashMap<>();

	private static void deny(String message) {
		System.err.println(message);
		System.err.println("Attendu : java Host serverIP serverPort hostName hostIP hostPort");
		System.exit(1);
	}

	public static void main(String[] args) {
		try { // TEST
			new Host(null, 5555, "HostTest", InetAddress.getLocalHost(), 3333);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		//		if (args.length < 5) {
		//			deny("Mauvais nombre d'arguments.");
		//		}
		//
		//		InetAddress serverIP = null;
		//		try {
		//			serverIP = InetAddress.getByName(args[0]);
		//		} catch (UnknownHostException e) {
		//			deny("Paramètre n°1 invalide, adresse IP du serveur non reconnue.");
		//		}
		//
		//		int serverPort = 0;
		//		try {
		//			serverPort = Integer.parseInt(args[1]);
		//		} catch (NumberFormatException e) {
		//			deny("Paramètre n°2 invalide, numéro de port du serveur attendu.");
		//			return;
		//		}
		//
		//		InetAddress hostIP = null;
		//		try {
		//			hostIP = InetAddress.getByName(args[0]);
		//		} catch (UnknownHostException e) {
		//			deny("Paramètre n°4 invalide, adresse IP d'hôte non reconnue.");
		//		}
		//
		//		int hostPort = 0;
		//		try {
		//			hostPort = Integer.parseInt(args[4]);
		//		} catch (NumberFormatException e) {
		//			deny("Paramètre n°5 invalide, numéro de port libre d'hôte attendu");
		//		}
		//		new Host(serverIP, serverPort, args[2], hostIP, hostPort);
	}

	public Host(InetAddress serverIP, int serverPort, String name, InetAddress IP, int port) {
		super();
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.name = name;
		this.IP = IP;
		this.port = port;

		// TODO établir connexion au serveur

		try (ServerSocket ss = new ServerSocket(port)) {
			System.out.println("Lancement hôte : IP=" + IP + ", port=" + port + ".");
			while (true) {
				new Thread(new PlayerHandler(ss.accept())).start();
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

	private class InGamePlayer extends Player { // THINK Classe externe ?

		PlayerHandler handler;
		volatile boolean active = false;
		volatile int inGamePoints = 0;

		volatile int safeSquares = 0;
		volatile int foundMines = 0;

		public InGamePlayer(String username, String password, PlayerHandler handler) {
			super(username, password);
			this.handler = handler;
		}

		public String[] publicData() {
			return new String[] { this.username, valueOf(inGamePoints), valueOf(totalPoints), valueOf(safeSquares),
					valueOf(foundMines) };
		}

	}

	private class PlayerHandler extends Thread implements AutoCloseable {

		// TODO gérer inactivité client
		Socket socket;
		MyPrintWriter out;
		MyBufferedReader in;

		private volatile boolean running = true;

		InGamePlayer player;

		public PlayerHandler(Socket socket) {
			super();
			System.out.println("Nouvelle connexion : " + socket.getRemoteSocketAddress());
			try {
				this.socket = socket;
				this.out = new MyPrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				this.in = new MyBufferedReader(new InputStreamReader(socket.getInputStream()));
				this.socket.setSoTimeout(CONNECTED_DELAY); // TEST Neutraliser RUOK
				new Thread(new Ping()).start();
			} catch (IOException e) {
				System.err.println("Pas de réponse de la socket client : " + socket.getRemoteSocketAddress() + ".");
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				player = identification();
				//				if (player != null) {
				//					addAvailable(player, this);
				//					System.out.println("Utilisateur '" + player.username + "' connecté depuis "
				//							+ socket.getRemoteSocketAddress() + ".");
				//				}
				while (running) {
					handleInGame();
				}
			} catch (InterruptedException e) {
				System.err.println(e.getMessage());
				close();
			} catch (SocketTimeoutException e) {
				System.err.println("Le client " + socket.getRemoteSocketAddress() + " n'a pas répondu à temps.");
				close();
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				System.out.println("Interruption de la communication avec le client.");
				close();
			} catch (IOException e) {
				if (e instanceof BindException) {
					System.err.println("Socket serveur déjà en cours d'utilisation.");
				} else if (e instanceof SocketException) {
					if (e.getMessage() == null) {
						e.printStackTrace();
						close();
					}
					String name = "";
					if (player != null) {
						name = "Utilisateur '" + player.username + "'";
					}
					if (e.getMessage().toUpperCase().equals("SOCKET CLOSED")) {
						System.out.println("Fin de la communication : " + name + socket.getRemoteSocketAddress() + ".");
					} else {
						System.err
						.println(e.getMessage() + ", client : " + name + socket.getRemoteSocketAddress() + ".");
					}
				} else {
					System.err.println(
							"Communication impossible avec le client : " + socket.getRemoteSocketAddress() + ".");
					e.printStackTrace();
					close();
				}
			}
		}

		/*
		 *  FUTURE Nombre de récursions limitée à au moins 1024, définissable avec -xss.
		 *  Solution 1 : Limiter le nombre de tentatives de connexion.
		 */
		public InGamePlayer identification() throws IOException, InterruptedException {
			if (!running) {
				throw new InterruptedException();
			}
			Message message = in.receive();
			if (message.getType().equals(Message.IMOK)) {
				return identification();
			}
			/* Anomalies */
			if (!message.getType().equals(Message.JOIN)) {
				out.send(Message.IDKH, null, "Vous devez d'abord vous identifier : JOIN Username Password");
				return identification();
			}
			/* Mauvais nombre d'arguments */
			if (!validArguments(message)) {
				out.send(Message.IDNO, null, "Identifiant et/ou Mot de passe manquant");
				return identification();
			}
			String username = message.getArg(0);
			String password = message.getArg(1);
			InGamePlayer p = inGamePlayers.get(username);

			/* Reconnexion */
			if (p != null) {
				if (!p.checkPassword(password)) {
					out.send(Message.JNNO, null, "Mot de passe incorrect.");
					return identification();
				}

				if (p.active) {
					out.send(Message.JNNO, null, "Vous semblez déjà en train de jouer.");
					return identification(); // éventuellement kick()
				}

				p.active = true;
				sendGameState(p);
				return p;
			}

			/* Hôte saturé */
			if (inGamePlayers.size() >= MAX_PLAYERS) {
				out.send(Message.JNNO, null, "Nombre maximum de joueurs atteint. Plus de place disponible !");
				return identification();
			}

			/* Nouvelle connexion */
			// TODO échange de données Server-Host
			InGamePlayer p2 = new InGamePlayer(username, password, this);
			inGamePlayers.put(username, p2);
			this.player = p2;
			System.out.println("nouveau"); // TEST
			sendGameState(p2);
			return p2;
		}

		private void sendGameState(InGamePlayer player) {
			/* Send board */
			out.send(Message.JNOK, new String[] { String.valueOf(board.height) },
					"Bienvenue sur " + Host.this.name + ", " + player.username + " !");
			for (int y = 0; y < board.height; y++) {
				List<String> lineContent = board.lineContentAt(y);
				lineContent.add(0, String.valueOf(y));
				out.send(Message.BDIT, lineContent.stream().toArray(String[]::new));
			}

			/* Send in-game players data */
			out.send(Message.IGNB, new String[] { String.valueOf(inGamePlayers.size()) });
			for (InGamePlayer igp : inGamePlayers.values()) {
				out.send(Message.IGPL, igp.publicData());
			}

			/* Inform other players */
			for (InGamePlayer igp : inGamePlayers.values()) {
				if (igp != player && igp.active) {
					out.send(Message.CONN, player.publicData());
				}
			}
		}

		private void handleInGame() throws InterruptedException, IOException {
			if (!running) {
				throw new InterruptedException();
			}
			Message msg = in.receive();
			switch (msg.getType()) {
			case Message.IMOK: // Permet de reset le SO_TIMEOUT de la socket
				break;
			case Message.CLIC:
				int abscissa = msg.getArgAsInt(0);
				int ordinate = msg.getArgAsInt(1);
				
				if (!board.validAbscissa(abscissa) || !board.validOrdinate(ordinate)) {
					out.send(Message.OORG, new String[]{valueOf(abscissa), valueOf(ordinate)}, "Coordonnées invalides ! ");
				}
				
				List<String[]> allArgs = board.clickAt(abscissa, ordinate, player.username);
				if (allArgs == null) {
					out.send(Message.LATE, null, "Case déjà déminée.");
					break;
				}
				
				for (String[] line : allArgs) {
					for (InGamePlayer igp : inGamePlayers.values()) {
						igp.handler.out.send(Message.SQRD, line);
					}
				}
				break;
			default:
				out.send(Message.IDKH, null, "Commande inconnue ou pas encore implémentée");
				break;
			}
		}

		private class Ping implements Runnable {

			public static final int frequency = 5000;

			@Override
			public void run() {
				while (running) {
					out.send(Message.RUOK);
					try {
						Thread.sleep(frequency);
					} catch (InterruptedException e) {
						System.err.println("Interruption du Thread ConnectionChecker pendant sleep()");
					}
				}
			}

		}

		/**
		 * Autorise la Thread à s'arrêter, met le joueur inactif. Ferme la socket et les streams associés.
		 */
		@Override
		public void close() {
			running = false;
			try {
				//				if (player != null) { //TODO
				//					available.remove(player);
				//				}
				out.close();
				in.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
