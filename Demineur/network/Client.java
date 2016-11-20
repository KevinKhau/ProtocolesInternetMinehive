package network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import util.Message;
import util.TFSocket;

public class Client extends Entity {

	/*
	 * Temps maximal d'attente des réponses du serveur avant de redonner la main
	 * à l'utilisateur
	 */
	public static final long MAX_WAIT_TIME = 5000;

	private enum State {
		OFFLINE, CONNECTED, IN
	};

	/* FUTURE Déplacer an attributs de Handle une fois contraintes Scanner résolues */
	InetAddress destIP;
	int destPort = 5555;

	Scanner reader = new Scanner(System.in);

	public static void main(String[] args) {
		new Client();
		System.out.println("Fin client");
	}

	public Client() {
		name = "Client";
		try {
			destIP = InetAddress.getLocalHost();
			/* TEST pour tester avec d'autres machines */
			// destIP = InetAddress.getByName("192.168.137.67");
		} catch (UnknownHostException e) {
			System.err.println("Serveur inconnu.");
			System.exit(1);
		}
		while (true) {
			// TODO proposer communication soit avec serveur, soit avec hôte
			System.out.println("Serveur");
			linkServer();
//			System.out.println("Hôte");
//			linkHost();
		}
		// reader.close();
	}

	public void linkServer() {
		destPort = 5555;
		new ServerLinker();
	}

