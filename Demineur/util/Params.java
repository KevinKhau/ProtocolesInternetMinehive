package util;

/** Paramètres de configuration développeur */
public class Params {

	private static boolean output = false;
	
	/** Afficher les messages bruts lors de leur réception */
	public static boolean rawReception = output && true;
	/** Afficher les messages propres reçus après décodage */
	public static boolean cleanReception = output && true;

	/** Afficher les messages bruts avant expédition */
	public static boolean rawExpedition = output && true;
	/** Afficher les messages codés pour expédition */
	public static boolean codedExpedition = output && true;
	
}
