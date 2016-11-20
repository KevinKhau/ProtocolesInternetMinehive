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

	public static final String ALL = "ALL";
	public static final int MAX_ONLINE = 110;

	Map<String, Player> users = getPlayersFromXML();
	Map<Player, ClientHandler> available = new ConcurrentHashMap<>();

	Map<String, HostData> hostsDataHelper = new ConcurrentHashMap<>();
	Map<HostData, SocketHandler> hosts = new ConcurrentHashMap<>();

	Map<Player, HostData> inGame = new ConcurrentHashMap<>();

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

	/**
	 * Gère un seul client. // TODO rendre générique avec SocketHandler
	 */
	private class ClientHandler extends SocketHandler {

		/** Correspond à l'entityData casté. initialisé lors de identification() */ 
		Player player; 
		
		public ClientHandler(TFSocket socket) {
			super(socket, Player.NAME);
		}

		/**
		 * À REGI : identifie un joueur déjà existant, l'enregistre, ou invite à
		 * réessayer jusqu'à validation. Gère les réponses {@link Message#IDOK},
		 * {@link Message#IDNO}, {@link Message#IDIG}.
		 * 
		 * @return Un joueur identifié ou créé avec succès, ou tentatives
		 *         récursives sinon.
		 * @throws IOException
		 */
		@Override
		public void identification() throws IOException {
			if (!running) {
				disconnect();
				return;
			}
			Message message = socket.receive();
			if (message.getType().equals(Message.LEAV)) {
				System.out.println("Fin de la connexion avec " + socket.getRemoteSocketAddress() + ".");
				disconnect();
				return;
			}
			/* Serveur saturé */
			if (isFull()) {
				socket.send(Message.IDNO, null, "Le serveur est plein. Réessayez ultérieurement.");
				identification();
				return;
			}
			/* Anomalies */
			if (!message.getType().equals(Message.REGI)) {
				socket.send(Message.IDKS, null, "Vous devez d'abord vous connecter : REGI Username Password");
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
			entityData = users.get(username);
			/* Première fois */
			if (entityData == null) {
				player = new Player(username, password, Player.INITIAL_POINTS);
				users.put(username, player);
				writePlayer( player);
				socket.send(Message.IDOK, null, "Bienvenue " + username + " !");
			}
			player = (Player) entityData;
			/* Mauvais mot de passe */
			if (!player.password.equals(password)) {
				socket.send(Message.IDNO, null, "Mauvais mot de passe");
				identification();
				return;
			}
			/* Déjà connecté */
			if (available.containsKey(player)) {
				System.out.println("Connection override de " + player.username + ".");
				socket.send(Message.IDOK, null, "Bon retour " + username + " !");
				return;
			}
			/* Déjà en partie */
			HostData hd = inGame.get(player);
			if (hd != null) {
				socket.send(Message.IDIG, new String[] { hd.getIP().toString(), String.valueOf(hd.getPort()) },
						"Finissez votre partie en cours !");
				identification();
				return;
			}
			/* Connexion classique */
			socket.send(Message.IDOK, null, "Bonjour " + username + " !");
			return;
		}
		
		@Override
		protected void addEntityData() {
			addAvailable(player, this);
			System.out.println("Utilisateur '" + player.username + "' connecté depuis "
					+ socket.getRemoteSocketAddress() + ".");
		}

		@Override
		protected void handleMessage(Message reception) {
			switch (reception.getType()) {
			case Message.IMOK: // Permet de reset le SO_TIMEOUT de la socket
				break;
			case Message.REGI:
				socket.send(Message.IDKS, null, "Vous êtes déjà connecté !");
				break;
			case Message.LSMA:
				socket.send(Message.LMNB, new String[]{String.valueOf(hosts)});
				hosts.values().forEach(handler -> handler.socket.send(Message.RQDT, new String[]{player.username}));
				break;
			case Message.LSAV:
				sendAvailable(reception);
				break;
			case Message.LSUS:
				sendUsers(reception);
				break;
			case Message.NWMA:
				createMatch(reception);
				break;
			case Message.LEAV:
				System.out.println(
						"Déconnexion de l'utilisateur " + player.username + ", " + socket.getRemoteSocketAddress());
				disconnect();
				break;
			default:
				unknownMessage();
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
			if (hosts.size() >= 10) {
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

		@Override
		protected void removeEntityData() {
			if (entityData != null) {
				available.remove(entityData);
			}
		}

		@Override
		protected void unknownMessage() {
			socket.send(Message.IDKS, null, "Commande inconnue ou pas encore implémentée");
		}
	}

	private class HostHandler extends SocketHandler {

		public HostHandler(TFSocket socket) {
			super(socket, HostData.NAME);
		}

		@Override
		protected void identification() throws IOException {
			if (!running) {
				disconnect();
				return;
			}
			// Pas besoin de vérifier qu'il y a de la place, c'est fait à la création du Host
			Message message = socket.receive();
			if (!message.getType().equals(Message.LOGI)) {
				unknownMessage();
				disconnect();
				return;
			}
			String matchName = message.getArg(0);
			entityData = hostsDataHelper.get(matchName); 
			if (entityData == null) {
				socket.send(Message.IDNO, null, "Nom de partie inconnu.");
				disconnect();
				return;
			} else {
				socket.send(Message.IDOK, null, "C'est parti, " + entityData.name + " !");
				return;
			}
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
				hostsDataHelper.remove(entityData.name);
				hosts.remove(entityData);
				disconnect();
				break;
			default:
				unknownMessage();
				break;
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
		
		@Override
		protected void addEntityData() {
			hosts.put((HostData) entityData, this);
		}

		@Override
		protected void removeEntityData() {
			if (entityData != null) {
				hosts.remove(entityData);
			}
		}

		@Override
		protected void unknownMessage() {
			socket.send(Message.IDKS, null, "Commande inconnue ou pas encore implémentée");
		}

	}

}
