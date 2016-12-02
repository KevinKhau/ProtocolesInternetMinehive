package gui;

import java.util.Random;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public class UIInGamePlayer {
	private Color color;
	private StringProperty username;
	private IntegerProperty inGamePoints;
	private IntegerProperty totalPoints;
	private IntegerProperty safeSquares;
	private IntegerProperty foundMines;
	boolean active = true; // TODO compléter interactions CONN/AFKP
	
	public UIInGamePlayer(String username, int inGamePoints, int totalPoints, int safeSquares, int foundMines) {
		this.username = new SimpleStringProperty(username);
		this.inGamePoints = new SimpleIntegerProperty(inGamePoints);
		this.totalPoints = new SimpleIntegerProperty(totalPoints);
		this.safeSquares = new SimpleIntegerProperty(safeSquares);
		this.foundMines = new SimpleIntegerProperty(foundMines);
		setColor(randomColor());
	}

	private static Color randomColor() {
		Random r = new Random();
		return new Color(r.nextDouble(), r.nextDouble(), r.nextDouble(), 1);
	}

	public void setUsername(String value) {
		username.set(value);
	}

	public String getUsername() {
		return username.get();
	}

	public void incInGamePoints(int value) {
		setInGamePoints(getInGamePoints() + value);
	}

	public void setInGamePoints(int value) {
		inGamePoints.set(value);
	}

	public Integer getInGamePoints() {
		return inGamePoints.get();
	}
	
	public void incTotalPoints(int value) {
		setTotalPoints(getTotalPoints() + value);
	}
	
	public void setTotalPoints(int value) {
		totalPoints.set(value);
	}

	public Integer getTotalPoints() {
		return totalPoints.get();
	}
	
	public void incSafeSquares(int value) {
		setSafeSquares(getSafeSquares() + value);
	}
	
	public void setSafeSquares(int value) {
		safeSquares.set(value);
	}

	public Integer getSafeSquares() {
		return totalPoints.get();
	}
	
	public void incFoundMines(int value) {
		setFoundMines(getFoundMines() + value);
	}
	
	public void setFoundMines(int value) {
		foundMines.set(value);
	}

	public Integer getFoundMines() {
		return foundMines.get();
	}

	public void setColor(Color value) {
		color = value;
	}

	public Color getColor() {
		return color;
	}
	
	public synchronized void setActive() {
		this.active = true;
		this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.2);
	}
	
	public synchronized void setInactive() {
		this.active = false;
		this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 1);
	}
}
