package TP1.ex1_UDP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientUDP {

	public static void main(String[] args) {
		int port = 9876;
		String addressName = "localhost";
		try (Scanner inFromUser = new Scanner(System.in); DatagramSocket clientSocket = new DatagramSocket();) {
			InetAddress IPAddress = InetAddress.getByName(addressName);
			byte[] sendData = new byte[1024];
			String sentence = inFromUser.nextLine();
			sendData = sentence.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
			clientSocket.send(sendPacket);
			sendPacket.getData();
		} catch (UnknownHostException e) {
			System.err.println("InetAddress.getByName(" + addressName +")");
		} catch (IOException e) {
			System.err.println("Echec d'envoi d'un DatagramPacket");
			e.printStackTrace();
		}
	}

}
