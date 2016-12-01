package gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import network.Host;

public class UIDetailedHostData {

	StringProperty name;
	StringProperty IP;
	IntegerProperty port;
	IntegerProperty completion;
	Map<String, Integer> players = new HashMap<>();
	IntegerProperty slots = new SimpleIntegerProperty(Host.MAX_PLAYERS);
	
	public UIDetailedHostData(String name, String IPName, int port, int completion, Map<String, Integer> players) {
		this.name = new SimpleStringProperty(name);
		this.IP = new SimpleStringProperty(IPName);
		this.port = new SimpleIntegerProperty(port);
		this.completion = new SimpleIntegerProperty(completion);
		this.players = players;
	}

	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public String getIP() {
		return IP.get();
	}

	public void setIP(String IP) {
		this.IP.set(IP);
	}

	public Integer getPort() {
		return port.get();
	}

	public void setPort(int port) {
		this.port.set(port);
	}

	public Integer getCompletion() {
		return completion.get();
	}

	public void setCompletion(int completion) {
		this.completion.set(completion);
	}

	public Map<String, Integer> getPlayers() {
		return players;
	}

	public void setPlayers(Map<String, Integer> players) {
		this.players = players;
	}

	public Integer getSlots() {
		return slots.get();
	}

	public void setSlots(int slots) {
		this.slots.set(slots);
	}
	
	

}
