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

	MyPrintWriter out;
	MyBufferedReader in;
	
	public static void main(String[] args) {
		new Client();
	}

	public Client() {
		try (Socket socket = new Socket(serverIP, serverPort);
				MyPrintWriter out = new MyPrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
				MyBufferedReader in = new MyBufferedReader(new InputStreamReader(socket.getInputStream()))) {
			this.out = out;
			this.in = in;
			System.out.println("Client lancé sur " + socket.getLocalSocketAddress());
			connect();
		} catch (UnknownHostException ex) {
			System.err.println("Hôte inconnu : " + serverIP);
		} catch (SocketException ex) {
			System.err.println("Connexion non établie ou interrompue avec : " + serverIP);
		} catch (IOException e) {
			System.err.println("Echec de traitement d'un DatagramPacket.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Tente de se connecter au serveur
	 * @return false tant que le serveur n'a pas envoyé IDOK
	 * @throws IOException 
	 */
	public boolean connect() throws IOException {
		List<Message> tests = new ArrayList<>();
		tests.add(new Message(Message.LSUS, null, null)); //sans se connecter
		tests.add(new Message(Message.REGI, new String[]{"Adil"}, null)); //nbArgs
		tests.add(new Message(Message.REGI, new String[]{"Adil", "Challenger"}, null)); //mauvais mdp 
		tests.add(new Message(Message.REGI, new String[]{"Valloris", "Cylly"}, null)); //inGame
		tests.add(new Message(Message.REGI, new String[]{"BinômeDe", "Helmi"}, null)); //non existant OK
		tests.add(new Message(Message.REGI, new String[]{"Christophe", "Lam"}, null)); //déjà co : OK
		tests.add(new Message(Message.REGI, new String[]{"Adil", "Champion"}, "Je deviendrai challenger !")); // OK
		tests.add(new Message(Message.REGI, new String[]{"Adil", "Champion"}, null)); // Classique OK
		int index = 0;
		Message msg;
		do {
			if (index == tests.size()) {
				break;
			}
			out.send(tests.get(index));
			index++;
			msg = in.receive();
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (!msg.getType().equals("IDOK"));
		System.out.println("Connexion au serveur réussie. Je pars et ne préviens pas le serveur.");
		while(true) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("client");
		}
//		return true;
	}

}
