package gui;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import network.Host;
import network.Server;
import util.Message;
import util.TFSocket;

public class ClientApp extends Application {
	enum ServerState {
		OFFLINE, CONNECTED, IN
	}
	enum HostState {
		OFFLINE, CONNECTED, IN
	}
	public volatile Stage primaryStage;

	InetAddress receiverIP;
	int receiverPort = 5555;

	ServerState serverState = ServerState.OFFLINE;
	HostState hostState = HostState.OFFLINE;

	public volatile boolean waitingResponse = false;
	public volatile boolean running = true;

	public TFSocket socket;
	ServerHandler serverHandler;
	public TFSocket hostSocket;
	HostHandler hostHandler;

	Login login;
	Loading loading;
	ServerView serverView;
	Game hostView;
	
	String username;
	String password;


	public static void main(String[] args) {
		launch(args);
	}

	private void initApp() {
		primaryStage.setTitle("Minehive");
		primaryStage.setMinWidth(700);
		primaryStage.setMinHeight(350);
		primaryStage.setWidth(800);
		primaryStage.setHeight(600);

		displayLogin();
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		initApp();
		primaryStage.show();
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		if (serverHandler != null) {
			serverHandler.disconnect();
		}
		if (hostHandler != null) {
			hostHandler.disconnect();
		}
		System.exit(0);
	}
	
	public void joinServer(String IP, int port, String username, String password) {
		this.username = username;
		this.password = password;
		this.login = new Login(this);
		this.serverView = new ServerView(this);
		loading = new Loading(this, serverView, login);
		primaryStage.setScene(new Scene(loading));
		primaryStage.show();
		if (connectServer(IP, port)) {
			serverHandler = new ServerHandler();
			new Thread(serverHandler).start();
			loginServer(username, password);
		} else  {
			loading.previous();
		}
	}
	
	public void displayLogin() {
		login = new Login(this);
		Scene scene = new Scene(login);
		primaryStage.setScene(scene);
	}
	
	public void delayedLogin() {
		SceneSetter ss = new SceneSetter(primaryStage, new Login(this));
		new Thread(ss).start();
	}
	
	public boolean connectServer(String IPName, int port) {
		String receiverName = Server.NAME;
		InetAddress IP = null;
		try {
			IP = InetAddress.getByName(IPName);
		} catch (UnknownHostException e) {
			Dialog.error("Connection Error", "Connection Error", "Could not find IP.");
			return false;
		}
		try {
			socket = new TFSocket(IP, port);
		} catch (BindException e) {
			loading.previous();
			Dialog.exception(e, "IP ou port de connexion de " + receiverName + " défini invalide.");
			return false;
		} catch (ConnectException e) {
			loading.previous();
			Dialog.exception(e, receiverName + " ne semble pas lancé.");
			return false;
		} catch (SocketException e) {
			loading.previous();
			Dialog.exception(e, "Connexion interrompue avec " + receiverName + ".");
			return false;
		} catch (IOException e) {
			loading.previous();
			Dialog.exception(e, "Communication impossible " + e.getMessage() + ".");
			return false;
		}
		
		serverState = ServerState.CONNECTED;
		return true;
	}

	public void loginServer(String username, String password) {
		socket.send(Message.REGI, new String[] { username, password });
	}
	
	public void listMatches() {
		socket.send(Message.LSMA);
	}
	
	public void listUsers() {
		socket.send(Message.LSUS);
	}
	
	public void listAvailable() {
		socket.send(Message.LSAV);
	}
	
	public void createMatch() {
		socket.send(Message.NWMA, new String[]{Server.ALL});
	}
	
	class ServerHandler implements Runnable {
		
		String receiverName = Server.NAME;
		
		@Override
		public void run() {
			while (running && serverState != ServerState.OFFLINE) {
				try {
					Message rcv = socket.receive();
					System.out.println(rcv);
					handleMessage(rcv);
				} catch (IOException | IllegalArgumentException e) {
					disconnect();
					System.err.println(e.getMessage() + " : " + "Connection with Server lost");
					Dialog.delayedException(e, "Connection with Server lost");
				}
			}
		}
		
		protected void handleMessage(Message reception) {
			switch (reception.getType()) {
			/* REGI */
			case Message.IDOK:
				System.out.println("Identification au serveur établie !");
				serverState = ServerState.IN;
				loading.next();
				serverView.activate();
				break;
			case Message.IDNO:
				System.out.println("Identification échouée.");
				loading.previous();
				Dialog.error("Server response", "Identification failed", reception.getContent());
				break;
			case Message.IDIG:
				Dialog.error("Server response", "Already in-game", reception.getContent());
				disconnect();
				joinHost(reception.getArg(0), reception.getArgAsInt(1));
				break;

			/* LS?? */
			case Message.LMNB:
				serverView.clearHosts();
				break;
			case Message.LANB:
				break;
			case Message.LUNB:
				serverView.clearUsers();
				break;
			case Message.MATC:
				serverView.addHost(reception);
				break;
			case Message.AVAI:
				serverView.addAvailable(reception);
				break;
			case Message.USER:
				serverView.addUser(reception);
				break;

			/* NWMA */
			case Message.NWOK:
				listMatches();
				break;
			case Message.FULL:
			case Message.NWNO:
				break;

			case Message.KICK:
				/* FIXME pas d'effet */
				Dialog.error("Kicked by server", "Kicked by server", reception.getContent());
				disconnect();
				break;

			case Message.IDKS:
				System.out.println(receiverName + " reste béant : '" + reception + "'.");
				break;
			default:
				Dialog.error("Unknown message", "Unknown message from " + receiverName, reception.getContent());
				socket.send(Message.IDKC);
			}
		}
		
