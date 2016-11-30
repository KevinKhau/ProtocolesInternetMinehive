package network;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import util.Message;
import util.TFSocket;

/**
 * <pre>Entité1 -> Entité2</pre>
 * Effectue une suite d'instructions par rapport à un destinataire récepteur
 * suite à des entrées utilisateur.
 */
public abstract class Communicator implements Runnable {
	InetAddress receiverIP;
	int receiverPort = 5555;

	/**
	 * Temps maximal d'attente des réponses du serveur avant de redonner la main
	 * à l'utilisateur
	 */
	public static final long MAX_WAIT_TIME = 5000;

	enum State {
		OFFLINE, CONNECTED, IN
	};

	Scanner reader = new Scanner(System.in);

	String receiverName;
	ReceiverHandler handler;
	List<String> identificationWords;

	State state = State.OFFLINE;

	public volatile boolean waitingResponse = false;
	public volatile boolean running = true;

	TFSocket communicatorSocket;

	public Communicator() {
		setAttributes();
		try {
			communicatorSocket = new TFSocket(receiverIP, receiverPort);
			System.out.println("Connecté à " + receiverName + " : " + communicatorSocket.remoteData() + ".");
			state = State.CONNECTED;
			new Thread(handler).start();
		} catch (NoSuchElementException e) {
			e.printStackTrace();
			disconnect();
		} catch (BindException e) {
			System.err.println("IP ou port de connexion de " + receiverName + " défini invalide.");
			disconnect();
		} catch (ConnectException e) {
			System.err.println(receiverName + " ne semble pas lancé.");
			disconnect();
		} catch (SocketException e) {
			System.err.println("Connexion interrompue avec " + receiverName + ".");
			disconnect();
		} catch (IOException e) {
			System.err.println("Communication impossible " + receiverName + ".");
			disconnect();
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void run() {
		while (running) {
			while (state == State.CONNECTED && running) {
				login();
			}
			while (state == State.IN && running) {
				communicate();
			}
		}
	}
	
	protected abstract void setAttributes();

	/**
	 * Attend une action de la part de l'utilisateur avant d'envoyer un message
	 * au serveur
	 * 
	 * @return Message interprété par l'action utilisateur
	 */
	public Message input(String indication) {
		return keyboardInput(indication);
	}

	/**
	 * Attend une entrée clavier de la part de l'utilisateur
	 * 
	 * @return Message valide
	 */
	private Message keyboardInput(String indication) {
		while (running) {
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
		return null;
	}

	/**
	 * Tente de s'identifier auprès du destinataire
	 */
	public void login() {
		System.out.println("Tentative d'identification à " + receiverName + ".");
		Message send = input(
				"Envoyez un message d'identification à " + receiverName + ". Format : TYPE[#ARG]...[#Contenu] : ");
		if (!identificationWords.contains(send.getType())) {
			System.err
					.println("Type de message d'identification invalide. Attendu : " + identificationWords.toString());
			login();
			return;
		}
		communicatorSocket.send(send);
		leaveOrWait(send.getType());
	}
	
	public void login(String username, String password) {
	}

	protected void communicate() {
		Message m = input("Envoyez un message à " + receiverName + ". Format : TYPE[#ARG]...[#Contenu] : ");
		communicatorSocket.send(m);
		leaveOrWait(m.getType());
	}

	private void leaveOrWait(String type) {
		if (type.equals(Message.LEAV)) {
			System.out.println("Fin de la connexion avec " + communicatorSocket.getRemoteSocketAddress());
			disconnect();
		} else {
			waitResponse();
		}
	}

	/**
	 * Attend passivement une réponse de la part de l'entité réceptrice.
	 * {@linkplain ReceiverHandler#wakeCommunicator()} se chargera de notify() le Thread.
	 */
	public void waitResponse() {
		synchronized (Communicator.this) {
			waitingResponse = true;
			try {
				Communicator.this.wait(MAX_WAIT_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (waitingResponse) {
				System.err.println("Pas de réponse de " + receiverName + ". Nouvelle tentative.");
			}
		}
	}

	public synchronized void disconnect() {
		running = false;
		state = State.OFFLINE;
		if (communicatorSocket != null) {
			communicatorSocket.close();
		}
	}
	
	/**
	 * <pre>Entité1 <- Entité2</pre>
	 * Thread qui reste en écoute permanente d'une ServerSocket. Lance des
	 * instructions selon les messages reçus, et autorise le Client à répondre
	 * ou non selon {@linkplain ReceiverHandler#count}, géré par wakeClient()
	 */
	abstract class ReceiverHandler implements Runnable {
		int count = 0;

		/**
		 * Écoute en boucle en traitant chaque message reçu.
		 */
		@Override
		public void run() {
			while (running) {
				try {
					Message rcv = communicatorSocket.receive();
					System.out.println(rcv);
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
		 * jusqu'à que le {@linkplain ClientServerHandler} aie fini de traiter
		 * les messages reçus du {@linkplain Server}.
		 */
		protected synchronized void wakeCommunicator() {
			if (waitingResponse) {
				if (count > 0) {
					count--;
				}
				if (count > 0) {
					return;
				}
				synchronized (Communicator.this) {
					waitingResponse = false;
					Communicator.this.notify();
				}
			}
		}

		/** Comportement par défaut si le message reçu est inconnu */
		protected void unknownMessage() {
			System.err.println("Réponse inconnue de " + receiverName);
			communicatorSocket.send(Message.IDKC);
		}
	}

	/** Host <- Server, instancié dans l'hôte */
	abstract class HostServerHandler extends ReceiverHandler {

		@Override
		protected abstract void handleMessage(Message reception);
		
		protected String getUsername(Message reception) {
			String username = reception.getArg(0);
			if (username == null) {
				System.err.println("Anomalie : Pas de nom d'utilisateur donné par le serveur pour " + Message.RQDT + ".");
				return null;
			} else {
				return username;
			}
		}
		
		@Override
		protected void unknownMessage() {
			communicatorSocket.send(Message.IDKH, null, "Commande inconnue ou pas encore implémentée");
		}

	}
}