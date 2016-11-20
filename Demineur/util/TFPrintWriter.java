package util;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import static util.Message.encode;
import static util.StringUtil.getSpaces;

@Deprecated
/**
 * Abstraction supérieure de PrintWriter : Utiliser .send(...), qui code les
 * messages au bon format avant de les envoyer
 */
public class TFPrintWriter extends PrintWriter {

	public TFPrintWriter(OutputStreamWriter osw) {
		super(osw, true);
	}

	/**
	 * Encode selon le format du protocole : TYPE args... contenu
	 * 
	 * @param type
	 *            Type du message, composé de 4 lettres
	 * @param args
	 *            Arguments, nombre dépendant du type
	 * @param content
	 *            Contenu du message
	 */
	public void send(String type, String[] args, String content) {
		String before = "> Envoi : ";
		if (Params.rawExpedition || Params.codedExpedition) {
			System.out.print(before);
		}

		Message m = new Message(type, args, content);

		if (Params.rawExpedition) {
			System.out.println("'" + m + "' (brut).");
		}

		String codedMsg = encode(m);

		if (Params.codedExpedition) {
			String prefix = "";
			if (Params.rawExpedition) {
				prefix = getSpaces(before.length());
			}
			System.out.println(prefix + "'" + codedMsg + "' (codé).");
		}

		super.println(codedMsg);
	}

	public void send(String type, String[] args) {
		send(type, args, null);
	}

	public void send(String type) {
		send(type, null, null);
	}

	public void send(Message message) {
		send(message.getType(), message.getArgs(), message.getContent());
	}

}
