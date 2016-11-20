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
			identification();
			if (running) {
				addEntityData();
			}
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

	/** Instructions d'initialisation avec l'expéditeur. On veut juste s'assurer de l'identité de l'expéditeur, mais il n'est pas encore ajouté à la liste en question. */
	protected abstract void identification() throws IOException;

	/** Vérification de messages en boucle */
	protected abstract void handleMessage(Message reception);

	protected abstract void unknownMessage();

	/**
	 * Autorise la Thread à s'arrêter, enlève le Player correspondant de
	 * Thread s'il existe. Ferme la socket et les streams associés.
	 */
	protected void disconnect() {
		running = false;
		removeEntityData();
		socket.close();
	}
	
	protected abstract void addEntityData();
	protected abstract void removeEntityData();
}
