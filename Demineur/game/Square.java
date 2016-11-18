package game;

/**
 * Extrait du protocole :
 * Case x y : Dans un tel ordre, x est toujours l’abscisse, et y l’ordonnée.
 * Numéro (number) d’une case : Numéro, si existant, d’une case, dans [0, 8].
 * Valeur (value) d’une case : peut valoir soit {mine, number}.
 * Contenu (content) d’une case : peut valoir soit {hidden, value}.
 */
public class Square {
	
	public static final int MIN_VALUE = -1;
	public static final int MAX_VALUE = 8;
	
	public boolean hidden;
	public int value;
	
	public Square() {
		hidden = true;
		value = 0;
	}
}
