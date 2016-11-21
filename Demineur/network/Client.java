package network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import util.Message;

public class Client extends Entity {

	public static InetAddress COMMON_IP;

	public static void main(String[] args) {
		new Client();
		System.out.println("Fin client");
	}

	public Client() {
		super("Client");
		try {
			COMMON_IP = InetAddress.getLocalHost();
			/* TEST pour tester avec d'autres machines */
			// destIP = InetAddress.getByName("192.168.137.67");
		} catch (UnknownHostException e) {
			System.err.println("Serveur inconnu.");
			System.exit(1);
		}
		while (true) {
			// TODO proposer communication soit avec serveur, soit avec hôte
//			linkServer();
			 linkHost();
		}
		// reader.close();
	}

	public void linkServer() {
		System.out.println("Client - Serveur");
		new ServerCommunicator();
	}

	public void linkHost() {
		System.out.println("Client - Hôte");
		new HostCommunicator();
	}

	/** Client -> Server */
	private class ServerCommunicator extends Communicator {
		@Override
		protected void setAttributes() {
			receiverName = "Server";
			receiverIP = COMMON_IP;
			receiverPort = 5555;
			identificationWords = new LinkedList<>();
			identificationWords.add(Message.REGI);
			identificationWords.add(Message.LEAV);
			handler = new ClientServerHandler();
		}
	}

	/** Client -> Host */
	private class HostCommunicator extends Communicator {
		@Override
		protected void setAttributes() {
			receiverName = "Hôte";
			receiverIP = COMMON_IP;
			receiverPort = intKeyboardInput("Entrez le numéro de port de l'hôte.");
			identificationWords = new LinkedList<>();
			identificationWords.add(Message.JOIN);
			handler = new ClientHostHandler();
		}

		private int intKeyboardInput(String indication) {
			while (true) {
				if (indication != null) {
					System.out.println(indication);
				}
				try {
					return Integer.parseInt(reader.nextLine());
				} catch (NumberFormatException e) {
					System.err.println("Entier attendu !");
				} catch (NoSuchElementException e) {
					System.out.println("Au revoir !");
					System.exit(0);
				}
			}
		}
	}
}
