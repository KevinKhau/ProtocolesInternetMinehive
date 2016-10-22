package network;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import util.Message;
import util.MyBufferedReader;
import util.MyPrintWriter;

public class Client {
	String serverIP = "localhost";
	final int serverPort = 5555;
	Socket socket;

	MyPrintWriter out;
	MyBufferedReader in;

	Message message;

	public static void main(String[] args) throws IOException {
		List<Message> tests = new ArrayList<>();
		tests.add(new Message(Message.LSUS, null, null)); //sans se connecter
		tests.add(new Message(Message.REGI, new String[]{"Adil"}, null)); //nbArgs
		tests.add(new Message(Message.REGI, new String[]{"Adil", "Challenger"}, null)); //mauvais mdp 
		tests.add(new Message(Message.REGI, new String[]{"Valloris", "Cylly"}, null)); //inGame
		tests.add(new Message(Message.REGI, new String[]{"BinômeDe", "Helmi"}, null)); //non existant OK
		tests.add(new Message(Message.REGI, new String[]{"Christophe", "Lam"}, null)); //classique OK
		tests.add(new Message(Message.REGI, new String[]{"Adil", "Champion"}, "Je deviendrai challenger !")); // OK
		tests.add(new Message(Message.REGI, new String[]{"Adil", "Champion"}, null)); // Classique OK
		for (Message msg : tests) {
			new Thread() {
				public void run() {new Client(msg);};
			}.start();
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public Client(Message send) {
		this.message = send; //TODO remove l'attribut message, ici seulement pour tests
		try (Socket socket = new Socket(serverIP, serverPort);
				MyPrintWriter out = new MyPrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
				MyBufferedReader in = new MyBufferedReader(new InputStreamReader(socket.getInputStream()))) {
			this.socket = socket;
			this.out = out;
			this.in = in;
			System.out.println("Client lancé sur " + socket.getLocalSocketAddress());

			launch();
			
		} catch (UnknownHostException ex) {
			System.err.println("Hôte inconnu : " + serverIP);
		} catch (SocketException ex) {
			System.err.println("Connexion non établie ou interrompue avec : " + serverIP);
		} catch (IOException e) {
			System.err.println("Echec de traitement d'un DatagramPacket.");
			e.printStackTrace();
		}
	}

	public void launch() {
		try {
			login(message);
			while (true) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Client actif : " + socket.getLocalSocketAddress());
			}
		} catch (IOException e1) {
			System.err.println("Communication impossible avec le serveur");
		}
	};

	/**
	 * Tente de s'identifier auprès du serveur
	 * 
	 * @return false tant que le serveur n'a pas envoyé IDOK
	 * @throws IOException
	 */
	public boolean login(Message send) throws IOException {
		if (!send.getType().equals(Message.REGI)) {
			System.err.println("Tentative de connexion avec un message autre que " + Message.REGI + ".");
		}
		Message rcv;
		out.send(send);
		rcv = in.receive();
		switch (rcv.getType()) {
		case Message.IDOK:
			System.out.println("Connexion au serveur réussie.");
			return true;
		case Message.IDNO:
			System.out.println("Connexion échouée : " + rcv.getContent());
			return false;
		case Message.IDIG:
			System.out.println(rcv);
			return false;
		default:
			System.err.println("Réponse anormale du serveur.");
			return false;
		}
	}

}
