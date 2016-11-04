package game;

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import network.Client;

public class Board {
	public static final int WIDTH = 30;
	public static final int HEIGHT = 16;
	
	public static final byte VALUE_MASK = 	(byte) 0b00001111;
	public static final byte HIDDEN = 		(byte) 0b00100000;
	public static final byte BOMB = 		(byte) 0b01000000;
	
	private boolean first;
	private byte board[];
	
	public Board() {
		board = new byte[WIDTH * HEIGHT];
		reset();
	}
	
	// Pas vraiment besoin en principe, mais pour te faire plaisir
	public Square squareAt(int x, int y) {
		Square sq = new Square();
		sq.hidden = isHiddenAt(x, y);
		if (isBombAt(x, y))
			sq.value = -1;
		else
			sq.value = valueAt(x, y);
		
		return sq;
	}
	
	public boolean isHiddenAt(int x, int y) {
		return ((board[x + y * WIDTH] & HIDDEN) != 0);
	}
	
	public boolean isBombAt(int x, int y) {
		return ((board[x + y * WIDTH] & BOMB) != 0);
	}
	
	public int valueAt(int x, int y) {
		return board[x + y * WIDTH] & VALUE_MASK;
	}
	
	public void reset() {
		// Reset board
		Arrays.fill(board, HIDDEN);
		
		// Place new bombs
		initBombs();
		
		// Next player will be first
		first = true;
	}
	
	private void initBombs() {
		Random rand = new Random();
		
		// Get number of bombs to place
		int bombs = 0;
		while (bombs <= 5) {
			bombs = rand.nextInt(board.length / 5);
		}
		
		// Get bomb positions
		for (int i = 0; i < bombs; i++) {
			int x = rand.nextInt(WIDTH);
			int y = rand.nextInt(HEIGHT);
			int position = x + y * WIDTH;
			
			// Add the bomb and increment the squares around
			if((board[position] & BOMB) != 0) {
				// A bomb is already present here, will try another place
				i--;
			} else {
				// Create bomb
				board[position] = BOMB | HIDDEN;
				
				// Increment values around the bomb
				// Left
				if (x > 0) {
					board[position - 1]++;
				}
				
				// Top Left
				if (x > 0 && y > 0) {
					board[position - 1 - WIDTH]++;
				}
				
				// Up
				if (y > 0) {
					board[position - WIDTH]++;
				}
				
				// Top Right
				if (x < WIDTH - 1 && y > 0) {
					board[position + 1 - WIDTH]++;
				}
				
				// Right
				if (x < WIDTH - 1) {
					board[position + 1]++;
				}
				
				// Right Bottom
				if (x < WIDTH - 1 && y < HEIGHT - 1) {
					board[position + 1 + WIDTH]++;
				}
				
				// Down
				if (y < HEIGHT - 1) {
					board[position + WIDTH]++;
				}
				
				// Left Bottom
				if (x > 0 && y < HEIGHT - 1) {
					board[position - 1 + WIDTH]++;
				}
			}
		}
	}

	public int clickAt(int x, int y) {
		int position = x + y * WIDTH;
		int points = 0; // TODO

		if ((board[position + WIDTH] & BOMB) != 0) { // It's a bomb !
			// If it's the first click of the game, remove the bomb
			if (first) {
				// Remove the bomb
				board[position + WIDTH] = HIDDEN; 
				
				// Change the values corresponding
				// Left
				if (x > 0) {
					if ((board[position - 1] & BOMB) == 0)
						board[position - 1]--;
				}
				
				// Top Left
				if (x > 0 && y > 0) {
					if ((board[position - 1 - WIDTH] & BOMB) == 0)
						board[position - 1 - WIDTH]--;
				}
				
				// Up
				if (y > 0) {
					if ((board[position - WIDTH] & BOMB) == 0)
						board[position - WIDTH]--;
				}
				
				// Top Right
				if (x < WIDTH - 1 && y > 0) {
					if ((board[position + 1 - WIDTH] & BOMB) == 0)
						board[position + 1 - WIDTH]--;
				}
				
				// Right
				if (x < WIDTH - 1) {
					if ((board[position + 1] & BOMB) == 0)
						board[position + 1]--;
				}
				
				// Right Bottom
				if (x < WIDTH - 1 && y < HEIGHT - 1) {
					if ((board[position + 1 + WIDTH] & BOMB) == 0)
						board[position + 1 + WIDTH]--;
				}
				
				// Down
				if (y < HEIGHT - 1) {
					if ((board[position + WIDTH] & BOMB) == 0)
						board[position + WIDTH]--;
				}
				
				// Left Bottom
				if (x > 0 && y < HEIGHT - 1) {
					if ((board[position - 1 + WIDTH] & BOMB) == 0)
						board[position - 1 + WIDTH]--;
				}
				
				first = false;
				return clickAt(x, y);
			}
			
			board[position] ^= HIDDEN; // Set visible
			return -1; // TODO
			
		} else { // it's not a bomb

			// Do all the magic
			return reveal(x, y);
		}
	}
	
