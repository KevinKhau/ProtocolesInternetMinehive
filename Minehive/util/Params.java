package util;

import java.nio.file.Path;
import java.nio.file.Paths;


// FUTURE Configurer à partir d'un fichier parameters.properties
/** Paramètres de configuration développeur */
public class Params {

	private static boolean output = true;
	
	/** Afficher les messages bruts lors de leur réception */
	public static boolean rawReception = output && true;
	/** Afficher les messages propres reçus après décodage */
	public static boolean cleanReception = output && false;

	/** Afficher les messages bruts avant expédition */
	public static boolean rawExpedition = output && false;
	/** Afficher les messages codés pour expédition */
	public static boolean codedExpedition = output && true;
	
	public static final Path DIR_BIN = Paths.get("TempusFinis");
	public static final Path DIR_LOG = Paths.get("logs");
	
	public static final Path RES = Paths.get("res");
	public static final Path MINE_EXPLOSION = Paths.get(RES.toString(), "chocobosound.wav");
	public static final boolean CUSTOM_CURSOR = true;
	public static final Path CURSOR = Paths.get(RES.toString(), "chocobo.gif");
	public static final Path LOGO = Paths.get(RES.toString(), "FigaroChocobo.gif");
	public static boolean DEBUG_HOST = true;
	
}
