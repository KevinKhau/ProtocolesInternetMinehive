package data;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "player")
public class Person extends EntityData {
	public static final String NAME = "Joueur";
	public static final int INITIAL_POINTS = 0;
	
	@XmlAttribute (name = "username", required = true)
	public String username;
	
	@XmlAttribute (name = "password", required = true)
	public String password;
	
	@XmlElement (name = "points")
	public volatile int totalPoints;
	
	/** Allowed to create a match. */
	@XmlTransient
	public boolean permission = true;
	
	// Necessary for JAXB
	public Person() {
		super(NAME);
	}
	
	public Person(String username, String password) {
		super(NAME);
		this.username = username;
		this.password = password;
	}

	@Override
	public String toString() {
		return "Player [username=" + username + ", password=" + password + ", totalPoints=" + totalPoints + "]";
	}

	public Person(String username, String password, int points) {
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
		return (obj instanceof Person) && ((Person) obj).username == this.username;
	}
}
