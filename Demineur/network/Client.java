package network;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import util.Message;
import util.MyBufferedReader;
import util.MyPrintWriter;

public class Client implements AutoCloseable {

	/*
	 * Temps maximal d'attente des réponses du serveur avant de redonner la main
	 * à l'utilisateur
	 */
	public static final long maxWaitTime = 10000;

	private enum State {
		OFFLINE, CONNECTED, PLAYING
	};

	InetAddress serverIP;
	final int serverPort = 5555;

	Socket socket;
	MyPrintWriter out;
	MyBufferedReader in;

	boolean waitingResponse = false;
	volatile boolean running = true;

	Scanner reader = new Scanner(System.in);
	public State state = State.OFFLINE;

	public static void main(String[] args) {
		try (Client c = new Client()) {
			System.out.println("Fin client");
		}
	}

	public Client() {
		try {
			serverIP = InetAddress.getLocalHost();
//			serverIP = InetAddress.getByName("192.168.137.67"); // TEST Pour tester avoir d'autres machines
		} catch (UnknownHostException e) {
			System.err.println("Serveur inconnu.");
			System.exit(1);
		}
		while (true) {
			// TODO proposer communication soit avec serveur, soit avec hôte
			linkServer();
			System.out.println("Host time !");
			linkHost();
		}
	}

	public void linkServer() {
		try {
			this.socket = new Socket(serverIP, serverPort); // TEST
			this.out = new MyPrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			this.in = new MyBufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("Client connecté au serveur " + socket.getLocalSocketAddress() + ", port=" + serverPort);
			new Thread(new ServerListener()).start();
			while (running) {
				while (state == State.OFFLINE && running) {
					serverLogin();
				}
				while (state == State.CONNECTED && running) {
					communicate();
				}
			}
		} catch (IllegalMonitorStateException e) {
			System.err.println(e.getMessage() + ": Arrêt du client pendant qu'il était en attente.");
		} catch (NoSuchElementException e) {
			e.printStackTrace();
		} catch (UnknownHostException ex) {
			System.err.println("Serveur inconnu : " + serverIP);
		} catch (SocketException ex) {
			System.err.println("Connexion non établie ou interrompue avec : " + serverIP + ", port=" + serverPort);
		} catch (IOException e) {
			System.err.println("Communication impossible avec le serveur.");
			e.printStackTrace();
		}
	}

	public void linkHost() {
		final InetAddress hostIP = serverIP;
		int hostPort = intKeyboardInput("Entrez le numéro de port de l'hôte.");
		try {
			this.socket = new Socket(hostIP, hostPort);
			this.out = new MyPrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			this.in = new MyBufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("Client connecté à l'hôte " + socket.getLocalSocketAddress() + ", port=" + hostPort);
			new Thread(new HostListener()).start();
			while (running) {
				while (state == State.OFFLINE && running) {
					hostLogin();
				}
				while (state == State.CONNECTED && running) {
					communicate();
				}
			}
		} catch (IllegalMonitorStateException e) {
			System.err.println(e.getMessage() + ": Arrêt du client pendant qu'il était en attente.");
		} catch (NoSuchElementException e) {
			e.printStackTrace();
		} catch (UnknownHostException ex) {
			System.err.println("Hôte inconnu : " + hostIP);
		} catch (SocketException ex) {
			System.err.println("Connexion non établie ou interrompue avec : " + hostIP + ", port=" + hostPort);
		} catch (IOException e) {
			System.err.println("Communication impossible avec l'hôte.");
			e.printStackTrace();
		}
	}

	/**
	 * Attend une action de la part de l'utilisateur avant d'envoyer un message
	 * au serveur
	 * 
	 * @return Message interprété par l'action utilisateur
	 */
	public Message input(String indication) {
		return keyboardInput(indication); // FUTURE Changer en onClic() après
		// implémentation interface graphique
	}

	/**
	 * Attend une entrée clavier de la part de l'utilisateur
	 * 
	 * @return Message valide
	 */
	private Message keyboardInput(String indication) {
		if (indication != null) {
			System.out.println(indication);
		}
		Message m;
		try {
			m = Message.validMessage(reader.nextLine());
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return keyboardInput(indication);
		}
		return m;
	}

