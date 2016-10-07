package TP1.ex2_TCP;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ClientTCP extends Thread {
	int ID;
	final int serverPort = 1027;
	String addressName = "localhost";


	@Override
	public synchronized void run() {
		try (Socket socket = new Socket(addressName, serverPort);
				DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
			outToServer.writeBytes("Bonjour\n");
			String firstMsg = inFromServer.readLine();
			ID = Integer.parseInt(firstMsg.substring(firstMsg.lastIndexOf(" ")+1).replace(".", ""));
			System.out.println(firstMsg);
			while (true) {
				String rcv = inFromServer.readLine();
				System.out.println("'Serveur' à 'Client " + ID + "' : " + rcv);
			}
		} catch (UnknownHostException ex) {
			System.err.println("Hôte inconnu : " + addressName);
		} catch (SocketException ex) {
			System.err.println("Connexion non établie ou interrompue : " + addressName);
		} catch (IOException e) {
			System.err.println("Echec de traitement d'un DatagramPacket");
			e.printStackTrace();
		}
	}

	/**
	 * Lance 8 clients, 2 par 2, intercalés d'une seconde
	 */
	public static void main(String[] args) {
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
				try {
					Thread.sleep(waitTimeMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		//TODO Fermer certaines sockets pour tester la diminution du nombre de clients, socket.close()
	}

}
