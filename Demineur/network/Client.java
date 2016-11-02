package network;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

// THINK Doit pouvoir être alerté d'un KICK du serveur. Thread à part rien que pour l'écoute ?
public class Client implements AutoCloseable {

	/* Temps maximal d'attente des réponses du serveur avant de redonner la main à l'utilisateur */
	public static final long maxWaitTime = 10000;

	private enum State {
		OFFLINE, CONNECTED, PLAYING
	};

	final String serverIP = "localhost";
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
			System.out.println("end");
		}
	}

	public Client() {
		try {
			this.socket = new Socket(serverIP, serverPort);
			this.out = new MyPrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
			this.in = new MyBufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("Client connecté à " + socket.getLocalSocketAddress() + ".");
			new Thread(new ServerListener()).start();
			while (running) {
				while (state == State.OFFLINE && running) {
					login();
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
	private void login() {
		System.out.println("Tentative de connexion au serveur.");
		Message send = input();
		List<String> allowed = new LinkedList<>();
		allowed.add(Message.REGI);
		allowed.add(Message.LEAV);
		if (!allowed.contains(send.getType())) {
			System.err.println("Type de message de connexion invalide. Attendu : " + allowed.toString());
			login();
		}
		out.send(send);
		if (send.getType().equals(Message.LEAV)) {
			System.out.println("Fin de la connexion avec le serveur " + socket.getRemoteSocketAddress());
			close();
			return;
		} else {
			waitResponse();
		}
	}

	public void communicate() {
		Message m = input();
		out.send(m);
		if (m.getType().equals(Message.LEAV)) {
			System.out.println("Fin de la connexion avec le serveur " + socket.getRemoteSocketAddress());
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
					switch (rcv.getType()) {
					case Message.RUOK:
						out.send(Message.IMOK);
						break;
					
					/* REGI */
					case Message.IDOK:
						System.out.println("Connexion au serveur établie. " + rcv.getContent());
						state = State.CONNECTED;
						wakeClient();
						break;
					case Message.IDNO:
						System.out.println("Connexion échouée : " + rcv.getContent() + ".");
						wakeClient();
						break;
					case Message.IDIG:
						System.out.println(rcv);
						wakeClient();
						break;
						
					/* LS */
					case Message.LMNB:
						count = rcv.getArgAsInt(0);
						System.out.println(rcv);
						if (count == 0) {
							wakeClient();
						}
						break;
					case Message.MATC:
						System.out.println(rcv);
						wakeClient();
						break;
					case Message.LANB:
						count = rcv.getArgAsInt(0);
						System.out.println(rcv);
						if (count == 0) {
							wakeClient();
						}
						break;
					case Message.AVAI:
						System.out.println(rcv);
						wakeClient();
						break;
					case Message.LUNB:
						count = rcv.getArgAsInt(0);
						System.out.println(rcv);
						if (count == 0) {
							wakeClient();
						}
						break;
					case Message.USER:
						System.out.println(rcv);
						wakeClient();
						break;
						
					/* NWMA */
					case Message.NWOK:
						System.out.println(rcv);
						wakeClient();
						break;
					case Message.FULL:
						System.out.println(rcv);
						wakeClient();
						break;
					case Message.NWNO:
						System.out.println(rcv);
						wakeClient();
						break;

					case Message.KICK:
						System.out.println("Éjecté par le serveur : " + rcv + ".");
						close();
						break;

					case Message.IDKS:
						System.out.println("Le serveur reste béant : '" + rcv + "'.");
						wakeClient();
						break;
					default:
						System.err.println("Réponse inconnue du serveur : '" + rcv + "'.");
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
		 * Après avoir envoyé un {@linkplain Message} au {@linkplain Server}, le {@linkplain Client} est
		 * en attente d'une réponse. Pendant ce temps, il attend donc jusqu'à
		 * que le {@linkplain ServerListener} aie fini de traiter les messages
		 * reçus du {@linkplain Server}.
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
