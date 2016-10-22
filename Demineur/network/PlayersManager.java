package network;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

public class PlayersManager {
	// Pour tester rapidement si tout fonctionne
	/*public static void main(String[] args) {
		Player test = getPlayer("Tomek");
		System.out.println(test.username + " " + test.password + " " + test.points);
		test.points = 80;
		writePlayer(test);
		System.out.println("Success");
	}*/
	
	public static HashMap<String, Player> getPlayersFromXML() {
		try {
			JAXBContext context = JAXBContext.newInstance(Ranking.class);
			
			Unmarshaller unmarshaller = context.createUnmarshaller();
			Ranking ranking = (Ranking) unmarshaller.unmarshal(new File("Demineur/res/ranking.xml"));
			
			return toHashMap(ranking.players);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static void savePlayersToXML(HashMap<String, Player> players) {
		try {
			JAXBContext context = JAXBContext.newInstance(Ranking.class);
			
			Unmarshaller unmarshaller = context.createUnmarshaller();
			Ranking ranking = (Ranking) unmarshaller.unmarshal(new File("Demineur/res/ranking.xml"));
			
			ranking.players = toList(players);
			
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(ranking, new File("Demineur/res/ranking.xml"));
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	public static Player getPlayer(String username) {
		return getPlayersFromXML().get(username);
	}
	
	public static void writePlayer(Player player) {
		HashMap<String, Player> players = getPlayersFromXML();
		players.put(player.username, player);
		savePlayersToXML(players);
	}
	
	private static HashMap<String, Player> toHashMap(List<Player> list) {
		HashMap<String, Player> map = new HashMap<String, Player>();
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) != null) {
				map.put(list.get(i).username, list.get(i));
			}
		}
		
		return map;
	}
	
	private static List<Player> toList(HashMap<String, Player> map) {		
		return new ArrayList<Player>(map.values());
	}
	
	@XmlRootElement(name = "ranking")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class Ranking {
		@XmlAttribute(name = "machine", required = true)
		String machine;
		
		@XmlElement(name = "player", type = Player.class)
		List<Player> players;
	}
}
