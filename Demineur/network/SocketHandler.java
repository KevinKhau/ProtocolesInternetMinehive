package network;

import java.io.IOException;

import data.EntityData;
import util.Message;
import util.TFSocket;

/** Gère la connexion et les messages d'une autre entité expéditrice */
abstract class SocketHandler extends Thread {
	// TODO gérer inactivité client
	TFSocket socket;
	protected volatile boolean running = true;

	EntityData entityData;
	String entityName = "Entité";

	public SocketHandler(TFSocket socket, String name) {
		super();
		this.entityName = name;
		System.out.println(
				"Nouvelle connexion entrante " + entityName + " : " + socket.getRemoteSocketAddress());
		this.socket = socket;
		this.socket.ping();
	}

	@Override
	public void run() {
		try {
			entityData = link();
			while (running) {
				Message rcv = socket.receive();
				System.out.println(rcv);
				try {
					handleMessage(rcv);
				} catch (NullPointerException e) {
					System.err.println("Mauvais nombre d'arguments reçu.");
				}
			}
		} catch (IOException e) {
			disconnect();
		}
	}

	/** Instructions d'initialisation avec l'expéditeur */
	protected abstract EntityData link() throws IOException;

	/** Vérification de messages en boucle */
	protected abstract void handleMessage(Message reception);

	protected void unknownMessage() {
		System.err.println("Message inconnu de " + entityName);
		socket.send(Message.IDKC);
	}

	protected void disconnect() {
		running = false;
		removeEntityData();
		socket.close();
	}
	
	protected abstract void removeEntityData();
}
