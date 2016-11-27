package network;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;

import util.TFServerSocket;

public abstract class Listener implements Runnable {

	InetAddress IP;
	int port;
	
	public Listener(InetAddress IP, int port) {
		this.IP = IP;
		this.port = port;
	}

	@Override
	public void run() {
		try (TFServerSocket serverSocket = new TFServerSocket(port)) {
			System.out.println("Lancement serveur : IP=" + IP + ", port=" + port + ".");
			while (true) {
				listen(serverSocket);
			}
		} catch (BindException e) {
			System.err.println("Socket serveur déjà en cours d'utilisation : IP=" + IP + ", port=" + port + ".");
		} catch (IllegalArgumentException e) {
			System.err.println("Valeur de port invalide, doit être entre 0 et 65535.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Problème de traitement de la socket : port=" + port + ".");
			System.err.println("Port occupé ?");
			e.printStackTrace();
		}
	}

	protected abstract void listen(TFServerSocket serverSocket) throws IOException;
	
}
