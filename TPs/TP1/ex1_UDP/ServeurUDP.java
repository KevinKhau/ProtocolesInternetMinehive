package TP1.ex1_UDP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * 1. Si ce serveur est utilisé par plusieurs clients C1 et C2, C1 envoyant un
 * long message, alors les lettres de C2 remplaceront la premiére partie de C1,
 * sans troncaturer le message. Le byte[] receiveData doit étre vidé.
 * 
 */
public class ServeurUDP {

	public static void main(String[] args) {
		int port = 9876;
		try (DatagramSocket serverSocket = new DatagramSocket(port)) {
			while (true) {
				byte[] receiveData = new byte[1024];
				byte[] sendData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				InetAddress UserAddress = receivePacket.getAddress();
				int UserPort = receivePacket.getPort();
				String sentence = new String(receivePacket.getData());
				System.out.println(UserAddress + ", " + UserPort + " : " + sentence);
				System.out.println("Envoie d'un message au client.");
				sendData = "MessageAuClient".getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, UserAddress, UserPort);
				serverSocket.send(sendPacket);
			}
		} catch (SocketException e) {
			System.err.println("Erreur de création ou d'accès à une socket, port " + port);
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Echec de traitement d'un DatagramPacket");
			e.printStackTrace();
		}
	}

}
