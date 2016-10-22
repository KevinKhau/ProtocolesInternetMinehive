package util;

public class StringUtil {

	public static String getSpaces(int size) {
		return new String(new char[size]).replace('\0', ' ');
	}
	
}