	private int reveal(int x, int y) {
		int position = x + y * WIDTH;
		int points = 0; // TODO
		
		if ((board[position] & HIDDEN) == 0) { // Already visible
			this.display();
			return 0;
		}
		
		board[position] ^= HIDDEN; // Set visible
		
		// Check for current square value
		if ((board[position] & VALUE_MASK) > 0) {
			return 0;
		}
		
		// Is empty, need to search adjacent squares
		// Left
		if (x > 0) {
			if ((board[position - 1] & HIDDEN) != 0)
				points += reveal(x - 1, y);
		}
		
		// Top Left
		if (x > 0 && y > 0) {
			if ((board[position - 1 - WIDTH] & HIDDEN) != 0)
				points += reveal(x - 1, y - 1);
		}
		
		// Up
		if (y > 0) {
			if ((board[position - WIDTH] & HIDDEN) != 0)
				points += reveal(x, y - 1);
		}
		
		// Top Right
		if (x < WIDTH - 1 && y > 0) {
			if ((board[position + 1 - WIDTH] & HIDDEN) != 0)
				points += reveal(x + 1, y - 1);
		}
		
		// Right
		if (x < WIDTH - 1) {
			if ((board[position + 1] & HIDDEN) != 0)
				points += reveal(x + 1, y);
		}
		
		// Right Bottom
		if (x < WIDTH - 1 && y < HEIGHT - 1) {
			if ((board[position + 1 + WIDTH] & HIDDEN) != 0)
				points += reveal(x + 1, y + 1);
		}
		
		// Down
		if (y < HEIGHT - 1) {
			if ((board[position + WIDTH] & HIDDEN) != 0)
				points += reveal(x, y + 1);
		}
		
		// Left Bottom
		if (x > 0 && y < HEIGHT - 1) {
			if ((board[position - 1 + WIDTH] & HIDDEN) != 0)
				points += reveal(x - 1, y + 1);
		}
		
		return points;
	}

	public void display() {
		for (int i = 0; i < HEIGHT; i++) {
			for (int j = 0; j < WIDTH; j++) {
				if ((board[j + i * WIDTH] & HIDDEN) != 0) {
					System.out.print('#'); // Hidden
				} else {
					if ((board[j + i * WIDTH] & BOMB) != 0) {
						System.out.print('X'); // Bomb
					} else {
						if ((board[j + i * WIDTH] & VALUE_MASK) == 0) {
							System.out.print(' '); // Empty
						} else {
							/* UNIX only
							int val = board[j + i * WIDTH] & VALUE_MASK;
							switch (val) {
							case 1:
								System.out.print("\u001B[34m"); // Blue
								break;
							case 2:
								System.out.print("\u001B[32m"); // Green
								break;
							case 3:
								System.out.print("\u001B[31m"); // Red
								break;
							case 4:
								System.out.print("\u001B[35m"); // Dark blue
								break;
							case 5:
								System.out.print("\u001B[35m"); // Dark red
								break;
							case 6:
								System.out.print("\u001B[36m"); // Teal
								break;
							case 7:
								System.out.print(""); // Black
								break;
							case 8:
								System.out.print(""); // Grey
								break;
							}
							System.out.print((board[j + i * WIDTH] & VALUE_MASK) + "\u001B[0m");*/
							
							System.out.print((board[j + i * WIDTH] & VALUE_MASK)); // Number
						}
					}
				}
			}
			System.out.println();
		}
	}
	
	public static void main(String[] args) {
		boolean quit = false;
		Board b = new Board();
		Scanner scanner = new Scanner(System.in);

		while(!quit) {
			b.reset();
			boolean game_over = false;
			System.out.println("New game started");
			
			while(!game_over) {
				b.display();
				
				System.out.println("enter x: ");
				int x = scanner.nextInt();
				
				System.out.println("enter y: ");
				int y = scanner.nextInt();
				
				b.clickAt(x, y);
			}
		}
		
		scanner.close();
	}
}
