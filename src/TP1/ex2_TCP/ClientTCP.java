package TP1.ex2_TCP;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ClientTCP extends Thread {
	int port = 1027;
	String addressName = "localhost";

	@Override
	public void run() {
		try (Socket socket = new Socket(addressName, port);
				DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
			outToServer.writeBytes("Bonjour\n");
			while (true) {
				String rcv = inFromServer.readLine();
				System.out.println("Reçu du serveur : " + rcv);
			}
		} catch (UnknownHostException ex) {
			System.err.println("Hôte inconnu : " + addressName);
		} catch (SocketException ex) {
			System.err.println("Connexion impossible : " + addressName);
		} catch (IOException e) {
			System.err.println("Echec de traitement d'un DatagramPacket");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		final int NB_CLIENTS = 8;
		int waitTimeMs = 1000;
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
	}

}
