package TP1.ex2_TCP;

import java.io.BufferedReader;
import java.io.Closeable;
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

public class ServeurTCP {
	final int connectionPort = 1027;
	Manager manager = new Manager();
	public final static String RUOK = "RUOK";
	public final static int activeDelay = 500;

	/**
	 * Envoie des messages à un client spécifique
	 */
	class Messagerie implements Closeable {
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
					while (true) {
						out.println(RUOK);
						try {
							String rep = in.readLine(); 
							if (rep == null || !rep.equals(ClientTCP.IMOK)) {
								System.err.println("Le client " + Messagerie.this.clientID
										+ " a donné une réponse anormale. Fermeture de la connexion.");
								socket.close();
								break;
							}
						} catch (IOException e) {
							System.err.println("Le client " + Messagerie.this.clientID
									+ " ne répond pas. Fermeture de la connexion.");
							try {
								socket.close();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							break;
						}
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
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

		@Override
		public void close() throws IOException {
			out.close();
			in.close();
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
		@Override
		public void run() {
			while (true) {
				try {
					synchronized (this) {
						messageries.removeIf(m -> inactive(m)); // Java 8 <3
						if (messageries.isEmpty()) {
							wait(); // pour ne pas faire tourner inutilement le while(true)
						}
						messageries.forEach(m -> {
							new Thread() {
								@Override
								public void run() {
									m.send(messageries.size());
								}
							}.start();
						});
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
			messageries.forEach(i -> i.alertNewcomer(count));
			count++;
			m.checkOK();
			if (messageries.size() == 1) {
				try {
					start(); // 1ère fois, à run()
				} catch (IllegalThreadStateException e) {
					notify(); // par la suite, à wait()
				}
			}
		}

		public boolean inactive(Messagerie m) {
			if (m.socket.isClosed()) {
				System.out.println("Suppression du client " + m.clientID + ".");
				return true;
			}
			return false;
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
			e.printStackTrace();
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
