package unitTests;

import static org.junit.Assert.*;

import java.util.NoSuchElementException;
import java.util.Scanner;

import org.junit.Test;

public class ScannerTest {

	Scanner sc = new Scanner(System.in);
	
	public void test() {
		System.out.println("Entier reçu : '" + sc.nextInt() + "'");
		System.out.println("Ligne reçue : '" + sc.nextLine() + "'");
		// http://stackoverflow.com/questions/13102045/scanner-is-skipping-nextline-after-using-next-nextint-or-other-nextfoo
		sc.close();
	}

	@Test
	public void onlyInts() throws InterruptedException {
		while (true) {
			try {
				int option = Integer.parseInt(sc.nextLine());
			} catch (NumberFormatException e) {
				System.err.println("Entier attendu !");
			} catch (NoSuchElementException e) {
				break;
			}
		}
		Thread.sleep(1000);
		System.out.println("next");
		System.out.println(sc.nextLine());
		sc.close();
	}
	
}
