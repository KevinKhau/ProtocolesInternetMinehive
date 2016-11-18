package network;

import static util.Message.validArguments;
import static java.lang.String.valueOf;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
		super();
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.name = name;
		this.IP = IP;
		this.port = port;

		try {
			ServerSocket ss = new ServerSocket(port);
			// FUTURE établir connexion
		} catch (IOException e) {
			deny("Paramètre n°5 invalide, port occupé");
		}
	}

	private class InGamePlayer extends Player {

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

		@Override
		public void run() {

		}

		public Player identification() throws IOException, InterruptedException {
			if (!running) {
				throw new InterruptedException();
			}
			Message message = in.receive();
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

			/* TODO Server-Host Nouvelle connexion */
			InGamePlayer p2 = new InGamePlayer(username, password, this);
			inGamePlayers.put(username, p2);
			this.player = p2;
			out.send(Message.JNOK, null, "Bienvenue " + username + " !");
			return p2;
		}

		private void sendGameState(InGamePlayer player) {
			/* Send board ; active=true */
			out.send(Message.JNOK, new String[] { String.valueOf(Board.HEIGHT) },
					"Bon retour " + player.username + " !");
			for (int y = 0; y < Board.HEIGHT; y++) {
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

		@Override
		public void close() throws Exception {
			player.active = false;
		}

	}

}
