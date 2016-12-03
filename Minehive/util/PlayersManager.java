package util;

import java.io.File;
import java.nio.file.Paths;
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

import data.Player;

public final class PlayersManager {

	public final static File RANKING_FILE = Paths.get("res", "ranking.xml").toFile();

	// Pour tester rapidement si tout fonctionne
	public static void main(String[] args) {
		 Player test = getPlayer("Tomek");
		 System.out.println(test.username + " " + test.password + " " +
		 test.totalPoints);
		 test.totalPoints = 80;
		 writePlayer(test);
		 System.out.println("Success");
	}

	/**
	 * Obtenir la liste des joueurs en chargeant ranking.xml
	 * 
	 * @return Liste des joueurs
	 */
	public static HashMap<String, Player> getPlayersFromXML() {
		try {
			JAXBContext context = JAXBContext.newInstance(Ranking.class);

			Unmarshaller unmarshaller = context.createUnmarshaller();
			Ranking ranking = (Ranking) unmarshaller.unmarshal(RANKING_FILE);

			return toHashMap(ranking.players);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return new HashMap<>();
	}

	public static void savePlayersToXML(HashMap<String, Player> players) {
		try {
			Ranking ranking = new Ranking(toList(players));

			JAXBContext context = JAXBContext.newInstance(Ranking.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(ranking, RANKING_FILE);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	/**
	 * De préférence, obtenir un joueur directement d'une liste plutôt que
	 * passer par un chargement entier du fichier
	 */
	public static Player getPlayer(String username) {
		return getPlayersFromXML().get(username);
	}

	/*
	 * FUTURE Ne modifier qu'un seul noeud du fichier. Voir si on peut éviter de
	 * charger tous les noeuds
	 */
	/**
	 * Ajoute un joueur à la liste de ceux existants, puis écrit le tout dans le
	 * fichier XML.
	 * 
	 * @param player
	 *            Joueur à ajouter
	 */
	public static void writePlayer(Player player) {
		HashMap<String, Player> players = getPlayersFromXML();
		players.put(player.username, player);
		savePlayersToXML(players);
	}

	private static HashMap<String, Player> toHashMap(List<Player> list) {
		HashMap<String, Player> map = new HashMap<String, Player>();
		list.stream().filter(p -> p != null).forEach(p -> map.put(p.username, p));
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

		/**
		 * Constructeur vide pour JAXB
		 */
		@SuppressWarnings("unused")
		private Ranking() {
		}
		
		public Ranking(List<Player> players) {
			this.players = players;
		}

	}
}
