package network;

/**
 * Stocké côté serveur, regroupe les informations d'identité et de connexion
 * d'un hôte
 */
public class HostData {
	
	String name;
	String IP;
	int port;

	public HostData(String name, String IP, int port) {
		super();
		this.name = name;
		this.IP = IP;
		this.port = port;
	}
	
}
