package network;

import java.io.IOException;
import java.net.InetAddress;

import gui.Dialog;
import javafx.scene.Parent;
import util.Message;
import util.TFSocket;

public class ClientController<V extends Parent, ClientModel> {
	
	public TFSocket socket;
	protected V view;
	protected ClientModel model;

	public ClientController() {
	}

	public boolean connect(InetAddress IP, int port) {
		try {
			socket = new TFSocket(IP, 5555);
		} catch (IOException e) {
			e.printStackTrace();
			Dialog.error("Connection Error", "Connection Error", "Server is not responding.");
			return false;
		}
		return true;
	}
	
	public void loginServer(String username, String password) {
		socket.send(Message.REGI, new String[]{username, password});
	}
	
}
