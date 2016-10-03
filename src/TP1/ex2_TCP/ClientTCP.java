package TP1.ex2_TCP;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ClientTCP {

	public static void main(String[] args) {
		int port = 1027;
		String addressName = "localhost";
		try (Socket socket = new Socket(addressName, port)) {
			
		} catch (UnknownHostException ex) {
			System.err.println("Hôte inconnu : " + addressName);
		} catch (SocketException ex) {
			System.err.println("Connexion impossible : " + addressName);
		} catch (IOException e) {
			System.err.println("Echec de traitement d'un DatagramPacket");
			e.printStackTrace();
		}

	}

}
