package network;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "player")
public class Player {
	public static final int INITIAL_POINTS = 0;
	
	@XmlAttribute (name = "username", required = true)
	String username;
	
	@XmlAttribute (name = "password", required = true)
	String password;
	
	@XmlElement (name = "points")
	int points;
	
	// Necessary for JAXB
	public Player() {
		super();
		this.points = INITIAL_POINTS;
	}

	public Player(String username, String password, int points) {
		super();
		this.username = username;
		this.password = password;
		this.points = points;
	}
	
	
	public void setPoints(int points) {
		this.points = points;
	}
	
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
}
