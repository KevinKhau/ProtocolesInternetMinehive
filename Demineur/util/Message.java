package util;

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
	public static final String IMOK = "IMOK";
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
	public static final String KICK = "KICK";
	public static final String RQDT = "RQDT";
	public static final String PLNO = "PLNO";
	public static final String PLOK = "PLOK";
	public static final String IDKS = "IDKS";

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
	public static final String SDDT = "SDDT";
	public static final String PLIN = "PLIN";
	public static final String SCPS = "SCPS";
	public static final String ENDS = "ENDS";
	public static final String IDKH = "IDKH";

	public static final String POSTFIX = "";
	public static final String SEPARATOR = "#";

	private String type;
	private String[] args;
	/** Contenu souvent optionnel après les paramètres obligatoires du message.
	 * Propre dans l'objet, mais avec des dièses (#) au lieu d'espaces ( ) lors
	 * de l'envoi */
	private String content;

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

	public String getContent() {
		return content;
	}

	public static String encode(Message message) {
		StringBuilder sb = new StringBuilder(message.type);
		if (message.args != null) {
			int expected = getExpectedArgsLength(message.type);
			if (expected != -1 && expected != message.args.length) {
				System.err.println("~Encodage, '" + message.toString() + ": Mauvais nombre d'arguments, " + expected + " attendus");
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

	public static Message decode(String receive) {
		String[] slices = receive.split(SEPARATOR);
		String type = slices[0];
		int nbArgs = getExpectedArgsLength(type);
		if (nbArgs == -1) {
			return new Message(type, null, null);
		}
		String[] args = new String[nbArgs];
		String content = null;
		try {
			for (int i = 0; i < nbArgs; i++) {
				args[i] = slices[i + 1];
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("~Décodage, '" + receive + "' : Mauvais nombre d'arguments, " + nbArgs + " attendus.");
		}
		if (slices.length > nbArgs + 1) {
			content = slices[nbArgs + 1];
		}
		return new Message(type, args, content);
	}

	/**
	 * Vérifie qu'il y a le bon nombre d'arguments dans un message
	 * 
	 * @param message
	 * @return true si nombre d'arguments trouvés correspond au type du message
	 */
	public static boolean validArguments(Message message) {
		int expected = getExpectedArgsLength(message.type);
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
	 * @return Nombre d'arguments, ou -1 s'il n'est pas déterminable
	 */
	public static int getExpectedArgsLength(String type) {
		switch (type) {
		/* Server */
		case REGI:
			return 2;
		case LSMA:
			return 0;
		case LSAV:
			return 0;
		case LSUS:
			return 0;
		case NWMA:
			return 10;
		case LEAV:
			return 0;
		case IMOK:
			return 0;
		case JOIN:
			return 2;
		case CLIC:
			return 2;

		/* Client */
		case IDOK:
			return 0;
		case IDNO:
			return 0;
		case IDIG:
			return 2;
		case LMNB:
			return 1;
		case MATC:
			return -1;
		case LANB:
			return 1;
		case AVAI:
			return 2;
		case LUNB:
			return 1;
		case USER:
			return 2;
		case NWOK:
			return 2;
		case FULL:
			return 0;
		case KICK:
			return 0;
		case RQDT:
			return 0;
		case PLNO:
			return 2;
		case PLOK:
			return 2;

		/* Host */
		case JNNO:
			return 0;
		case JNOK:
			return 1;
		case BDIT:
			return 1 + Board.WIDTH;
		case IGNB:
			return 1;
		case IGPL:
			return 5;
		case CONN:
			return 5;
		case DECO:
			return 1;
		case LATE:
			return 0;
		case OORG:
			return 2;
		case SQRD:
			return 5;
		case ENDC:
			return 1;
		case SCPC:
			return 5;
		case SDDT:
			return -1;
		case PLIN:
			return 3;
		case SCPS:
			return 2;
		case ENDS:
			return 1;
		default:
			return -1;
		}
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

}
