package network;

public class Player {

	public static final int INITIAL_POINTS = 0;
	
	String username;
	String password;
	int points;

	public Player(String username, String password, int points) {
		super();
		this.username = username;
		this.password = password;
		this.points = points;
	}

}
