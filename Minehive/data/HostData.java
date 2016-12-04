package data;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import util.Params;

/**
 * Stocké côté serveur, regroupe les informations d'identité et de connexion
 * d'un hôte.
 */
public class HostData extends EntityData {

	public static final String NAME = "Hôte";

	private static int count = 1;

	public final String creator;

	/** Dépendant du compteur global {@link HostData#count} */
	// name;
	/** Addresse IP locale, non modifiable */
	public InetAddress IP;

	/** Doit être un port libre */
	public int port;
	
	public Path inLog, outLog, errorLog;

	/**
	 * Crée un HostData et vérifie que le port est libre en ouvrant
	 * temporairement une ServerSocket.
	 * @param creator Player who issued the creation of the match
	 * @throws IOException
	 */
	public HostData(String creator) throws IOException {
		super("Partie_" + String.valueOf(count));
		this.creator = creator;
		createLogFiles();
		count++;
		try (ServerSocket ss = new ServerSocket(0)) {
			IP = InetAddress.getLocalHost();
			port = ss.getLocalPort();
		} catch (IOException e) {
			throw new IOException("Aucun port libre trouvé.");
		}
	}
	
	private void createLogFiles() {
		this.inLog = createLogFile("In");
		this.outLog = createLogFile("Out");
		this.errorLog = createLogFile("Error");
	}
	
	private Path createLogFile(String sub) {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
		Date date = new Date();
		Path logPath = Paths.get(Params.LOG.toString(),
				this.name + sub + dateFormat.format(date) + ".log");
		try {
			Files.createFile(logPath);
		} catch (IOException e) {
			System.err.println("Failed to create log file '" + logPath.toString() + "', " + e.getMessage());
		}
		return logPath;
	}

	public String getName() {
		return name;
	}

	public String getIP() {
		return IP.getHostAddress();
	}

	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		return "HostData [creator=" + creator + ", IP=" + IP + ", port=" + port + ", name=" + name + "]";
	}

}
