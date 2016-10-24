package network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

/**
 * Hôte lancé par le serveur //TODO Programmer lancement par serveur au lieu de manuel une fois dév achevée
 */
public class Host {
	
	InetAddress serverIP;
	int serverPort;
	
	String name;
	InetAddress IP;
	int port;
	
	private static void deny(String message) {
		System.err.println(message);
		System.err.println("Attendu : java Host serverIP serverPort hostName hostIP hostPort");
		System.exit(1);
	}
	
	public static void main(String[] args) {
		if (args.length < 5) {
			deny("Mauvais nombre d'arguments.");
		}
		
		InetAddress serverIP = null;
		try {
			serverIP = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			deny("Paramètre n°1 invalide, adresse IP du serveur non reconnue.");
		}
		
		int serverPort = 0;
		try {
			serverPort = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			deny("Paramètre n°2 invalide, numéro de port du serveur attendu.");
			return;
		}
		
		InetAddress hostIP = null;
		try {
			hostIP = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			deny("Paramètre n°4 invalide, adresse IP d'hôte non reconnue.");
		}
		
		int hostPort = 0;
		try {
			hostPort = Integer.parseInt(args[4]);
		} catch (NumberFormatException e) {
			deny("Paramètre n°5 invalide, numéro de port libre d'hôte attendu");
		}
		new Host(serverIP, serverPort, args[2], hostIP, hostPort);
	}

	public Host(InetAddress serverIP, int serverPort, String name, InetAddress IP, int port) {
		super();
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.name = name;
		this.IP = IP;
		this.port = port;
		
		try {
			ServerSocket ss = new ServerSocket(port);
			// TODO établir connexion
		} catch (IOException e) {
			deny("Paramètre n°5 invalide, port occupé");
		}
	}
	
}
