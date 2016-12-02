package data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "player")
public class Player extends EntityData {
	public static final String NAME = "Joueur";
	public static final int INITIAL_POINTS = 0;
	
	@XmlAttribute (name = "username", required = true)
	public String username;
	
	@XmlAttribute (name = "password", required = true)
	public String password;
	
	@XmlElement (name = "points")
	public volatile int totalPoints;
	
	// Necessary for JAXB
	public Player() {
		super(NAME);
	}
	
	public Player(String username, String password) {
		super(NAME);
		this.username = username;
		this.password = password;
	}

	@Override
	public String toString() {
		return "Player [username=" + username + ", password=" + password + ", totalPoints=" + totalPoints + "]";
	}

	public Player(String username, String password, int points) {
		super(NAME);
		this.username = username;
		this.password = password;
		this.totalPoints = points;
	}
	
	public synchronized void setTotalPoints(int points) {
		this.totalPoints = points;
	}
	
	public synchronized void incTotalPoints(int supp) {
		totalPoints += supp;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * @param password
	 * @return true is the password is correct
	 */
	public boolean checkPassword(String password) {
		return this.password.equals(password);
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Player) && ((Player) obj).username == this.username;
	}
}
