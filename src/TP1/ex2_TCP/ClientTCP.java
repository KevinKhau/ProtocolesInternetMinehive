package TP1.ex2_TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ClientTCP extends Thread {
	int ID;
	final int serverPort = 1027;
	String addressName = "localhost";
	boolean running = true;
	public final static String IMOK = "IMOK";

	@Override
	public synchronized void run() {
		try (Socket socket = new Socket(addressName, serverPort);
				PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
			outToServer.println("Bonjour");
			String firstMsg = inFromServer.readLine();
			ID = Integer.parseInt(firstMsg.substring(firstMsg.lastIndexOf(" ") + 1).replace(".", ""));
			System.out.println("'Serveur' : " + firstMsg);
			while (running) {
				String rcv = inFromServer.readLine();
				if (rcv.equals(ServeurTCP.RUOK)) {
					outToServer.println(IMOK);
					continue;
				}
				System.out.println("'Serveur' à 'Client " + ID + "' : " + rcv);
			}
		} catch (UnknownHostException ex) {
			System.err.println("Hôte inconnu : " + addressName);
		} catch (SocketException ex) {
			System.err.println("Connexion non établie ou interrompue avec : " + addressName);
		} catch (IOException e) {
			System.err.println("Echec de traitement d'un DatagramPacket.");
			e.printStackTrace();
		}
	}

	/**
	 * Lance 8 threads clients, 2 par 2, intercalés d'une seconde.
	 * 8 threads actifs d'après le serveur.
	 * Après 10 secondes, interrompt la moitié d'entre eux.
	 * 4 threads supprimés par le serveur.
	 * 4 threads actifs d'après le serveur.
	 */
	public static void main(String[] args) throws InterruptedException {
		final int NB_CLIENTS = 8;
		final int waitTimeMs = 1000;
		ClientTCP[] clients = new ClientTCP[NB_CLIENTS];

		for (int i = 0; i < NB_CLIENTS; i++) {
			clients[i] = new ClientTCP();
		}

		int wait = 0;
		for (ClientTCP c : clients) {
			c.start();
			wait++;
			if (wait % 2 == 0) {
				Thread.sleep(waitTimeMs);
			}
		}

		Thread.sleep(10000);
		
		for (int i = 0; i < clients.length / 2; i++) {
			System.out.println("Client désactivé");
			clients[i].running = false;
		}
	}

}
