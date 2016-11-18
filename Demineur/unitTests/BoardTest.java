package unitTests;

import static game.Board.HEIGHT;
import static game.Board.WIDTH;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import game.Board;

public class BoardTest {

	@Test
	public void board() {
		Board b = new Board();
		System.out.println(b);
		b.display();
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				System.out.print(b.valueAt(x, y) + " ");
			}
			System.out.println();
		}
		
//		b.revealAll();
		
		for (int i = 0; i < HEIGHT; i++) {
			List<String> line = b.lineContentAt(i);
			line.add(0, String.valueOf(i));
			System.out.println(line);
//			System.out.println(Arrays.toString(line.stream().toArray(String[]::new)));
		}
		
	}

}
