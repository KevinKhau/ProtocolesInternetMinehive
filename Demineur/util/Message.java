package util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import game.Board;

/**
 * Pour simplifier le code, {@link util.MyPrintWriter} peut envoyer un message
 * sans créer de new {@link Message}, tandis que {@link util.MyBufferedReader}
 * transmet un {@link Message} facilement traitable.
 */
public class Message {
	/* Client */
	public static final String REGI = "REGI";
	public static final String LSMA = "LSMA";
	public static final String LSAV = "LSAV";
	public static final String LSUS = "LSUS";
	public static final String NWMA = "NWMA";
	public static final String LEAV = "LEAV";
	public static final String IMOK = "IMOK"; // +fromHost, +toHost
	public static final String JOIN = "JOIN";
	public static final String CLIC = "CLIC";
	public static final String IDKC = "IDKC";

	/* Server */
	public static final String IDOK = "IDOK";
	public static final String IDNO = "IDNO";
	public static final String IDIG = "IDIG";
	public static final String LMNB = "LMNB";
	public static final String MATC = "MATC";
	public static final String LANB = "LANB";
	public static final String AVAI = "AVAI";
	public static final String LUNB = "LUNB";
	public static final String USER = "USER";
	public static final String NWOK = "NWOK";
	public static final String FULL = "FULL";
	public static final String NWNO = "NWNO";
	public static final String KICK = "KICK";
	public static final String RQDT = "RQDT";
	public static final String PLNO = "PLNO";
	public static final String PLOK = "PLOK";
	public static final String IDKS = "IDKS";
	public static final String RUOK = "RUOK"; // +toHost, +inHost

	/* Host */
	public static final String JNNO = "JNNO";
	public static final String JNOK = "JNOK";
	public static final String BDIT = "BDIT";
	public static final String IGNB = "IGNB";
	public static final String IGPL = "IGPL";
	public static final String CONN = "CONN";
	public static final String DECO = "DECO";
	public static final String LATE = "LATE";
	public static final String OORG = "OORG";
	public static final String SQRD = "SQRD";
	public static final String ENDC = "ENDC";
	public static final String SCPC = "SCPC";
	public static final String AFKP = "AFKP";
	public static final String BACK = "BACK";
	public static final String SDDT = "SDDT";
	public static final String PLIN = "PLIN";
	public static final String SCPS = "SCPS";
	public static final String ENDS = "ENDS";
	public static final String IDKH = "IDKH";

	/** Associe à chaque type de message le nombre de paramètres attendus */
	public static final Map<String, Integer> TYPES;
	static {
		Map<String, Integer> map = new HashMap<>();
		/* Client */
		map.put(REGI, 2);
		map.put(LSMA, 0);
		map.put(LSAV, 0);
		map.put(LSUS, 0);
		map.put(NWMA, null);
		map.put(LEAV, 0);
		map.put(IMOK, 0);
		map.put(JOIN, 2);
		map.put(CLIC, 2);
		map.put(IDKC, 0);
		map.put(IMOK, 0);

		/* Server */
		map.put(IDOK, 0);
		map.put(IDNO, 0);
		map.put(IDIG, 2);
		map.put(LMNB, 1);
		map.put(MATC, null);
		map.put(LANB, 1);
		map.put(AVAI, 2);
		map.put(LUNB, 1);
		map.put(USER, 2);
		map.put(NWOK, 2);
		map.put(FULL, 0);
		map.put(NWNO, 0);
		map.put(KICK, 0);
		map.put(RQDT, 0);
		map.put(PLNO, 1);
		map.put(PLOK, 2);
		map.put(IDKS, 0);
		map.put(RUOK, 0);

		/* Host */
		map.put(JNNO, 0);
		map.put(JNOK, 1);
		map.put(BDIT, 1 + Board.WIDTH);
		map.put(IGNB, 1);
		map.put(IGPL, 5);
		map.put(CONN, 5);
		map.put(DECO, 1);
		map.put(LATE, 0);
		map.put(OORG, 2);
		map.put(SQRD, 5);
		map.put(ENDC, 1);
		map.put(SCPC, 5);
		map.put(AFKP, 1);
		map.put(BACK, 1);
		map.put(SDDT, null);
		map.put(PLIN, 3);
		map.put(SCPS, 2);
		map.put(ENDS, 1);
		map.put(IDKH, 0);

		TYPES = Collections.unmodifiableMap(map);
	}

	public static final String POSTFIX = "";
	public static final String SEPARATOR = "#";

	private String type;
	private String[] args;
	/**
	 * Contenu souvent optionnel après les paramètres obligatoires du message.
	 * Propre dans l'objet, mais avec des dièses (#) au lieu d'espaces ( ) lors
	 * de l'envoi
	 */
	public String content;

	public Message(String type, String[] args, String content) {
		super();
		if (type == null || type == "") {
			throw new NullPointerException("Tentative de création d'un message réseau sans type. Interdit.");
		}
		this.type = type;
		this.args = args;
		this.content = content;
	}

