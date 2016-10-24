package network;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import util.Message;
import util.MyBufferedReader;
import util.MyPrintWriter;

// THINK Doit pouvoir être alerté d'un KICK du serveur. Thread à part rien que pour l'écoute ?
public class Client {
	final String serverIP = "localhost";
	final int serverPort = 5555;

	Socket socket;
	MyPrintWriter out;
	MyBufferedReader in;

	boolean running = true;

	Scanner reader = new Scanner(System.in);

	public static void main(String[] args) throws IOException {
		new Client();
	}

	public Client() {
		try (Socket socket = new Socket(serverIP, serverPort);
				MyPrintWriter out = new MyPrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
				MyBufferedReader in = new MyBufferedReader(new InputStreamReader(socket.getInputStream()))) {
			this.socket = socket;
			this.out = out;
			this.in = in;
			System.out.println("Client lancé sur " + socket.getLocalSocketAddress() + ".");

			while (!login());
			while (running) {
				communicate();
			}
		} catch (NoSuchElementException e) {
			e.printStackTrace();
		} catch (UnknownHostException ex) {
			System.err.println("Hôte inconnu : " + serverIP);
		} catch (SocketException ex) {
			System.err.println("Connexion non établie ou interrompue avec : " + serverIP);
		} catch (IOException e) {
			System.err.println("Communication impossible avec le serveur.");
			e.printStackTrace();
		}
	}

	/**
	 * Attend une action de la part de l'utilisateur avant d'envoyer un message
	 * au serveur
	 * 
	 * @return Message interprété par l'action utilisateur
	 */
	public Message input() {
		return keyboardInput(); // FUTURE Changer en onClic() après
								// implémentation interface graphique
	}

	/**
	 * Attend une entrée clavier de la part de l'utilisateur
	 * 
	 * @return Message valide
	 */
	private Message keyboardInput() {
		System.out.println("Envoyez un message au serveur. Format : TYPE[#ARG]...[#Contenu] : ");
		Message m;
		try {
			m = Message.validMessage(reader.nextLine());
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return keyboardInput();
		}
		return m;
	}

	/**
	 * Tente de s'identifier auprès du serveur
	 * 
	 * @return false tant que le serveur n'a pas envoyé IDOK
	 * @throws IOException
	 */
	private boolean login() throws IOException {
		System.out.println("Tentative de connexion au serveur.");
		Message send = input();
		if (!send.getType().equals(Message.REGI)) {
			System.err.println("Type de message de connexion invalide. Attendu : " + Message.REGI + ".");
			return login();
		}
		out.send(send);
		Message rcv = in.receive();
		switch (rcv.getType()) {
		case Message.IDOK:
			System.out.println("Connexion au serveur réussie.");
			return true;
		case Message.IDNO:
			System.out.println("Connexion échouée : " + rcv.getContent() + ".");
			return false;
		case Message.IDIG:
			System.out.println(rcv);
			return false;
		default:
			System.err.println("Réponse inattendue du serveur : " + rcv + ".");
			return false;
		}
	}

	public void communicate() throws IOException {
		Message m = input();
		out.send(m);
		if (m.getType().equals(Message.LEAV)) {
			System.out.println("Fin de la communication avec le serveur.");
			running = false;
			return;
		}
		in.receive();
	}

}
