package data;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Stocké côté serveur, regroupe les informations d'identité et de connexion
 * d'un hôte.
 */
public class HostData extends EntityData {

	public static final String NAME = "Hôte";
	
	private static int count = 1;

	/** Dépendant du compteur global {@link HostData#count} */
//	name;
	/** Addresse IP locale, non modifiable */
	public InetAddress IP;
	/** Doit être un port libre */
	public int port;

	/**
	 * Crée un HostData et vérifie que le port est libre en ouvrant
	 * temporairement une ServerSocket.
	 * @throws IOException
	 */
	public HostData() throws IOException {
		super("Partie_" + String.valueOf(count));
		count++;
		try (ServerSocket ss = new ServerSocket(0)) {
			IP = InetAddress.getLocalHost();
			port = ss.getLocalPort();
		} catch (IOException e) {
			throw new IOException("Aucun port libre trouvé.");
		}
	}

	public HostData(int port) throws IOException {
		super("Partie_" + String.valueOf(count));
		count++;
		try (ServerSocket ss = new ServerSocket(port)) {
			this.IP = ss.getInetAddress();
			this.port = ss.getLocalPort();
		} catch (IOException e) {
			throw new IOException("Aucun port libre trouvé.");
		}
	}

	public String getName() {
		return name;
	}

	public InetAddress getIP() {
		return IP;
	}

	public int getPort() {
		return port;
	}

}
