package gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class UIUser {

	StringProperty name;
	IntegerProperty points;
	BooleanProperty online = new SimpleBooleanProperty(false);
	
	public UIUser(String name, int points) {
		super();
		this.name = new SimpleStringProperty(name);
		this.points = new SimpleIntegerProperty(points);
	}

	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public Integer getPoints() {
		return points.get();
	}

	public void setPoints(int points) {
		this.points.set(points);
	}
	
	public Boolean isOnline() {
		return this.online.get();
	}
	
	public void setOnline(boolean online) {
		this.online.set(online);
	}
	
}
