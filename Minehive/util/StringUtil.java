package util;

public final class StringUtil {

	private StringUtil(){}
	
	/**
	 * @param size
	 * @return String de taille {@code size} avec seulement des espaces
	 */
	public static String getSpaces(int size) {
		return new String(new char[size]).replace('\0', ' ');
	}
	
	public static boolean isInteger(String str) {
	    if (str == null) {
	        return false;
	    }
	    int length = str.length();
	    if (length == 0) {
	        return false;
	    }
	    int i = 0;
	    if (str.charAt(0) == '-') {
	        if (length == 1) {
	            return false;
	        }
	        i = 1;
	    }
	    for (; i < length; i++) {
	        char c = str.charAt(i);
	        if (c < '0' || c > '9') {
	            return false;
	        }
	    }
	    return true;
	}

	/**
	 * Obtenir seulement la partie adresse d'une InetAddress.
	 * Exemple : "Agito/192.168.0.7" -> "192.168.0.7" 
	 */
	public static String truncateAddress(String IP) {
		if (IP.contains("/")) {
			String[] slices = IP.split("/");
			if (slices.length >= 2) {
				IP = slices[1];
			} else {
				IP = slices[0];
			}
		}
		return IP;
	}
	
}
