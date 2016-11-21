package network;

import static java.lang.String.valueOf;
import static util.Message.validArguments;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import data.Player;
import game.Board;
import network.Communicator.State;
import util.Message;
import util.TFServerSocket;
import util.TFSocket;

/**
 * Hôte lancé par le serveur //FUTURE Programmer lancement par serveur au lieu
 * de manuel une fois dév achevée
 */
public class Host extends Entity {

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

	private static void deny(String message) {
		System.err.println(message);
		System.err.println("Attendu : java Host serverIP serverPort hostName hostIP hostPort");
		System.exit(1);
	}

	public static void main(String[] args) {
		try { // TEST
			if (args.length < 1) {
				System.err.println("Paramètre port de connexion pour les clients attendu.");
				System.exit(1);
			}
			new Host(InetAddress.getLocalHost(), 7777, "Partie_1", InetAddress.getLocalHost(), Integer.parseInt(args[0])); // TEST
//			new Host(null, 7777, "HostTest", InetAddress.getLocalHost(), 3333);
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
		super(name);
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.IP = IP;
		this.port = port;

		serverCommunicator = new ServerCommunicator();
		new Thread(serverCommunicator).start();
		
		// FUTURE Attente passive
		while (serverCommunicator.running) {
			if (serverCommunicator == null || serverCommunicator.state == State.OFFLINE) {
				continue;
			}
			try (TFServerSocket ss = new TFServerSocket(port)) {
				System.out.println("Lancement hôte : IP=" + IP + ", port=" + port + ".");
				// FUTURE  extends Listener
				new Thread(new PlayerHandler(ss.accept())).start();
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
	}

	class InGamePlayer extends Player { // THINK Classe externe ?

		public static final String NAME = "Jouer Actif";

		PlayerHandler handler;
		volatile boolean active;
		volatile int inGamePoints = 0;

		volatile int safeSquares = 0;
		volatile int foundMines = 0;

		/** Reconnexion */
		private InGamePlayer(String username, String password, PlayerHandler handler) {
			super(username, password);
			this.handler = handler;
			setActive();
		}

		/** Nouveau joueur */
		private InGamePlayer(Player player, PlayerHandler handler) {
			super(player.username, player.password);
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

	/** Host <- Client */
	class PlayerHandler extends SenderHandler {

		/** Correspond à l'entityData casté. Initialisé lors de identification() */
		InGamePlayer inGamePlayer;

		public PlayerHandler(TFSocket socket) {
			super(socket, InGamePlayer.NAME);
		}

		@Override
		public void identification() throws IOException {
			if (!running) {
				disconnect();
				return;
			}
			Message message = socket.receive();
			/* Anomalies */
			if (!message.getType().equals(Message.JOIN)) {
				socket.send(Message.IDKH, null, "Vous devez d'abord vous identifier : JOIN Username Password");
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
			senderData = inGamePlayers.get(username);

			/* Reconnexion */
			if (senderData != null) {
				inGamePlayer = (InGamePlayer) senderData;
				if (!inGamePlayer.checkPassword(password)) {
					socket.send(Message.JNNO, null, "Mot de passe incorrect.");
					identification();
					return;
				}

				if (inGamePlayer.active) {
					socket.send(Message.JNNO, null, "Vous semblez déjà en train de jouer.");
					identification(); // THINK kick() ?
					return;
				}

				inGamePlayer.setActive();
				sendGameState(inGamePlayer);
				return;
			}

			/* Hôte saturé */
			if (inGamePlayers.size() >= MAX_PLAYERS) {
				socket.send(Message.JNNO, null, "Nombre maximum de joueurs atteint. Plus de place disponible !");
				identification();
				return;
			}

			/* Nouvelle connexion */
			Player p = new Player(username, password);
			standingByHelper.put(p.username, p);
			standingBy.put(p, this);
			serverCommunicator.transferRequest(new Message(Message.PLIN, new String[]{Host.this.name, username, password}, null));
			return;
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
			inGamePlayers.values().stream().filter(p -> (!p.equals(player) && p.active)).forEach(p -> socket.send(Message.CONN, player.publicData()));
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
				}

				List<String[]> allArgs = board.clickAt(abscissa, ordinate, inGamePlayer.username);
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
				inGamePlayer.active = false;
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
				String username;
				Player waitingPlayer;
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
					username = getUsername(reception);
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
					wakeCommunicator();
					username = getUsername(reception);
					if (username == null) {
						return;
					}
					waitingPlayer = standingByHelper.remove(username);
					if (waitingPlayer != null) {
						Message m = new Message(Message.JNNO, null, reception.getContent());
						transferResponse(waitingPlayer, m);
					} else {
						playerNotFound(waitingPlayer);
					}
					break;
				case Message.PLOK:
					wakeCommunicator();
					username = getUsername(reception);
					if (username == null) {
						return;
					}
					waitingPlayer = standingByHelper.remove(username);
					if (waitingPlayer != null) {
						PlayerHandler ph = standingBy.remove(waitingPlayer);
						if (ph == null) {
							playerNotFound(waitingPlayer);
							return;
						}
						InGamePlayer igp = new InGamePlayer(waitingPlayer, ph);
						inGamePlayers.put(igp.username, igp);
						ph.inGamePlayer = igp;
						ph.sendGameState(igp);
					} else {
						playerNotFound(waitingPlayer);
					}
					break;
				case Message.IDKS:
					System.out.println(receiverName + " reste béant : '" + reception + "'.");
				default:
					unknownMessage();
					break;
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
		public void transferRequest(Message message) {
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
				waitResponse();
			}
			
		}
		
		@Override
		protected void login() {
			communicatorSocket.send(Message.LOGI, new String[] { name });
			waitResponse();
		}

		/** Attend une réponse du Serveur à destination du Client */
		@Override
		public synchronized void waitResponse() {
			synchronized (ServerCommunicator.this) {
				waitingResponse = true;
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
		
		/** Transfert un message reçu du Serveur au Client */
		public void transferResponse(Player standingByPlayer, Message reception) {
			PlayerHandler ph = standingBy.remove(standingByPlayer);
			if (ph == null) {
				playerNotFound(standingByPlayer);
				return;
			}
			ph.socket.send(reception);
		}

		public void playerNotFound(Player standingByPlayer) {
			System.err.println("Le joueur " + standingByPlayer.name + " ne semble plus connecté.");
		}
		
	}

}
