package network;

import static java.lang.String.valueOf;
import static util.Message.validArguments;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import data.Person;
import game.Board;
import network.Communicator.State;
import util.Message;
import util.Params;
import util.TFServerSocket;
import util.TFSocket;

/**
 * Hôte lancé par le serveur
 */
public class Host extends Entity {

	public static final String JAR_NAME = "Host.jar";
	public static final String NAME = "Hôte";

	public static final int MAX_PLAYERS = 10;
	public static final int BEST_BOUNTY = 50;
	public static final int WORST_BOUNTY = -50;

	public static final int ACTIVE_DELAY = 30000;
	public static final int CONNECTED_DELAY = 10000;

	String password = "Chocobo";

	InetAddress serverIP;
	int serverPort;
	private volatile ServerCommunicator serverCommunicator;

	InetAddress IP;
	int port;

	Board board = new Board();
	volatile int multiplicator = 0;

	volatile Map<String, Person> standingByHelper = new ConcurrentHashMap<>();
	volatile Map<Person, PlayerHandler> standingBy = new ConcurrentHashMap<>();

	volatile Map<String, InGamePlayer> inGamePlayers = new ConcurrentHashMap<>();

	static Logger launchLogger = Logger.getLogger("HostLaunch");

	private static void deny(String message) {
		System.err.println(message);
		System.err.println("Attendu : java Host serverIP serverPort hostName hostIP hostPort [hostPassword]");
		launchLogger.warning(message);
		System.exit(1);
	}