		public synchronized void disconnect() {
			serverState = ServerState.OFFLINE;
			if (socket != null) {
				socket.close();
			}
			if (hostSocket == null || hostSocket.isClosed()) {
				delayedLogin();
			}
		}
		
	}
	
	public void joinHost(String IP, int port) {
		this.login = new Login(this);
		this.hostView = new Game(this);
		loading = new Loading(this, hostView, login);
		try {
			primaryStage.setScene(new Scene(loading));
			primaryStage.show();
		} catch (IllegalStateException e) {
			SceneSetter.delayedScene(primaryStage, new Loading(this, hostView, login));
		}
		if (connectHost(IP, port)) {
			this.hostHandler = new HostHandler();
			new Thread(hostHandler).start();
			loginHost(username, password);
		} else {
			System.err.println("Failed to connect to Host");
		}
	}
	
	public boolean connectHost(String IPName, int port) {
		String receiverName = Server.NAME;
		InetAddress IP = null;
		try {
			IP = InetAddress.getByName(IPName);
		} catch (UnknownHostException e) {
			Dialog.error("Connection Error", "Connection Error", "Could not find IP.");
			return false;
		}
		try {
			hostSocket = new TFSocket(IP, port);
		} catch (BindException e) {
			loading.previous();
			Dialog.exception(e, "IP ou port de connexion de " + receiverName + " défini invalide.");
			return false;
		} catch (ConnectException e) {
			loading.previous();
			try {
				Dialog.exception(e, receiverName + " ne semble pas lancé.");
			} catch (IllegalStateException e1) {
				Dialog.delayedException(e, receiverName + " ne semble pas lancé.");
			}
			return false;
		} catch (SocketException e) {
			loading.previous();
			Dialog.exception(e, "Connexion interrompue avec " + receiverName + ".");
			return false;
		} catch (IOException e) {
			loading.previous();
			Dialog.exception(e, "Communication impossible " + e.getMessage() + ".");
			return false;
		}
		hostState = HostState.CONNECTED;
		return true;
	}
	
	class HostHandler implements Runnable {

		String receiverName = Host.NAME;
		
		@Override
		public void run() {
			while (running && hostState != HostState.OFFLINE) {
				try {
					Message rcv = hostSocket.receive();
					System.out.println(rcv);
					handleMessage(rcv);
				} catch (IOException | IllegalArgumentException e) {
					disconnect();
					System.err.println(e.getMessage() + " : " + "Connection with Host lost");
					Dialog.delayedException(e, "Connection with Host lost");
				}
			}
		}
		
		protected void handleMessage(Message reception) {
			switch (reception.getType()) {
			/* Connection and activity */
			case Message.DECO:
			case Message.AFKP:
			case Message.BACK:
				break;

			/* JOIN */ // TODO all messages
			case Message.JNNO:
				System.out.println("Identification à l'hôte échouée.");
				loading.previous();
				break;
			case Message.JNOK:
				System.out.println("Identification à l'hôte établie !");
				hostState = HostState.IN;
				loading.next();
			case Message.IGNB:
				break;
			case Message.BDIT:
				String[] args = reception.getArgs();
				String[] contents = Arrays.copyOfRange(args, 1, args.length);
				hostView.board.revealLine(reception.getArgAsInt(0), contents);
				break;
			case Message.IGPL:
				break;
			case Message.CONN:
				break;

			/* CLIC */
			case Message.LATE:
			case Message.OORG:
				break;
			case Message.SQRD:
				hostView.revealSquare(reception.getArgAsInt(0), reception.getArgAsInt(1), reception.getArgAsInt(2), reception.getArgAsInt(3));
				break;

			/* Fin de partie */
			case Message.ENDC:
			case Message.SCPC:
				break;

			case Message.IDKH:
				System.out.println(receiverName + " reste béant : '" + reception + "'.");
				break;
			default:
				Dialog.error("Unknown message", "Unknown message from " + receiverName, reception.getContent());
				hostSocket.send(Message.IDKC);
			}
		}
		
		public synchronized void disconnect() {
			hostState = HostState.OFFLINE;
			if (hostSocket != null) {
				hostSocket.close();
			}
			delayedLogin();
		}
	}
	
	public void loginHost(String username, String password) {
		hostSocket.send(Message.JOIN, new String[]{ username, password });
	}
	
	public void click(int x, int y) {
		hostSocket.send(Message.CLIC, new String[]{ String.valueOf(x), String.valueOf(y) });
	}
	
}
