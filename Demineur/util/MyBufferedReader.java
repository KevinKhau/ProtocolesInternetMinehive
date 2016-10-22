package util;

import static util.Message.decode;
import static util.StringUtil.getSpaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class MyBufferedReader extends BufferedReader {

	public MyBufferedReader(Reader in) {
		super(in);
	}

	public Message receive() throws IOException {
		String before = "< Réception : ";
		if (Params.rawReception || Params.cleanReception) {
			System.out.print(before);
		}
		
		String raw = super.readLine();
		
		if (Params.rawReception) {
			System.out.println("'" + raw + "' (brut).");
		}
		
		if (raw == null) {
			System.err.println("Message vide reçu. Interruption.");
			throw new IOException();
		}
		Message msg = decode(raw);
		
		if (Params.cleanReception) {
			String prefix = "";
			if (Params.rawReception) {
				prefix = getSpaces(before.length());
			}
			System.out.println(prefix + "'" + msg + "' (clean).");
		}
		
		return msg;
	}

}