	public String getType() {
		return type;
	}

	public String[] getArgs() {
		return args;
	}

	/**
	 * Obtenir l'argument du message à l'indice indiquée
	 * 
	 * @return argument, ou chaîne vide si non existant
	 */
	public String getArg(int index) {
		if (args.length - 1 >= index) {
			return args[index];
		} else {
			return null;
		}
	}
	
	public int getArgAsInt(int index) {
		int res;
		try {
			res = Integer.parseInt(getArg(index));
		} catch (NumberFormatException | NullPointerException e) {
			res = 0;
		}
		return res;
	}

	public String getContent() {
		if (content == null) {
			return "";
		}
		return content;
	}

	public static String encode(Message message) {
		StringBuilder sb = new StringBuilder(message.type);
		if (message.args != null) {
			Integer expected = getExpectedArgsLength(message.type);
			if (expected != null && expected != message.args.length) {
				System.err.println("~Encodage, '" + message.toString() + ": Mauvais nombre d'arguments, " + expected
						+ " attendus");
			}
			for (String s : message.args) {
				sb.append(SEPARATOR).append(s);
			}
		}
		if (message.content != null) {
			sb.append(SEPARATOR).append(message.content);
		}
		return sb.toString();
	}

	public static String encode(String type) {
		return encode(type, null, null);
	}

	public static String encode(String type, String[] args) {
		return encode(type, args, null);
	}

	public static String encode(String type, String[] args, String content) {
		return encode(new Message(type, args, content));
	}

	/**
	 * Retranscrit un String en un message. Si le nombre de paramètres pour un
	 * type de message est inconnu (-1), alors tout ce qui suit le type est
	 * considéré comme argument.
	 * 
	 * @param receive
	 * @return
	 */
	public static Message decode(String receive) {
		String[] slices = receive.split(SEPARATOR);
		String type = slices[0];
		if (!TYPES.containsKey(type)) {
			System.err.println("Type de message '" + type + "' non reconnu.");
		}
		Integer expected = getExpectedArgsLength(type);
		if (expected == null) {
			expected = slices.length - 1;
		}
		String[] args = new String[expected];
		String content = null;
		try {
			for (int i = 0; i < expected; i++) {
				args[i] = slices[i + 1];
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("~Décodage, '" + receive + "' : Mauvais nombre d'arguments, " + expected + " attendus.");
		}
		if (slices.length > expected + 1) {
			content = slices[expected + 1];
		}
		return new Message(type, args, content);
	}

	/**
	 * Vérifie qu'il y a le bon nombre d'arguments dans un message donné.
	 * 
	 * @param message
	 * @return true si nombre d'arguments trouvés correspond au type du message
	 */
	public static boolean validArguments(Message message) {
		Integer expected = getExpectedArgsLength(message.type);
		if (expected == null && message.getArgs() == null) {
			return true;
		}
		if (expected != message.getArgs().length) {
			return false;
		}
		for (String s : message.getArgs()) {
			if (s == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Renvoie le nombre d'arguments attendus par type.
	 * 
	 * @param type
	 *            Type du message, en 4 caractères
	 * @return Nombre d'arguments, ou null si non pas déterminable ou mauvais
	 *         type
	 */
	public static Integer getExpectedArgsLength(String type) {
		return TYPES.get(type);
	}

	/** Obtenir le message proprement lors de la réception utilisateur */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(type);
		if (args != null) {
			for (String s : args) {
				sb.append(' ').append(s);
			}
		}
		if (content != null && content != "") {
			sb.append(' ').append(content);
		}
		return sb.toString();
	}

	/**
	 * Contrôle le contenu d'un string pour en faire un message. Beaucoup plus
	 * strict que {@link Message#decode(String)}.
	 * 
	 * @param string
	 * @return Message valide respectant le format du protocole
	 */
	public static Message validMessage(String string) throws IllegalArgumentException {
		if (string.contains("\n") || string.contains("\r")) {
			throw new IllegalArgumentException("Sauts de ligne interdits");
		}
		String[] slices = string.split(SEPARATOR);
		String type = slices[0];
		if (!TYPES.containsKey(type)) {
			throw new IllegalArgumentException(
					"Type de message '" + type + "' non reconnu. Indication : '" + SEPARATOR + "' séparateur.");
		}
		Integer expected = getExpectedArgsLength(type);
		if (expected != null && expected > slices.length - 1) {
			throw new IllegalArgumentException(
					"Nombre de paramètres pour '" + type + "' incorrect. " + expected + " attendus.");
		}
		if (expected == null) {
			expected = slices.length - 1;
		}
		String[] args = new String[expected];
		String content = null;
		for (int i = 0; i < expected; i++) {
			args[i] = slices[i + 1];
		}
		if (slices.length > expected + 1) {
			content = slices[expected + 1];
		}
		return new Message(type, args, content);
	}

}
