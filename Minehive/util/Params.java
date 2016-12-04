package util;

import java.nio.file.Path;
import java.nio.file.Paths;

import network.Host;


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
	
	/* Directories */
	public static final Path BIN = Paths.get("TempusFinis");
	public static final Path LOG = Paths.get(Params.BIN.toString(), "logs");
	public static final Path RES = Paths.get("res");
	public static final Path HOST_JAR = Paths.get(BIN.toString(), Host.JAR_NAME);
	
	public static final Path MINE_EXPLOSION = Paths.get(RES.toString(), "chocobosound.wav");
	public static final boolean CUSTOM_CURSOR = true;
	public static final Path CURSOR = Paths.get(RES.toString(), "chocobo.gif");
	public static final Path LOGO = Paths.get(RES.toString(), "FigaroChocobo.gif");
	
	/**
	 * Pour lancer manuellement l'hôte et voir tous les messages affichés sur
	 * terminal. Un seul argument à préciser : le port de connexion. Attention
	 * cependant : faire varier le nom de partie (Par défaut Partie_1) si
	 * nécessaire.
	 */
	public static boolean DEBUG_HOST = false;
	
}
