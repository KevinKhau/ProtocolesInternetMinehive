package network;

import java.io.IOException;

import data.EntityData;
import util.Message;
import util.TFSocket;

/** Gère la connexion et les messages d'une autre entité expéditrice */
abstract class SenderHandler extends Thread {
	// TODO gérer inactivité de l'expéditeur
	TFSocket socket;
	protected volatile boolean running = true;

	EntityData senderData;
	String senderName = "Entité";

	public SenderHandler(TFSocket socket, String name) {
		super();
		this.senderName = name;
		System.out.println("Nouvelle connexion entrante " + senderName + " : " + socket.getRemoteSocketAddress());
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
				Message rcv = null;
				try {
					rcv = socket.receive();
				} catch (IllegalArgumentException e) {
					System.err.println(e.getMessage() + ". Fin de la connexion.");
					return;
				}
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

	/**
	 * <p>
	 * Instructions d'initialisation avec l'expéditeur. On veut juste s'assurer
	 * de l'identité de l'expéditeur, mais il n'est pas encore ajouté à la liste
	 * en question.
	 * </p>
	 * <p>
	 * FUTURE Nombre de récursions limitée à au moins 1024, définissable avec
	 * -xss. Solution 1 : Limiter le nombre de tentatives de connexion.
	 * Solution 2 : mode itératif.
	 * </p>
	 */
	protected abstract void identification() throws IOException;

	/**
	 * Gère toutes les requêtes possibles de l'expéditeur après son
	 * identification.
	 */
	protected abstract void handleMessage(Message reception);

	protected abstract void unknownMessage();

	/**
	 * Autorise la Thread à s'arrêter, enlève le Player correspondant de Thread
	 * s'il existe. Ferme la socket et les streams associés.
	 */
	protected void disconnect() {
		running = false;
		removeEntityData();
		socket.close();
	}

	protected abstract void addEntityData();

	protected abstract void removeEntityData();
}
