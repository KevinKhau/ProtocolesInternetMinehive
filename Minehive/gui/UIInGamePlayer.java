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
	boolean active = true;
	
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
		double clarity = 0.2;
		return new Color(sub(clarity), sub(clarity), sub(clarity), 1);
	}
	
	/**
	 * Getting a random double between 0 and 1
	 * 
	 * @param clarity
	 *            Multiplication factor. The bigger the lighter. 0 : no
	 *            influence, ordinary random. 1 : white.
	 */
	private static double sub(double clarity) {
		Random r = new Random();
		return clarity + ((1.0 - clarity) * r.nextDouble());
	}

	public void setUsername(String value) {
		username.set(value);
	}

	public String getUsername() {
		return username.get();
	}

	public IntegerProperty inGamePointsProperty() {
		return inGamePoints;
	}
	
	public synchronized void incInGamePoints(int value) {
		setInGamePoints(getInGamePoints() + value);
	}

	public synchronized void setInGamePoints(int value) {
		inGamePoints.set(value);
	}

	public Integer getInGamePoints() {
		return inGamePoints.get();
	}
	
	public IntegerProperty totalPointsProperty() {
		return totalPoints;
	}
	
	public synchronized void incTotalPoints(int value) {
		setTotalPoints(getTotalPoints() + value);
	}
	
	public synchronized void setTotalPoints(int value) {
		totalPoints.set(value);
	}

	public Integer getTotalPoints() {
		return totalPoints.get();
	}
	
	public IntegerProperty safeSquaresProperty() {
		return safeSquares;
	}
	
	public synchronized void incSafeSquares(int value) {
		setSafeSquares(getSafeSquares() + value);
	}
	
	public synchronized void setSafeSquares(int value) {
		safeSquares.set(value);
	}

	public Integer getSafeSquares() {
		return safeSquares.get();
	}
	
	public IntegerProperty foundMinesProperty() {
		return foundMines;
	}
	
	public synchronized void incFoundMines(int value) {
		setFoundMines(getFoundMines() + value);
	}
	
	public synchronized void setFoundMines(int value) {
		foundMines.set(value);
	}

	public Integer getFoundMines() {
		return foundMines.get();
	}

	public void setColor(Color value) {
		color = value;
	}

	public Color getColor() {
		if (active) {
			return color;
		} else {
			return Color.TRANSPARENT;
		}
	}
	
	public synchronized void setActive() {
		this.color = color.darker();
		this.active = true;
	}
	
	public synchronized void setInactive() {
		this.color = color.brighter();
		this.active = false;
	}
}
