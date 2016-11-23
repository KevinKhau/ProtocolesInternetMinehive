package gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public class Player {
	private int p;
	private Color color;
    private StringProperty name;
    private StringProperty points;
    
    public Player(String string, Color color) {
		setName(string);
		setPoints(0);
		setColor(color);
	}
    
	public void setName(String value) { nameProperty().set(value); }
    public String getFirstName() { return nameProperty().get(); }
    public StringProperty nameProperty() { 
        if (name == null) name = new SimpleStringProperty(this, "name");
        return name; 
    }

    public void addPoints(int value) {
    	setPoints(p + value);
    }
    
    public void setPoints(int value) {
    	p = value;
    	pointsProperty().set(Integer.toString(p)); 
    }
    public String getPoints() { return pointsProperty().get(); }
    public StringProperty pointsProperty() { 
        if (points == null) points = new SimpleStringProperty(this, "points");
        return points; 
    }
    
    public void setColor(Color value) { 
    	color = value;
    }
    
    public Color getColor() {
    	return color;
    }
}
