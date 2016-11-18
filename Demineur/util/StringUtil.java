package util;

public class StringUtil {

	/**
	 * @param size
	 * @return String de taille {@code size} avec seulement des espaces
	 */
	public static String getSpaces(int size) {
		return new String(new char[size]).replace('\0', ' ');
	}
	
}
