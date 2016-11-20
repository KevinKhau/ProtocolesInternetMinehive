package network;

import static util.Message.validArguments;
import static util.PlayersManager.getPlayersFromXML;
import static util.PlayersManager.writePlayer;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import data.EntityData;
import data.HostData;
import data.Player;
import util.Message;
import util.PlayersManager;
import util.TFServerSocket;
import util.TFSocket;

/**
 * Reste attentif à la connexion de nouveaux clients ou hôtes
 */
public class Server extends Entity {

	InetAddress serverIP;
	final int serverPort = 5555;

	Map<String, Player> users = getPlayersFromXML();

	public static final String ALL = "ALL";
	public static final int MAX_ONLINE = 110;
	Map<Player, ClientHandler> available = new ConcurrentHashMap<>();
	Map<Player, HostData> inGame = new ConcurrentHashMap<>();

	Map<String, EntityData> hostsDataHelper = new ConcurrentHashMap<>();
	Map<EntityData, SocketHandler> hostsData = new ConcurrentHashMap<>();

	public static final int ACTIVE_DELAY = 300000;

	public static void main(String[] args) {
		new Server();
	}

	public Server() {
		name = "Serveur";
		try (TFServerSocket server = new TFServerSocket(serverPort)) {
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

	public synchronized boolean addAvailable(Player player, ClientHandler handler) {
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
	 * Obtenir le Handler gérant la connexion d'un client disponible à partir d'un nom d'utilisateur
	 * @param username
	 */
	public ClientHandler getHandler(String username) {
		if (username == null) {
			System.err.println("Message anormale reçu. Nom de joueur requis.");
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

	/** Gère la connexion et les messages avec une autre entité */
	private abstract class SocketHandler extends Thread {
		// TODO gérer inactivité client
		TFSocket socket;
		protected volatile boolean running = true;

		EntityData entityData;
		String entityName = "Entité";
		Map<String, EntityData> helper;
		Map<EntityData, SocketHandler> list;

		public SocketHandler(TFSocket socket, String name) {
			super();
			this.entityName = name;
			System.out.println(
					"Nouvelle connexion entrante " + entityName + " : " + socket.getRemoteSocketAddress());
			this.socket = socket;
			this.socket.ping();
		}

		@Override
		public void run() {
			try {
				entityData = link();
				while (running) {
					Message rcv = socket.receive();
					System.out.println(rcv);
					try {
						handleMessage(rcv);
					} catch (NullPointerException e) {
						System.err.println("Mauvais nombre d'arguments reçu.");
					}
				}
			} catch (IOException e) {
				disconnect();
			}
		}

		/** Instructions d'initialisation avec l'expéditeur */
		protected abstract EntityData link() throws IOException;

		/** Vérification de messages en boucle */
		protected abstract void handleMessage(Message reception);

		protected void unknownMessage() {
			System.err.println("Message inconnu de " + entityName);
			socket.send(Message.IDKC);
		}

		protected void disconnect() {
			running = false;
			if (entityData != null) {
				list.remove(entityData);
			}
			socket.close();
		}

	}

	/**
	 * Gère un seul client. // TODO rendre générique avec SocketHandler
	 */
	private class ClientHandler extends Thread {
		TFSocket socket;

		private volatile boolean running = true;

		Player player;

		public ClientHandler(TFSocket socket) {
			super();
			System.out.println("Nouvelle connexion : " + socket.getRemoteSocketAddress());
			this.socket = socket;
			this.socket.ping();
		}

		@Override
		public void run() {
			try {
				player = identification();
				if (player != null) {
					addAvailable(player, this);
					System.out.println("Utilisateur '" + player.username + "' connecté depuis "
							+ socket.getRemoteSocketAddress() + ".");
				}
				while (running) {
					handleMessage();
				}
			} catch (InterruptedException e) {
				System.err.println(e.getMessage());
				disconnect();
			} catch (IOException e) {
				disconnect();
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
			Message message = socket.receive();
			if (message.getType().equals(Message.LEAV)) {
				System.out.println("Fin de la connexion avec " + socket.getRemoteSocketAddress() + ".");
				disconnect();
				return null;
			}
			/* Serveur saturé */
			if (isFull()) {
				socket.send(Message.IDNO, null, "Le serveur est plein. Réessayez ultérieurement.");
				return identification();
			}
			/* Anomalies */
			if (!message.getType().equals(Message.REGI)) {
				socket.send(Message.IDKS, null, "Vous devez d'abord vous connecter : REGI Username Password");
				return identification();
			}
			/* Mauvais nombre d'arguments */
			if (!validArguments(message)) {
				socket.send(Message.IDNO, null, "Identifiant et/ou Mot de passe manquant");
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
				socket.send(Message.IDOK, null, "Bienvenue " + username + " !");
				return p;
			}
			/* Mauvais mot de passe */
			if (!p.password.equals(password)) {
				socket.send(Message.IDNO, null, "Mauvais mot de passe");
				return identification();
			}
			/* Déjà connecté */
			if (available.containsKey(p)) {
				System.out.println("Connection override de " + p.username + ".");
				socket.send(Message.IDOK, null, "Bon retour " + username + " !");
				return p;
			}
			/* Déjà en partie */
			HostData hd = inGame.get(p);
			if (hd != null) {
				socket.send(Message.IDIG, new String[] { hd.getIP().toString(), String.valueOf(hd.getPort()) },
						"Finissez votre partie en cours !");
				return identification();
			}
			/* Connexion classique */
			socket.send(Message.IDOK, null, "Bonjour " + username + " !");
			return p;
		}

		/**
		 * Gère toutes les requêtes possibles du client après son
		 * identification.
		 * 
		 * @throws InterruptedException
		 * @throws IOException
		 */
		private void handleMessage() throws IOException {
			Message msg = socket.receive();
			switch (msg.getType()) {
			case Message.IMOK: // Permet de reset le SO_TIMEOUT de la socket
				break;
			case Message.REGI:
				socket.send(Message.IDKS, null, "Vous êtes déjà connecté !");
				break;
			case Message.LSMA:
				// TODO Traitement LSMA
				socket.send(Message.IDKS, null, "LSMA en cours d'implémentation");
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
				disconnect();
				break;
			default:
				socket.send(Message.IDKS, null, "Commande inconnue ou pas encore implémentée");
				break;
			}
		}

		private synchronized void sendAvailable(Message msg) {
			socket.send(Message.LANB, new String[] { String.valueOf(available.size()) });
			for (Player p : available.keySet()) {
				socket.send(Message.AVAI, new String[] { p.username, String.valueOf(p.totalPoints) });
			}
		}

		private synchronized void sendUsers(Message msg) {
			socket.send(Message.LUNB, new String[] { String.valueOf(users.size()) });
			for (Player p : users.values()) {
				socket.send(Message.USER, new String[] { p.username, String.valueOf(p.totalPoints) });
			}
		}

		private void createMatch(Message msg) {
			if (hostsData.size() >= 10) {
				socket.send(Message.FULL, null, "Trop de parties en cours. Réessayez ultérieurement.");
				return;
			}
			HostData hd = null;
			try {
				hd = new HostData();
			} catch (IOException e) {
				socket.send(Message.NWNO, null, e.getMessage());
			}
			// Runtime -> java [Host path] serverIP serverPort hd.getName()
			// hd.getIP() hd.getPort() // FUTURE Lancer programme externe
			String[] sendArgs = new String[] { hd.getIP().toString(), String.valueOf(hd.getPort()) };
			// FUTURE corriger après dev future
			socket.send(Message.NWOK, sendArgs,
					"Votre partie a été créée. Mais n'y allez pas encore (jeu à implémenter) !");

			/* Aucun invité */
			if (msg.getArgs() == null) {
				return;
			}

			/* ALL : inviter tous les joueurs disponibles */
			String arg1 = msg.getArg(0);
			if (arg1 != null && arg1.equals(ALL)) {
				for (ClientHandler h : available.values()) {
					h.socket.send(Message.NWOK, sendArgs, player.username + " vous défie !");
				}
			}

			/* Liste spécifique de joueurs invités */
			for (String playerName : msg.getArgs()) {
				Player p = users.get(playerName);
				if (p == null) {
					continue;
				}
				ClientHandler h = available.get(p);
				if (h != null) {
					h.socket.send(Message.NWOK, sendArgs, player.username + " vous défie !");
				}
			}
		}

		/**
		 * Le serveur interrompt la connexion avec ce client. Player ne doit pas
		 * être null, et donc la connexion doit déjà avoir été établie.
		 */
		public void kick() {
			socket.send(Message.KICK);
			disconnect();
		}

		/**
		 * Autorise la Thread à s'arrêter, enlève le Player correspondant de
		 * Thread s'il existe. Ferme la socket et les streams associés.
		 */
		public void disconnect() {
			running = false;
			if (player != null) {
				available.remove(player);
			}
			socket.close();
		}
	}

	private class HostHandler extends SocketHandler {

		public HostHandler(TFSocket socket) {
			super(socket, HostData.NAME);
			helper = hostsDataHelper;
			list = hostsData;
		}

		@Override
		protected EntityData link() throws IOException {
			if (!running) {
				disconnect();
			}
			// Pas besoin de vérifier qu'il y a de la place, c'est fait à la création du Host
			Message message = socket.receive();
			if (!message.getType().equals(Message.LOGI)) {
				unknownMessage();
				disconnect();
				return null;
			}
			String matchName = message.getArg(0);
			entityData = helper.get(matchName); 
			if (entityData == null) {
				socket.send(Message.IDNO, null, "Nom de partie inconnu.");
				disconnect();
				return null;
			}
			socket.send(Message.IDOK, null, "C'est parti, " + entityData.name + " !");
			list.put(entityData, this);
			return entityData;
		}

		@Override
		protected void handleMessage(Message reception) {
			ClientHandler ch;
			switch (reception.getType()) {
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
					System.err.println("Message anormale de l'hôte : Mot de passe non défini. Rappel : PLIN#MatchName#Username#Password");
					break;
				}
				Player p = getPlayer(username);
				if (p == null) {
					socket.send(Message.PLNO, new String[]{username}, "Utilisateur inexistant.");
					break;
				}
				if (!p.password.equals(password)) {
					socket.send(Message.PLNO, new String[]{username}, "Mauvais mot de passe.");
				}
				if (inGame.containsKey(p)) {
					socket.send(Message.PLNO, new String[]{username}, username + " a déjà une partie en cours, qu'il doit finir !");
				}
				socket.send(Message.PLOK, new String[]{username, String.valueOf(p.totalPoints)});
				inGame.put(p, (HostData) entityData);
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
				helper.remove(entityData.name);
				list.remove(entityData);
				disconnect();
				break;
			default:
				unknownMessage();
			}
		}
		
		private Player getPlayer(String username) {
			if (username == null) {
				System.err.println("Message anormale de l'hôte : Nom d'utilisateur non défini.");
				return null;
			}
			Player p = users.get(username);
			if (p == null) {
				socket.send(Message.PLNO, new String[]{username}, "Utilisateur inexistant.");
				return null;
			}
			return p;
		}

	}

}
