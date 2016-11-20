package network;

import static java.lang.String.valueOf;
import static util.Message.validArguments;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import data.Player;
import game.Board;
import util.Message;
import util.TFServerSocket;
import util.TFSocket;

/**
 * Hôte lancé par le serveur //FUTURE Programmer lancement par serveur au lieu
 * de manuel une fois dév achevée
 */
public class Host extends Entity {

	public static final int MAX_PLAYERS = 10;

	public static final int ACTIVE_DELAY = 30000;
	public static final int CONNECTED_DELAY = 10000;

	InetAddress serverIP;
	int serverPort;

	InetAddress IP;
	int port;

	Board board = new Board();
	int multiplicator = 0;

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

		try (TFServerSocket ss = new TFServerSocket(port)) {
			System.out.println("Lancement hôte : IP=" + IP + ", port=" + port + ".");
			while (true) {
				new Thread(new PlayerHandler(ss.accept())).start();
			}
		} catch (BindException e) {
			System.err.println("Socket hôte déjà en cours d'utilisation.");
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
		volatile boolean active;
		volatile int inGamePoints = 0;

		volatile int safeSquares = 0;
		volatile int foundMines = 0;

		private InGamePlayer(String username, String password, PlayerHandler handler) {
			super(username, password);
			this.handler = handler;
			setActive();
		}

		private String[] publicData() {
			return new String[] { this.username, valueOf(inGamePoints), valueOf(totalPoints), valueOf(safeSquares),
					valueOf(foundMines) };
		}
		
		private void setActive() {
			active = true;
			multiplicator++;
		}
		
		private void setInactive() {
			active = false;
			multiplicator--;
		}

	}
	
	private class PlayerHandler extends Thread implements AutoCloseable {

		// TODO gérer inactivité client
		TFSocket socket;

		private volatile boolean running = true;

		InGamePlayer player;

		public PlayerHandler(TFSocket socket) {
			super();
			System.out.println("Nouvelle connexion : " + socket.getRemoteSocketAddress());
			this.socket = socket;
			this.socket.ping();
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
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				System.out.println("Interruption de la communication avec le client.");
				close();
			} catch (IOException e) {
				close();
			}
		}

		/*
		 *  FUTURE Nombre de récursions limitée à au moins 1024, définissable avec -xss.
		 *  Solution 1 : Limiter le nombre de tentatives de connexion.
		 */
		public InGamePlayer identification() throws InterruptedException, IOException {
			if (!running) {
				throw new InterruptedException();
			}
			Message message = socket.receive();
			if (message.getType().equals(Message.IMOK)) {
				return identification();
			}
			/* Anomalies */
			if (!message.getType().equals(Message.JOIN)) {
				socket.send(Message.IDKH, null, "Vous devez d'abord vous identifier : JOIN Username Password");
				return identification();
			}
			/* Mauvais nombre d'arguments */
			if (!validArguments(message)) {
				socket.send(Message.IDNO, null, "Identifiant et/ou Mot de passe manquant");
				return identification();
			}
			String username = message.getArg(0);
			String password = message.getArg(1);
			InGamePlayer p = inGamePlayers.get(username);

			/* Reconnexion */
			if (p != null) {
				if (!p.checkPassword(password)) {
					socket.send(Message.JNNO, null, "Mot de passe incorrect.");
					return identification();
				}

				if (p.active) {
					socket.send(Message.JNNO, null, "Vous semblez déjà en train de jouer.");
					return identification(); // éventuellement kick()
				}

				p.setActive();
				sendGameState(p);
				return p;
			}

			/* Hôte saturé */
			if (inGamePlayers.size() >= MAX_PLAYERS) {
				socket.send(Message.JNNO, null, "Nombre maximum de joueurs atteint. Plus de place disponible !");
				return identification();
			}

			/* Nouvelle connexion */
			// TODO échange de données Server-Host 
			
			InGamePlayer p2 = new InGamePlayer(username, password, this); // TEST à supprimer
			inGamePlayers.put(username, p2);
			this.player = p2;
			sendGameState(p2);
			return p2;
		}

		private void sendGameState(InGamePlayer player) {
			/* Send board */
			socket.send(Message.JNOK, new String[] { String.valueOf(board.height) },
					"Bienvenue sur " + Host.this.name + ", " + player.username + " !");
			for (int y = 0; y < board.height; y++) {
				List<String> lineContent = board.lineContentAt(y);
				lineContent.add(0, String.valueOf(y));
				socket.send(Message.BDIT, lineContent.stream().toArray(String[]::new));
			}

			/* Send in-game players data */
			socket.send(Message.IGNB, new String[] { String.valueOf(inGamePlayers.size()) });
			for (InGamePlayer igp : inGamePlayers.values()) {
				socket.send(Message.IGPL, igp.publicData());
			}

			/* Inform other players */
			inGamePlayers.values().stream().filter(p -> (p != player && p.active)).forEach(p -> socket.send(Message.CONN, player.publicData()));
		}

		private void handleInGame() throws InterruptedException, IOException {
			if (!running) {
				throw new InterruptedException();
			}
			Message msg = socket.receive();
			switch (msg.getType()) {
			case Message.IMOK: // Permet de reset le SO_TIMEOUT de la socket
				break;
			case Message.CLIC:
				int abscissa = msg.getArgAsInt(0);
				int ordinate = msg.getArgAsInt(1);
				
				if (!board.validAbscissa(abscissa) || !board.validOrdinate(ordinate)) {
					socket.send(Message.OORG, new String[]{valueOf(abscissa), valueOf(ordinate)}, "Coordonnées invalides ! ");
				}
				
				List<String[]> allArgs = board.clickAt(abscissa, ordinate, player.username);
				if (allArgs == null) {
					socket.send(Message.LATE, null, "Case déjà déminée.");
					break;
				}
				
				for (String[] line : allArgs) {
					for (InGamePlayer igp : inGamePlayers.values()) {
						line[3] = valueOf(Integer.parseInt(line[3]) * multiplicator);
						igp.handler.socket.send(Message.SQRD, line);
					}
				}
				break;
			default:
				socket.send(Message.IDKH, null, "Commande inconnue ou pas encore implémentée");
				break;
			}
		}

		/**
		 * Autorise la Thread à s'arrêter, met le joueur inactif.
		 */
		@Override
		public void close() {
			running = false;
			if (player != null) {
				player.setInactive();
			}
			socket.close();
		}

	}

}
