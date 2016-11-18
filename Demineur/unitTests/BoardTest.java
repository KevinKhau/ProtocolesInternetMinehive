package unitTests;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;

import game.Board;

public class BoardTest {

	Scanner reader = new Scanner(System.in);

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
		
		int x = intKeyboardInput("x");
		int y = intKeyboardInput("y");
		List<String[]> msgs = b.clickAt(x, y, "Maxwell");
		msgs.forEach(l -> System.out.println(Arrays.toString(l)));
		
	}
	
	private int intKeyboardInput(String indication) {
		if (indication != null) {
			System.out.println(indication);
		}
		while (!reader.hasNextInt()) {
			System.out.print("Tapez un entier : ");
			reader.next();
		}
		return reader.nextInt();
	}
	
//	@Test
	public void listOfArrays(){
		List<String[]> list = new ArrayList<>();
		list.add(new String[]{"Hey", "Wendy"});
		list.add(new String[]{"Bye", "Abigail"});
		list.forEach(l -> System.out.println(Arrays.toString(l)));
	}
}
