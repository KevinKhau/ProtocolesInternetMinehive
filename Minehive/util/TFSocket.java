package util;

import static util.Message.decode;
import static util.Message.encode;
import static util.StringUtil.getSpaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class TFSocket extends Socket {

	private static final int CONNECTED_DELAY = 10000;
	private PrintWriter out;
	private BufferedReader in;

	volatile boolean running = true;
	
	public TFSocket(InetAddress address, int port) throws IOException {
		super(address, port);
		init();
	}

	public TFSocket() {
	}

	public void init() throws IOException {
		this.out = new PrintWriter(new OutputStreamWriter(getOutputStream()), true);
		this.in = new BufferedReader(new InputStreamReader(getInputStream()));
		setSoTimeout(CONNECTED_DELAY);
	}

	public void send(String type, String[] args, String content) {
		String before = "> Envoi : ";
		if (Params.rawExpedition || Params.codedExpedition) {
			System.out.print(before);
		}

		Message m = new Message(type, args, content);

		if (Params.rawExpedition) {
			System.out.println("'" + m + "' (brut).");
		}

		String codedMsg = encode(m);

		if (Params.codedExpedition) {
			String prefix = "";
			if (Params.rawExpedition) {
				prefix = getSpaces(before.length());
			}
			System.out.println(prefix + "'" + codedMsg + "' (codé).");
		}

		out.println(codedMsg);
	}
	
	public void send(String type, String[] args) {
		send(type, args, null);
	}

	public void send(String type) {
		send(type, null, null);
	}

	public void send(Message message) {
		send(message.getType(), message.getArgs(), message.getContent());
	}

	public Message receive() throws IOException {
		String raw = null;
		try {
			raw = in.readLine();
		} catch (SocketTimeoutException e) {
			System.err.println("Pas de réponse du client depuis " + CONNECTED_DELAY + ". Considéré déconnecté.");
			throw e;
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.out.println("Message reçu invalide !");
			throw e;
		} catch (BindException e) {
			System.err.println("IP ou port de connexion défini invalide : " + remoteData() + ".");
			throw e;
		} catch (ConnectException e) {
			System.err.println("L'entité de réception ne semble pas lancée.");
			throw e;
		} catch (SocketException e) {
			System.err.println("Connexion interrompue avec : " + remoteData() + ".");
			throw e;
		} catch (IOException e) {
			System.err.println("Communication impossible avec le client : " + remoteData() + ".");
			e.printStackTrace();
			throw e;
		}

		String before = "< Réception : ";
		if (Params.rawReception || Params.cleanReception) {
			System.out.print(before);
		}

		if (Params.rawReception) {
			System.out.println("'" + raw + "' (brut).");
		}

		if (raw == null) {
			throw new IllegalArgumentException("Message vide reçu.");
		}
		Message msg = decode(raw);

		if (Params.cleanReception) {
			String prefix = "";
			if (Params.rawReception) {
				prefix = getSpaces(before.length());
			}
			System.out.println(prefix + "'" + msg + "' (clean).");
		}
		
		if (msg.getType().equals(Message.IMOK)) {
			return receive();
		}
		
		if (msg.getType().equals(Message.RUOK)) {
			send(Message.IMOK);
			return receive();
		}

		return msg;
	}
	
	/** Envoie RUOK à répétition */
	public void ping() {
		new Thread(new Ping()).start();
	}

	public String remoteData() {
		return getRemoteSocketAddress().toString();
	}
	
	@Override
	public synchronized void close() {
		running = false;
		try {
			super.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class Ping implements Runnable {
		public static final int FREQUENCY = 5000;
		
		@Override
		public void run() {
			while (running) {
				send(Message.RUOK);
				try {
					Thread.sleep(FREQUENCY);
				} catch (InterruptedException e) {
					System.err.println("Interruption du Thread Ping pendant sleep()");
				}
			}
		}
		
	}
	
}