	private int intKeyboardInput(String indication) {
		if (indication != null) {
			System.out.println(indication);
		}
		while (!reader.hasNextInt()) {
			System.out.print("Tapez un entier : ");
			try {
				reader.next();
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
		return reader.nextInt();
	}

	/**
	 * Tente de s'identifier auprès du serveur
	 * 
	 * @return false tant que le serveur n'a pas envoyé IDOK
	 * @throws IOException
	 */
	private void serverLogin() {
		System.out.println("Tentative de connexion au serveur.");
		Message send = input("Envoyez un message au serveur. Format : TYPE[#ARG]...[#Contenu] : ");
		List<String> allowed = new LinkedList<>();
		allowed.add(Message.REGI);
		allowed.add(Message.LEAV);
		if (!allowed.contains(send.getType())) {
			System.err.println("Type de message de connexion invalide. Attendu : " + allowed.toString());
			serverLogin();
		}
		out.send(send);
		leaveOrWait(send.getType());
	}

	/**
	 * Tente de s'identifier auprès de l'hôte, de la même manière qu'avec le serveur
	 * 
	 * @return false tant que le serveur n'a pas envoyé IDOK
	 * @throws IOException
	 */
	private void hostLogin() {
		System.out.println("Tentative de connexion à l'hôte.");
		Message send = input("Envoyez un message à l'hôte. Format : TYPE[#ARG]...[#Contenu] : ");
		List<String> allowed = new LinkedList<>();
		allowed.add(Message.JOIN);
		if (!allowed.contains(send.getType())) {
			System.err.println("Type de message de connexion invalide. Attendu : " + allowed.toString());
			hostLogin();
		}
		out.send(send);
		waitResponse();
	}

	public void communicate() {
		Message m = input("Envoyez un message. Format : TYPE[#ARG]...[#Contenu] : ");
		out.send(m);
		leaveOrWait(m.getType());
	}

	private void leaveOrWait(String type) {
		if (type.equals(Message.LEAV)) {
			System.out.println("Fin de la connexion avec " + socket.getRemoteSocketAddress());
			close();
			return;
		} else {
			waitResponse();
		}
	}

	@Override
	public void close() {
		running = false;
		try {
			state = State.OFFLINE;
			out.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void waitResponse() {
		synchronized (this) {
			waitingResponse = true;
			try {
				wait(maxWaitTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	class ServerListener implements Runnable {

		int count = 0;

		@Override
		public void run() {
			while (running) {
				try {
					Message rcv = in.receive();
					if (rcv.getType().equals(Message.RUOK)) {
						out.send(Message.IMOK);
						continue;
					}
					System.out.println(rcv);
					switch (rcv.getType()) {
						/* REGI */
					case Message.IDOK:
						System.out.println("Connexion au serveur établie !");
						state = State.CONNECTED;
						wakeClient();
						break;
					case Message.IDNO:
						System.out.println("Connexion échouée.");
					case Message.IDIG:
						wakeClient();
						break;

						/* LS */
					case Message.LMNB:
					case Message.LANB:
					case Message.LUNB:
						count += rcv.getArgAsInt(0);
						if (count == 0) {
							wakeClient();
						}
						break;
					case Message.MATC:
					case Message.AVAI:
					case Message.USER:
						wakeClient();
						break;

						/* NWMA */
					case Message.NWOK:
					case Message.FULL:
					case Message.NWNO:
						wakeClient();
						break;

					case Message.KICK:
						System.out.println("Éjecté par le serveur.");
						close();
						break;

					case Message.IDKS:
						System.out.println("Le serveur reste béant !");
						wakeClient();
						break;
					default:
						System.err.println("Réponse inconnue du serveur.");
					}
				} catch (IllegalArgumentException ex) {
					System.err.println(ex.getMessage());
					close();
				} catch (SocketException ex) {
					System.err
					.println("Connexion non établie ou interrompue avec : " + socket.getRemoteSocketAddress());
					close();
				} catch (IOException e) {
					System.err.println("Communication impossible avec le serveur " + socket.getRemoteSocketAddress());
					e.printStackTrace();
					close();
				}
			}
		}

		/**
		 * Après avoir envoyé un {@linkplain Message} au {@linkplain Server}, le
		 * {@linkplain Client} est en attente d'une réponse. Il bloque donc
		 * jusqu'à que le {@linkplain ServerListener} aie fini de traiter les
		 * messages reçus du {@linkplain Server}.
		 */
		private synchronized void wakeClient() {
			if (waitingResponse) {
				count--;
				if (count > 0) {
					return;
				}
				synchronized (Client.this) {
					waitingResponse = false;
					Client.this.notify();
				}
			}
		}

	}

	class HostListener implements Runnable {

		int count = 0;

		@Override
		public void run() {
			// TODO Finir, tester Client.HostListener
			while(running) {
				try {
					Message rcv = in.receive();
					if (rcv.getType().equals(Message.RUOK)) {
						out.send(Message.IMOK);
						continue;
					}
					System.out.println(rcv);
					switch (rcv.getType()) {
					/* Connection and activity */
					case Message.DECO:
					case Message.AFKP:
					case Message.BACK:
						break;

						/* JOIN */
					case Message.JNNO:
						System.out.println("Connexion à l'hôte échouée.");
						wakeClient();
						break;
					case Message.JNOK:
						System.out.println("Connexion à l'hôte établie !");
						state = State.CONNECTED;
					case Message.IGNB: // TODO vérifier qu'à cette étape le client est encore bloqué
						count += rcv.getArgAsInt(0);
						if (count == 0) {
							wakeClient();
						};
						break;
					case Message.BDIT:
					case Message.IGPL:
						wakeClient();
						break;
					case Message.CONN:
						break;

						/* CLIC */
					case Message.LATE:
					case Message.OORG:
					case Message.SQRD:
						System.out.println(rcv);
						wakeClient();
						break;

						/* Fin de partie */
					case Message.ENDC:
					case Message.SCPC:
						break;

					case Message.IDKH:
						System.out.println("L'hôte reste béant : '" + rcv + "'.");
						wakeClient();
						break;
					default:
						System.err.println("Réponse inconnue de l'hôte : '" + rcv + "'.");
					}
				} catch (SocketException ex) {
					System.err.println("Connexion non établie ou interrompue avec : " + socket.getRemoteSocketAddress());
					close();
				} catch (IOException e) {
					System.err.println("Communication impossible avec le serveur " + socket.getRemoteSocketAddress());
					e.printStackTrace();
					close();
				}
			}

		}

		/**
		 * Équivalent de ServerListener#wakeClient()
		 */
		private synchronized void wakeClient() {
			if (waitingResponse) {
				count--;
				if (count > 0) {
					return;
				}
				synchronized (Client.this) {
					waitingResponse = false;
					Client.this.notify();
				}
			}
		}
	}
}
