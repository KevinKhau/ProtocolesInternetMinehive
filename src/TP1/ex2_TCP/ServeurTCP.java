package TP1.ex2_TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

// TODO Repérer quand une connexion est fermée, et retirer les clients correspondants (par exemple quand arrêt d'un programme ClientTCP)
public class ServeurTCP {
	// TODO Déplacer nombreClients dans le Manager ?
	static int nombreClients = 0;
	final int connectionPort = 1027;
	Manager manager = new Manager();

	/**
	 * Envoie des messages à un client spécifique
	 */
	class Messagerie {
		Socket socket;
		PrintWriter pw;

		public Messagerie(Socket socket, PrintWriter pw) {
			this.socket = socket;
			this.pw = pw;
		}

		public void welcome(int id) {
			pw.write("Bienvenue ! Vous êtes le client " + id + ".\n");
			pw.flush();
		}

		public void send() {
			pw.write("Nombre de clients: " + nombreClients + ".\n");
			pw.flush();
		}

		public void alertNewcomer(int id) {
			pw.write("Un nouvel ami est arrivé : 'Client " + id + "' (" + socket.getInetAddress() + "/"
					+ socket.getPort() + ")\n");
			pw.flush();
		}
	}

	/**
	 * Synchronize les messages pour des communications simultanées
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
					// TODO ConcurrentModificationException aléatoire à corriger
					for (Messagerie m : messageries) {
						m.send();
					}
					Thread.sleep(sleepTimeMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		public synchronized void addMessenger(Messagerie m) {
			messageries.add(m);
			// TODO ID attribué par count pas totalement unique, parce qu'on
			// revient sur le même nombre après Integer.MAX*2
			m.welcome(count);
			for (Messagerie messagerie : messageries) {
				messagerie.alertNewcomer(count);
			}
			count++;
			if (messageries.size() == 1) {
				start();
			}
		}

		// TODO Trouver quand appeler cette méthode.
		public synchronized void removeMessenger(Messagerie m) {
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

	// TODO Vérifier que partout, nombreClients == manager.messageries.size() ==
	// [Print Writers ouverts]
	public void demarrer() {
		try (ServerSocket server = new ServerSocket(port)) {
		System.out.println("Lancement serveur : port " + connectionPort + ".");
			while (true) {
				Socket socket = server.accept();
				nombreClients++;
				InetAddress userAddress = socket.getInetAddress();
				int userPort = socket.getPort();

				// TODO Vérifier scrupuleusement s'ils sont fermés
				// convenablement
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				String message = br.readLine();
				System.out.println("(UserAddress/Port) " + userAddress + "/" + userPort + " : " + message);
				manager.addMessenger(new Messagerie(socket, pw));

			}
		} catch (IllegalArgumentException e) {
			System.err.println("Valeur de port invalide, doit être entre 0 et 65535");
		} catch (IOException e) {
			System.err.println("Problème de traitement de la socket : port " + connectionPort);
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ServeurTCP server = new ServeurTCP();
		server.demarrer();
	}

}
