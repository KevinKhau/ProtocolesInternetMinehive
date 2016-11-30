package gui;

import java.net.InetAddress;

import util.TFSocket;

public class ClientModel {
	enum State {
		OFFLINE, CONNECTED, IN
	};

	InetAddress receiverIP;
	int receiverPort = 5555;

	String receiverName;
	State state = State.OFFLINE;

	public volatile boolean waitingResponse = false;
	public volatile boolean running = true;

	TFSocket communicatorSocket;
}
