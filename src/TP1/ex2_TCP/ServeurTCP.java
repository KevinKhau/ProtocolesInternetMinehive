package TP1.ex2_TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;

// TODO Repérer quand une connexion est fermée, et retirer les clients correspondants (par exemple quand arrêt d'un programme ClientTCP)
public class ServeurTCP {
	// TODO Déplacer nombreClients dans le Manager ?
	final int connectionPort = 1027;
	Manager manager = new Manager();
	public final static String RUOK = "RUOK";
	public final static int activeDelay = 5000;

	/**
	 * Envoie des messages à un client spécifique
	 */
	class Messagerie {
		Socket socket;
		PrintWriter out;
		BufferedReader in;
		
		int clientID;
		
		public Messagerie(Socket socket, PrintWriter pw, BufferedReader br) {
			this.socket = socket;
			this.out = pw;
			this.in = br;
		}

		public void welcome(int id) {
			clientID = id;
			out.println("Bienvenue ! Vous êtes le client " + id + ".");
		}

		public void send(int nombreClients) {
			out.println("Nombre de clients: " + nombreClients + ".");
		}

		/**
		 * Vérifie en parallèle que le client est toujours actif.
		 */
		public void checkOK() {
			new Thread() {
				@Override
				public void run() {
					out.println(RUOK);
					try {
						if (!in.readLine().equals(ClientTCP.IMOK)) {
							socket.close();
						}
					} catch (IOException e) {
						System.err.println("Le client "  + Messagerie.this.clientID + " ne répond pas. Fermeture de la connexion.");
						try {
							socket.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}.start();
		}

		public void alertNewcomer(int id) {
			out.println("Un nouvel ami est arrivé : 'Client " + id + "' (" + socket.getInetAddress() + "/"
					+ socket.getPort() + ")");
			out.flush();
		}
	}

	/**
	 * Synchronise les messages pour des communications simultanées
	 */
	class Manager extends Thread {
		final int sleepTimeMs = 10000;
		int count = 1; // Pour l'ID d'un client, pas obligatoirement en relation
						// avec le nombre total de clients
		private LinkedList<Messagerie> messageries = new LinkedList<>();

		/**
		 * Appelle la méthode .send() de chaque messagerie toutes les 10s
		 */
		public void run() {
			while (true) {
				try {
					synchronized (this) {
						for (Messagerie m : messageries) {
							if (m.socket.isClosed()) {
								removeMessenger(m);
							}
							m.send(messageries.size());
							m.checkOK();
						}
					}
					Thread.sleep(sleepTimeMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		public synchronized void addMessenger(Messagerie m) {
			messageries.add(m);
			m.welcome(count);
			for (Messagerie messagerie : messageries) { // TODO en threads pour
														// simultané
				messagerie.alertNewcomer(count);
			}
			count++;
			if (messageries.size() == 1) {
				// TODO notify() à la place si reprise
				start();
			}
		}

		public synchronized void removeMessenger(Messagerie m) {
			System.out.println("Suppression du client " + m.clientID + ".");
			messageries.remove(m);
			if (messageries.isEmpty()) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public void demarrer() {
		try (ServerSocket server = new ServerSocket(connectionPort)) {
			System.out.println("Lancement serveur : port " + connectionPort + ".");
			while (true) {
				Socket socket = server.accept();
				socket.setSoTimeout(activeDelay);
				InetAddress userAddress = socket.getInetAddress();
				int userPort = socket.getPort();

				// TODO Vérifier scrupuleusement s'ils sont fermés
				// convenablement
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
				String message = br.readLine();
				System.out.println("(UserAddress/Port) " + userAddress + "/" + userPort + " : " + message);
				manager.addMessenger(new Messagerie(socket, pw, br));

			}
		} catch (SocketTimeoutException e) {
			System.err.println("Le client n'a pas répondu à temps.");
		} catch (BindException e) {
			System.err.println("Socket serveur déjà en cours d'utilisation.");
		} catch (IllegalArgumentException e) {
			System.err.println("Valeur de port invalide, doit être entre 0 et 65535.");
		} catch (IOException e) {
			System.err.println("Problème de traitement de la socket : port " + connectionPort + ".");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ServeurTCP server = new ServeurTCP();
		server.demarrer();
	}

}
