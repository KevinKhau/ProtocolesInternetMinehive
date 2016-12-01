package network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import util.Message;

public class Client extends Entity {

	public static final String NAME = "Client";
	public static InetAddress commonIP;
	
	public Communicator communicator;

	public static void main(String[] args) {
		new Client();
		System.out.println("Fin client");
	}

	public Client() {
		super(NAME);
		setAddress();
		/* TEST pour tester avec d'autres machines */
		// destIP = setAddress("192.168.137.67");
		while (true) {
			// TODO proposer communication soit avec serveur, soit avec hôte
			linkServer();
			linkHost();
		}
	}
	
	public void setAddress() {
		try {
			commonIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			System.err.println("IP introuvable.");
			System.exit(1);
		}
	}
	
	public void setAddress(String address) {
		try {
			commonIP = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			System.err.println("IP introuvable.");
			System.exit(1);
		}
	}
	
	public void linkServer() {
		System.out.println("Client - Serveur");
		communicator = new ServerCommunicator();
		communicator.run();
	}

	public void linkHost() {
		System.out.println("Client - Hôte");
		communicator = new HostCommunicator();
		communicator.run();
	}

	/** Client -> Server */
	public class ServerCommunicator extends Communicator {
		@Override
		protected void setAttributes() {
			receiverName = Server.NAME;
			receiverIP = commonIP;
			receiverPort = 5555;
			identificationWords = new LinkedList<>();
			identificationWords.add(Message.REGI);
			identificationWords.add(Message.LEAV);
			handler = new ClientServerHandler();
		}

		/** Client <- Server */
		class ClientServerHandler extends ReceiverHandler {
			@Override
			protected void handleMessage(Message reception) {
				switch (reception.getType()) {
				/* REGI */
				case Message.IDOK:
					System.out.println("Identification au serveur établie !");
					state = State.IN;
					wakeCommunicator();
					break;
				case Message.IDNO:
					System.out.println("Identification échouée.");
				case Message.IDIG:
					wakeCommunicator();
					break;

				/* LS */
				case Message.LMNB:
				case Message.LANB:
				case Message.LUNB:
					count = reception.getArgAsInt(0);
					if (count == 0) {
						wakeCommunicator();
					}
					break;
				case Message.MATC:
				case Message.AVAI:
				case Message.USER:
					wakeCommunicator();
					break;

				/* NWMA */
				case Message.NWOK:
				case Message.FULL:
				case Message.NWNO:
					wakeCommunicator();
					break;

				case Message.KICK:
					System.out.println("Éjecté par le serveur.");
					disconnect();
					break;

				case Message.IDKS:
					System.out.println(receiverName + " reste béant : '" + reception + "'.");
					wakeCommunicator();
					break;
				default:
					unknownMessage();
				}
			}
		}
	}

	/** Client -> Host */
	private class HostCommunicator extends Communicator {
		@Override
		protected void setAttributes() {
			receiverName = "Hôte";
			receiverIP = commonIP;
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

		/** Client <- Host */
		class ClientHostHandler extends ReceiverHandler {
			@Override
			protected void handleMessage(Message reception) {
				switch (reception.getType()) {
				/* Connection and activity */
				case Message.DECO:
				case Message.AFKP:
				case Message.BACK:
					break;

				/* JOIN */
				case Message.JNNO:
					System.out.println("Identification à l'hôte échouée.");
					wakeCommunicator();
					break;
				case Message.JNOK:
					System.out.println("Identification à l'hôte établie !");
					state = State.IN;
				case Message.IGNB:
					count = reception.getArgAsInt(0);
					if (count == 0) {
						wakeCommunicator();
					}
					break;
				case Message.BDIT:
				case Message.IGPL:
					wakeCommunicator();
					break;
				case Message.CONN:
					break;

				/* CLIC */
				case Message.LATE:
				case Message.OORG:
				case Message.SQRD:
					wakeCommunicator();
					break;

				/* Fin de partie */
				case Message.ENDC:
				case Message.SCPC:
					break;

				case Message.IDKH:
					System.out.println(receiverName + " reste béant : '" + reception + "'.");
					wakeCommunicator();
					break;
				default:
					unknownMessage();
				}
			}
		}
	}
}