	public static void main(String[] args) {
		Path launchLogPath = null;
		try {
			launchLogPath = Paths.get(Params.LOG.toString(), "HostLaunch" + "Log.xml");
			launchLogger.addHandler(new FileHandler(launchLogPath.toString()));
		} catch (Exception e) {
			try {
				launchLogger.addHandler(new FileHandler(Params.BIN.toString() + "/" + "HostLaunch" + "Log.xml"));
			} catch (SecurityException | IOException e1) {
				e1.printStackTrace();
			}
		}
		if (Params.DEBUG_HOST) {
			try {
				if (args.length < 1) {
					System.err.println("Paramètre port de connexion pour les clients attendu.");
					System.exit(1);
				}
				new Host(InetAddress.getLocalHost(), 7777, "Partie_1", InetAddress.getLocalHost(),
						Integer.parseInt(args[0]), null);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
			return;
		}

		launchLogger.info("Arguments : " + Arrays.toString(args));
		System.out.println("Arguments : " + Arrays.toString(args));
		if (args.length < 5) {
			deny("Mauvais nombre d'arguments.");
		}

		InetAddress serverIP = null;
		try {
			serverIP = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			deny("Paramètre n°1 invalide, adresse IP du serveur non reconnue.");
		}

		int serverPort = 0;
		try {
			serverPort = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			deny("Paramètre n°2 invalide, numéro de port du serveur attendu.");
			return;
		}

		InetAddress hostIP = null;
		try {
			hostIP = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			deny("Paramètre n°4 invalide, adresse IP d'hôte non reconnue.");
		}

		int hostPort = 0;
		try {
			hostPort = Integer.parseInt(args[4]);
		} catch (NumberFormatException e) {
			deny("Paramètre n°5 invalide, numéro de port libre d'hôte attendu");
		}
		
		String password = null;
		if (args.length < 6) {
			String errorMsg = "Password not provided. Will use the default one.";
			System.err.println(errorMsg);
			launchLogger.warning(errorMsg);
		} else {
			password = args[5];
		}
		
		new Host(serverIP, serverPort, args[2], hostIP, hostPort, password);
	}

	public Host(InetAddress serverIP, int serverPort, String name, InetAddress IP, int port, String password) {
		super(name);
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.IP = IP;
		this.port = port;
		if (password != null) {
			this.password = password;
		}
		serverCommunicator = new ServerCommunicator();
		new Thread(serverCommunicator).start();

		// FUTURE Attente passive
		try (TFServerSocket ss = new TFServerSocket(port)) {
			while (serverCommunicator.running) {
				if (serverCommunicator != null && serverCommunicator.state == State.IN) {
					System.out.println("Lancement hôte : IP=" + IP + ", port=" + port + ".");
					// FUTURE extends Listener
					new Thread(new PlayerHandler(ss.accept())).start();
				}
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

	public void endMatch() {
		/* Bonus */
		InGamePlayer best = inGamePlayers.values().stream()
				.max((p1, p2) -> Integer.compare(p1.safeSquares, p2.safeSquares)).get();
		best.incPoints(BEST_BOUNTY);
		InGamePlayer worst = inGamePlayers.values().stream()
				.max((p1, p2) -> Integer.compare(p1.foundMines, p2.foundMines)).get();
		worst.incPoints(WORST_BOUNTY);
		String comment = best.username + " is the best minesweeper: +" + BEST_BOUNTY + " ! " + worst.username
				+ " has digged up the most mines: " + WORST_BOUNTY + "...";

		/* Envois SCP? */
		inGamePlayers.values().forEach(igp -> {
			if (serverCommunicator != null && serverCommunicator.running) {
				serverCommunicator.communicatorSocket.send(Message.SCPS,
						new String[] { igp.username, valueOf(igp.totalPoints) });
			}
			inGamePlayers.values().stream().filter(igp2 -> igp2.handler.running)
					.forEach(igp2 -> igp2.handler.socket.send(Message.SCPC, igp.publicData(), comment));
		});

		/* Conclusion END? */
		if (serverCommunicator != null && serverCommunicator.running) {
			serverCommunicator.communicatorSocket.send(Message.ENDS, new String[] { name }, "End of the match!");
			serverCommunicator.disconnect();
		}
		inGamePlayers.values().stream().filter(igp -> igp.handler.running).forEach(igp -> {
			igp.handler.socket.send(Message.ENDC, new String[] { valueOf(inGamePlayers.size()) }, "End of the match!");
			igp.handler.disconnect();
		});

		System.exit(0);

	}

	class InGamePlayer extends Person {

		public static final String NAME = "Jouer Actif";

		PlayerHandler handler;
		private volatile boolean active;
		private volatile int inGamePoints = 0;

		private volatile int safeSquares = 0;
		private volatile int foundMines = 0;

		/** Nouveau joueur */
		private InGamePlayer(Person player, PlayerHandler handler) {
			super(player.username, player.password);
			LOGGER.info("Nouveau joueur connecté " + player);
			this.handler = handler;
			setActive();
		}

		private String[] publicData() {
			return new String[] { this.username, valueOf(inGamePoints), valueOf(totalPoints), valueOf(safeSquares),
					valueOf(foundMines) };
		}

		private synchronized void setActive() {
			if (!active) {
				active = true;
				synchronized (Host.this) {
					multiplicator++;
				}
			}
		}

		private synchronized void setInactive() {
			if (active) {
				active = false;
				synchronized (Host.this) {
					multiplicator--;
				}
			}
		}

		public synchronized void incFoundMines() {
			foundMines++;
		}

		public synchronized void incSafeSquares() {
			safeSquares++;
		}

		public synchronized void incPoints(int add) {
			inGamePoints += add;
			incTotalPoints(add);
		}

	}

	/** Host <- Client */
	class PlayerHandler extends SenderHandler {

		/**
		 * Correspond à l'entityData casté. Initialisé lors de identification()
		 */
		InGamePlayer inGamePlayer;
		volatile boolean identified = false;

		public PlayerHandler(TFSocket socket) {
			super(socket, InGamePlayer.NAME);
		}

		@Override
		public void run() {
			try {
				identification();
				if (running) {
					addEntityData();
				}
				while (running) {
					Message rcv = null;
					rcv = socket.receive();
					LOGGER.info(rcv.toString());
					handleMessage(rcv);
				}
			} catch (IOException | IllegalArgumentException e) {
				LOGGER.warning("Déconnexion : " + e.getMessage());
				disconnect();
			}
		}

		@Override
		public void identification() throws IOException {
			while (running) {
				Message message = socket.receive();

				/* Anomalies */
				if (!message.getType().equals(Message.JOIN)) {
					socket.send(Message.IDKH, null, "Vous devez d'abord vous identifier : JOIN Username Password");
					continue;
				}
				/* Mauvais nombre d'arguments */
				if (!validArguments(message)) {
					socket.send(Message.IDNO, null, "Identifiant et/ou Mot de passe manquant");
					continue;
				}
				String username = message.getArg(0);
				String password = message.getArg(1);
				senderData = inGamePlayers.get(username);

				/* Reconnexion */
				if (senderData != null) {
					synchronized (this) {
						inGamePlayer = (InGamePlayer) senderData;
						LOGGER.fine("Tentative de reconnexion de " + inGamePlayer);
						if (!inGamePlayer.checkPassword(password)) {
							socket.send(Message.JNNO, null, "Mot de passe incorrect.");
							inGamePlayer = null;
							continue;
						}
						if (inGamePlayer.active) {
							socket.send(Message.JNNO, null, "Vous semblez déjà en train de jouer.");
							inGamePlayer = null;
							continue;
						}
					}
					inGamePlayer.setActive();
					inGamePlayer.handler = this;
					sendGameState(inGamePlayer);
					break;
				}

				/* Hôte saturé */
				if (inGamePlayers.size() >= MAX_PLAYERS) {
					socket.send(Message.JNNO, null, "Nombre maximum de joueurs atteint. Plus de place disponible !");
					continue;
				}

				/* Nouvelle connexion */
				Person p = new Person(username, password);
				standingByHelper.put(p.username, p);
				standingBy.put(p, this);
				serverCommunicator.communicate(
						new Message(Message.PLIN, new String[] { Host.this.name, username, password }, null));
				waitResponse();
				if (identified) {
					break;
				} else {
					continue;
				}
			}
		}

		/**
		 * Non utilisé, déjà bien géré dans identification()
		 */
		@Override
		protected void addEntityData() {
		}

		public void sendGameState(InGamePlayer player) {
			/* Send board */
			socket.send(Message.JNOK, new String[] { String.valueOf(board.height) },
					"Bienvenue sur " + Host.this.name + ", " + player.username + " !");
			for (int y = 0; y < board.height; y++) {
				List<String> lineContent = board.lineContentAt(y);
				lineContent.add(0, String.valueOf(y));
				String[] args = lineContent.stream().toArray(String[]::new);
				socket.send(Message.BDIT, args);
			}

			/* Send in-game players data */
			socket.send(Message.IGNB, new String[] { String.valueOf(inGamePlayers.size()) });
			for (InGamePlayer igp : inGamePlayers.values()) {
				socket.send(Message.IGPL, igp.publicData());
			}

			/* Inform other players */
			inGamePlayers.values().stream().filter(p -> (!p.equals(player) && p.active)).forEach(p -> {
				p.handler.socket.send(Message.CONN, player.publicData());
			});
		}

		@Override
		protected void handleMessage(Message reception) {
			switch (reception.getType()) {
			case Message.CLIC:
				int abscissa = reception.getArgAsInt(0);
				int ordinate = reception.getArgAsInt(1);

				List<String[]> allArgs;
				try {
					allArgs = board.clickAt(abscissa, ordinate, inGamePlayer.username);
				} catch (ArrayIndexOutOfBoundsException e) {
					socket.send(Message.OORG, new String[] { valueOf(abscissa), valueOf(ordinate) }, e.getMessage());
					break;
				}
				if (allArgs == null) {
					socket.send(Message.LATE, null, "Case déjà déminée.");
					break;
				}

				for (String[] line : allArgs) {
					int value = Integer.parseInt(line[2]);
					if (value < 0) {
						inGamePlayer.incFoundMines();
					} else {
						inGamePlayer.incSafeSquares();
					}
					int points = Integer.parseInt(line[3]) * multiplicator;
					inGamePlayer.incPoints(points);
					line[3] = valueOf(points);
					inGamePlayers.values().stream().filter(igp -> igp.handler.running)
							.forEach(igp -> igp.handler.socket.send(Message.SQRD, line));
				}

				/* Fin de partie */
				if (board.isFinished()) {
					endMatch();
				}
				break;
			default:
				unknownMessage();
				break;
			}
		}

		@Override
		protected void unknownMessage() {
			socket.send(Message.IDKH, null, "Commande inconnue ou pas encore implémentée");
		}

		@Override
		protected void removeEntityData() {
			if (inGamePlayer != null) {
				inGamePlayer.setInactive();
				LOGGER.info(inGamePlayer + " désormais inactif.");
				for (InGamePlayer igp : inGamePlayers.values()) {
					if (igp != inGamePlayer && igp.active) {
						igp.handler.socket.send(Message.DECO, new String[] { inGamePlayer.username });
					}
				}
			}
		}

		/**
		 * Bloque ce PlayerHandler en attendant une réponse du Server
		 */
		private void waitResponse() {
			synchronized (PlayerHandler.this) {
				try {
					PlayerHandler.this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Pas d'entrée utilisateur.
	 */
	private class ServerCommunicator extends Communicator {

		private volatile boolean waitingRequest = true;
		Message send = null;

		@Override
		protected void setAttributes() {
			receiverName = "Server";
			receiverIP = serverIP;
			receiverPort = serverPort;
			handler = new ImplHostServerHandler();
		}

		/** Host <- Server */
		private class ImplHostServerHandler extends HostServerHandler {
			@Override
			protected void handleMessage(Message reception) {
				switch (reception.getType()) {
				case Message.IDOK:
					state = State.IN;
					wakeCommunicator();
					break;
				case Message.IDNO:
					disconnect();
					wakeCommunicator();
					break;
				case Message.RQDT:
					String username = getUsername(reception);
					if (username == null) {
						return;
					}
					List<String> args = new LinkedList<>();
					args.add(username);
					args.add(IP.getHostAddress());
					args.add(String.valueOf(port));
					args.add(name);
					args.add(String.valueOf(board.getCompletion()));
					inGamePlayers.values().forEach(p -> {
						args.add(p.username);
						args.add(String.valueOf(p.inGamePoints));
					});
					communicatorSocket.send(Message.SDDT, args.stream().toArray(String[]::new));
					break;
				case Message.PLNO:
					refusePlayer(reception);
					break;
				case Message.PLOK:
					acceptPlayer(reception);
					break;
				case Message.IDKS:
					System.out.println(receiverName + " reste béant : '" + reception + "'.");
				default:
					unknownMessage();
					break;
				}
			}

			private void refusePlayer(Message reception) {
				String username = getUsername(reception);
				if (username == null) {
					return;
				}
				Person waitingPlayer = standingByHelper.remove(username);
				if (waitingPlayer != null) {
					PlayerHandler ph = standingBy.remove(waitingPlayer);
					if (ph == null) {
						playerNotFound(waitingPlayer);
						return;
					}
					Message m = new Message(Message.JNNO, null, reception.getContent());
					synchronized (ph) {
						ph.notify();
					}
					ph.socket.send(m);
				} else {
					playerNotFound(waitingPlayer);
				}
			}

			private void acceptPlayer(Message reception) {
				String username = getUsername(reception);
				if (username == null) {
					return;
				}
				Person waitingPlayer = standingByHelper.remove(username);
				if (waitingPlayer != null) {
					PlayerHandler ph = standingBy.remove(waitingPlayer);
					if (ph == null) {
						playerNotFound(waitingPlayer);
						return;
					}
					InGamePlayer igp = new InGamePlayer(waitingPlayer, ph);
					igp.totalPoints = reception.getArgAsInt(1);
					inGamePlayers.put(igp.username, igp);
					ph.inGamePlayer = igp;
					synchronized (ph) {
						ph.identified = true;
						ph.notify();
					}
					ph.sendGameState(igp);
				} else {
					playerNotFound(waitingPlayer);
				}
			}

			protected String getUsername(Message reception) {
				String username = reception.getArg(0);
				if (username == null) {
					LOGGER.warning(
							"Anomalie : Pas de nom d'utilisateur donné par le serveur pour " + Message.RQDT + ".");
					System.err.println(
							"Anomalie : Pas de nom d'utilisateur donné par le serveur pour " + Message.RQDT + ".");
					return null;
				} else {
					return username;
				}
			}

			@Override
			protected synchronized void wakeCommunicator() {
				if (waitingResponse) {
					if (count > 0) {
						count--;
					}
					if (count > 0) {
						return;
					}
					synchronized (ServerCommunicator.this) {
						waitingResponse = false;
						waitingRequest = true;
						ServerCommunicator.this.notify();
					}
				}
			}
		}

		/** Transfert un message du Client au Serveur */
		public void communicate(Message message) {
			this.send = message;
			synchronized (ServerCommunicator.this) {
				waitingRequest = false;
				ServerCommunicator.this.notify();
			}
		}

		/**
		 * En boucle : Attend passivement jusqu'à requête Client. Une fois
		 * réveillé, envoie un message au Serveur. Attend une réponse du
		 * Serveur.
		 */
		@Override
		protected void communicate() {
			while (waitingRequest) {
				synchronized (ServerCommunicator.this) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			if (!waitingRequest) {
				communicatorSocket.send(send);
				waitingRequest = true;
			}

		}

		@Override
		public void login() {
			Message m = new Message(Message.LOGI, new String[] { name, password }, null);
			waitingRequest = false;
			waitingResponse = true;
			communicatorSocket.send(m);
			waitResponse();
		}

		@Override
		public synchronized void waitResponse() {
			synchronized (ServerCommunicator.this) {
				while (waitingResponse) {
					try {
						ServerCommunicator.this.wait(MAX_WAIT_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (waitingResponse) { // FUTURE compteur avant déconnexion
						System.err.println("Pas de réponse du Serveur. Nouvelle tentative.");
					}
				}
			}
		}

		public void playerNotFound(Person standingByPlayer) {
			if (standingByPlayer != null) {
				System.err.println("Le joueur " + standingByPlayer.name + " ne semble plus connecté.");
			}
		}

	}

}
