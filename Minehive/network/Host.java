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
import java.util.logging.Level;
import java.util.logging.Logger;

import data.Player;
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

	public static final int ACTIVE_DELAY = 30000;
	public static final int CONNECTED_DELAY = 10000;

	InetAddress serverIP;
	int serverPort;
	private volatile ServerCommunicator serverCommunicator;

	InetAddress IP;
	int port;

	Board board = new Board();
	volatile int multiplicator = 0;

	volatile Map<String, Player> standingByHelper = new ConcurrentHashMap<>();
	volatile Map<Player, PlayerHandler> standingBy = new ConcurrentHashMap<>();

	volatile Map<String, InGamePlayer> inGamePlayers = new ConcurrentHashMap<>();

	static Logger launchLogger = Logger.getLogger("HostLaunch");
	
	private static void deny(String message) {
		System.err.println(message);
		System.err.println("Attendu : java Host serverIP serverPort hostName hostIP hostPort");
		launchLogger.log(Level.SEVERE, message);
		System.exit(1);
	}

	public static void main(String[] args) {
		try {
			Path logPath = Paths.get(Params.DIR_BIN, Params.DIR_LOG, "HostLaunch" + "Log.xml");
			launchLogger.addHandler(new FileHandler(logPath.toString()));
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
		if (Params.DEBUG_HOST) {
			try { // TEST
				if (args.length < 1) {
					System.err.println("Paramètre port de connexion pour les clients attendu.");
					System.exit(1);
				}
				new Host(InetAddress.getLocalHost(), 7777, "Partie_1", InetAddress.getLocalHost(), Integer.parseInt(args[0])); // TEST
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
			return;
		}
		
		launchLogger.log(Level.SEVERE, Arrays.toString(args));
		launchLogger.log(Level.CONFIG, "first");
		System.out.println(String.join(" ", args));
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
		new Host(serverIP, serverPort, args[2], hostIP, hostPort);
	}

	public Host(InetAddress serverIP, int serverPort, String name, InetAddress IP, int port) {
		super(name);
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.IP = IP;
		this.port = port;

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

	class InGamePlayer extends Player { // THINK Classe externe ?

		public static final String NAME = "Jouer Actif";

		PlayerHandler handler;
		volatile boolean active;
		volatile int inGamePoints = 0;

		volatile int safeSquares = 0;
		volatile int foundMines = 0;

		/** Nouveau joueur */
		private InGamePlayer(Player player, PlayerHandler handler) {
			super(player.username, player.password);
			LOGGER.warning("Nouveau joueur connecté " + player);
			this.handler = handler;
			setActive();
		}

		private String[] publicData() {
			return new String[] { this.username, valueOf(inGamePoints), valueOf(totalPoints), valueOf(safeSquares),
					valueOf(foundMines) };
		}

		private void setActive() {
			if (!active) {
				active = true;
				synchronized (Host.this) {
					multiplicator++;
				} 
			}
		}

		private void setInactive() {
			if (active) {
				active = false;
				synchronized (Host.this) {
					multiplicator--;
				} 
			}
		}

	}

	/** Host <- Client */
	class PlayerHandler extends SenderHandler {

		/** Correspond à l'entityData casté. Initialisé lors de identification() */
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
					LOGGER.finest(rcv.toString());
					try {
						handleMessage(rcv);
					} catch (NullPointerException e) {
						LOGGER.warning(e.getMessage() + " : Mauvais nombre d'arguments reçu.");
					}
				}
			} catch (IOException | IllegalArgumentException e) {
				LOGGER.fine("Déconnexion " + e.getMessage());
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
					inGamePlayer = (InGamePlayer) senderData;
					LOGGER.fine("Tentative de reconnexion de " + inGamePlayer);
					if (!inGamePlayer.checkPassword(password)) {
						socket.send(Message.JNNO, null, "Mot de passe incorrect.");
						continue;
					}

					if (inGamePlayer.active) {
						socket.send(Message.JNNO, null, "Vous semblez déjà en train de jouer.");
						continue;
						// THINK kick() ?
					}

					inGamePlayer.setActive();
					sendGameState(inGamePlayer);
					break;
				}

				/* Hôte saturé */
				if (inGamePlayers.size() >= MAX_PLAYERS) {
					socket.send(Message.JNNO, null, "Nombre maximum de joueurs atteint. Plus de place disponible !");
					continue;
				}

				/* Nouvelle connexion */
				Player p = new Player(username, password);
				standingByHelper.put(p.username, p);
				standingBy.put(p, this);
				serverCommunicator.communicate(new Message(Message.PLIN, new String[]{Host.this.name, username, password}, null));
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
				socket.send(Message.BDIT, lineContent.stream().toArray(String[]::new));
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
			case Message.IMOK: // Permet de reset le SO_TIMEOUT de la socket
				break;
			case Message.CLIC:
				int abscissa = reception.getArgAsInt(0);
				int ordinate = reception.getArgAsInt(1);

				if (!board.validAbscissa(abscissa) || !board.validOrdinate(ordinate)) {
					socket.send(Message.OORG, new String[]{valueOf(abscissa), valueOf(ordinate)}, "Coordonnées invalides ! ");
					break;
				}

				List<String[]> allArgs = board.clickAt(abscissa, ordinate, inGamePlayer.username);
				if (allArgs == null) {
					socket.send(Message.LATE, null, "Case déjà déminée.");
					break;
				}

				for (String[] line : allArgs) {
					line[3] = valueOf(Integer.parseInt(line[3]) * multiplicator);
					for (InGamePlayer igp : inGamePlayers.values()) {
						igp.handler.socket.send(Message.SQRD, line);
					}
				}
				// TODO Fin de partie ?
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
				LOGGER.fine(inGamePlayer + " désormais inactif.");
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
					LOGGER.severe("Received RQDT");
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
				Player waitingPlayer = standingByHelper.remove(username);
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
				Player waitingPlayer = standingByHelper.remove(username);
				if (waitingPlayer != null) {
					PlayerHandler ph = standingBy.remove(waitingPlayer);
					if (ph == null) {
						playerNotFound(waitingPlayer);
						return;
					}
					waitingPlayer.totalPoints = reception.getArgAsInt(1);
					InGamePlayer igp = new InGamePlayer(waitingPlayer, ph);
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
					LOGGER.warning("Anomalie : Pas de nom d'utilisateur donné par le serveur pour " + Message.RQDT + ".");
					System.err.println("Anomalie : Pas de nom d'utilisateur donné par le serveur pour " + Message.RQDT + ".");
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
		 * En boucle : 
		 * Attend passivement jusqu'à requête Client.
		 * Une fois réveillé, envoie un message au Serveur.
		 * Attend une réponse du Serveur.
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
			Message m = new Message(Message.LOGI, new String[] { name }, null);
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

		public void playerNotFound(Player standingByPlayer) {
			if (standingByPlayer != null) {
				System.err.println("Le joueur " + standingByPlayer.name + " ne semble plus connecté.");
			}
		}

	}

}
