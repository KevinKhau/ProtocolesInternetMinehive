package unitTests;


import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;

import game.Board;

public class BoardTest {


	@Test
	public void board() {
		Board b = new Board();
		System.out.println(b);
		b.display();
		for (int y = 0; y < b.height; y++) {
			for (int x = 0; x < b.width; x++) {
				System.out.print(b.valueAt(x, y) + " ");
			}
			System.out.println();
		}
		
//		b.revealAll();
		
		for (int i = 0; i < b.height; i++) {
			List<String> line = b.lineContentAt(i);
			line.add(0, String.valueOf(i));
			System.out.println(line);
//			System.out.println(Arrays.toString(line.stream().toArray(String[]::new)));
		}
		
	}
	
	@Test
	public void scannerInt() {
		System.out.print("input");
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("Enter a whole number.");
            String input = sc.next();
            int intInputValue = 0;
            try {
                intInputValue = Integer.parseInt(input);
                System.out.println("Correct input, exit");
                break;
            } catch (NumberFormatException ne) {
                System.out.println("Input is not a number, continue");
            }
        }
        sc.close();
	}

}