	public void linkHost() {
		destPort = intKeyboardInput("Entrez le numéro de port de l'hôte.");
		new HostLinked();
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

	/**
	 * Effectue une suite d'instructions par rapport à un destinataire suite à des entrées utilisateur
	 */
	private abstract class Linker {
		String name;
		SocketHandler listener;
		List<String> identificationWords;

		State state = State.OFFLINE;
		
		volatile boolean waitingResponse = false;
		volatile boolean running = true;
		
		TFSocket socket;
		
		public Linker() {
			setAttributes();
			try {
				socket = new TFSocket(destIP, destPort);
				System.out.println("Client connecté à " + name + " : " + socket.remoteData() + ".");
				state = State.CONNECTED;
				new Thread(listener).start();
				while (running) {
					while (state == State.CONNECTED && running) {
						login();
					}
					while (state == State.IN && running) {
						communicate();
					}
				}
			} catch (IllegalMonitorStateException e) {
				System.err.println("Interruption du client pendant qu'il était en attente.");
			} catch (NoSuchElementException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				System.err.println(name + " inconnu : " + "IP=" + destIP + ", port=" + destPort + ".");
			} catch (SocketException e) {
				System.err.println(
						"Connexion non établie avec " + name + " : " + "IP=" + destIP + ", port=" + destPort + ".");
			} catch (IOException e) {
				System.err.println(
						"Communication impossible avec " + name + " : " + "IP=" + destIP + ", port=" + destPort + ".");
				e.printStackTrace();
			}
		}

		protected abstract void setAttributes();

		/**
		 * Attend une action de la part de l'utilisateur avant d'envoyer un
		 * message au serveur
		 * 
		 * @return Message interprété par l'action utilisateur
		 */
		public Message input(String indication) {
			return keyboardInput(indication); // FUTURE Changer en onClic()
												// après
			// implémentation interface graphique
		}

		/**
		 * Attend une entrée clavier de la part de l'utilisateur
		 * 
		 * @return Message valide
		 */
		private Message keyboardInput(String indication) {
			while (true) {
				if (indication != null) {
					System.out.println(indication);
				}
				try {
					return Message.validMessage(reader.nextLine());
				} catch (IllegalArgumentException e) {
					System.err.println(e.getMessage());
				} catch (NoSuchElementException e) {
					System.out.println("Au revoir !");
					System.exit(0);
				}
			}
		}

		/**
		 * Tente de s'identifier auprès du destinataire
		 */
		protected void login() {
			System.out.println("Tentative de connexion à " + name + ".");
			Message send = input("Envoyez un message de connexion à " + name + ". Format : TYPE[#ARG]...[#Contenu] : ");
			if (!identificationWords.contains(send.getType())) {
				System.err
						.println("Type de message de connexion invalide. Attendu : " + identificationWords.toString());
				login();
			}
			socket.send(send);
			leaveOrWait(send.getType());
		}

		protected void communicate() {
			Message m = input("Envoyez un message à " + name + ". Format : TYPE[#ARG]...[#Contenu] : ");
			socket.send(m);
			leaveOrWait(m.getType());
		}

		private void leaveOrWait(String type) {
			if (type.equals(Message.LEAV)) {
				System.out.println("Fin de la connexion avec " + socket.getRemoteSocketAddress());
				disconnect();
				return;
			} else {
				waitResponse();
			}
		}

		public void waitResponse() {
			synchronized (Linker.this) {
				waitingResponse = true;
				try {
					Linker.this.wait(MAX_WAIT_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		public synchronized void disconnect() {
			running = false;
			state = State.OFFLINE;
			socket.close();
		}

		/**
		 * Thread qui reste en écoute permanente d'une ServerSocket. Lance des
		 * instructions selon les messages reçus, et autorise le Client à répondre
		 * ou non selon {@linkplain SocketHandler#count}, géré par wakeClient()
		 */
		abstract class SocketHandler implements Runnable {
			int count = 0;
		
			@Override
			/**
			 * Écoute en boucle en traitant chaque message reçu.
			 */
			public void run() {
				while (running) {
					try {
						Message rcv = socket.receive();
						handleMessage(rcv);
					} catch (IOException e) {
						disconnect();
					}
				}
			}
		
			/** Vérification de messages en boucle */
			protected abstract void handleMessage(Message reception);
		
			/**
			 * Après avoir envoyé un {@linkplain Message} au {@linkplain Server}, le
			 * {@linkplain Client} est en attente d'une réponse. Il bloque donc
			 * jusqu'à que le {@linkplain ServerHandler} aie fini de traiter les
			 * messages reçus du {@linkplain Server}.
			 */
			protected synchronized final void wakeHandler() {
				if (waitingResponse) {
					if (count > 0) {
						count--;
					}
					if (count > 0) {
						return;
					}
					synchronized (Linker.this) {
						waitingResponse = false;
						Linker.this.notify();
					}
				}
			}
		
			/** Comportement par défaut si le message reçu est inconnu */
			protected void unknownMessage() {
				System.err.println("Réponse inconnue du serveur.");
				socket.send(Message.IDKC);
			}
		}

		class ServerHandler extends SocketHandler {
			@Override
			protected void handleMessage(Message reception) {
				System.out.println(reception);
				switch (reception.getType()) {
				/* REGI */
				case Message.IDOK:
					System.out.println("Connexion au serveur établie !");
					state = State.IN;
					wakeHandler();
					break;
				case Message.IDNO:
					System.out.println("Connexion échouée.");
				case Message.IDIG:
					wakeHandler();
					break;
		
				/* LS */
				case Message.LMNB:
				case Message.LANB:
				case Message.LUNB:
					count = reception.getArgAsInt(0);
					if (count == 0) {
						wakeHandler();
					}
					break;
				case Message.MATC:
				case Message.AVAI:
				case Message.USER:
					wakeHandler();
					break;
		
				/* NWMA */
				case Message.NWOK:
				case Message.FULL:
				case Message.NWNO:
					wakeHandler();
					break;
		
				case Message.KICK:
					System.out.println("Éjecté par le serveur.");
					disconnect();
					break;
		
				case Message.IDKS:
					System.out.println("Le serveur reste béant !");
					wakeHandler();
					break;
				default:
					unknownMessage();
				}
			}
		}

		class HostHandler extends SocketHandler {
			@Override
			protected void handleMessage(Message reception) {
				System.out.println(reception);
				switch (reception.getType()) {
				/* Connection and activity */
				case Message.DECO:
				case Message.AFKP:
				case Message.BACK:
					break;
		
				/* JOIN */
				case Message.JNNO:
					System.out.println("Connexion à l'hôte échouée.");
					wakeHandler();
					break;
				case Message.JNOK:
					System.out.println("Connexion à l'hôte établie !");
					state = State.IN;
				case Message.IGNB: // TODO vérifier qu'à cette étape le client est
									// encore bloqué
					count = reception.getArgAsInt(0);
					if (count == 0) {
						wakeHandler();
					}
					;
					break;
				case Message.BDIT:
				case Message.IGPL:
					wakeHandler();
					break;
				case Message.CONN:
					break;
		
				/* CLIC */
				case Message.LATE:
				case Message.OORG:
				case Message.SQRD:
					System.out.println(reception);
					wakeHandler();
					break;
		
				/* Fin de partie */
				case Message.ENDC:
				case Message.SCPC:
					break;
		
				case Message.IDKH:
					System.out.println("L'hôte reste béant : '" + reception + "'.");
					wakeHandler();
					break;
				default:
					unknownMessage();
				}
			}
		}
	}

	private class ServerLinker extends Linker {
		@Override
		protected void setAttributes() {
			name = "Server";
			identificationWords = new LinkedList<>();
			identificationWords.add(Message.REGI);
			identificationWords.add(Message.LEAV);
			listener = new ServerHandler();
		}
	}

	private class HostLinked extends Linker {
		protected void setAttributes() {
			name = "Hôte";
			identificationWords = new LinkedList<>();
			identificationWords.add(Message.JOIN);
			listener = new HostHandler();
		}
	}
}
