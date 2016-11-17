package game;

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;


public class Board {
	public static final int WIDTH = 30;
	public static final int HEIGHT = 16;
	public static final int NB_BOMBS = 99;

	private static final byte VALUE_MASK = 	(byte) 0b00001111;
	
	private static final byte HIDDEN_BIT = 	(byte) 0b00100000;
	private static final byte REVEAL_MASK = (byte) 0b11011111;
	
	private static final byte BOMB_BIT = 	(byte) 0b01000000;
	private static final byte DEFUSE_BOMB = (byte) 0b10111111;
	
	private boolean first;
	private byte board[];
	
	public Board() {
		board = new byte[WIDTH * HEIGHT];
		reset();
	}
	
	public boolean isHiddenAt(int x, int y) {
		return ((board[x + y * WIDTH] & HIDDEN_BIT) != 0);
	}
	
	public boolean isBombAt(int x, int y) {
		return ((board[x + y * WIDTH] & BOMB_BIT) != 0);
	}
	
	public int valueAt(int x, int y) {
		return board[x + y * WIDTH] & VALUE_MASK;
	}
	
	public void reset() {
		// Reset board
		Arrays.fill(board, HIDDEN_BIT);
		
		// Place new bombs
		initBombs();
		
		// Next player will be first
		first = true;
	}
	
	private void initBombs() {
		Random rand = new Random();
		
		// Get number of bombs to place
		/*int bombs = 0;
		while (bombs <= 5) {
			bombs = rand.nextInt(board.length / 5);
		}*/
		int bombs = NB_BOMBS;
		
		// Get bomb positions
		for (int i = 0; i < bombs; i++) {
			int x = rand.nextInt(WIDTH);
			int y = rand.nextInt(HEIGHT);
			int position = x + y * WIDTH;
			
			// Add the bomb and increment the squares around
			if((board[position] & BOMB_BIT) != 0) {
				// A bomb is already present here, will try another place
				i--;
			} else {
				// Create bomb
				board[position] = BOMB_BIT | HIDDEN_BIT;
				
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

	/* Retourne les points du joueur à ajouter ou retirer lors du clic */
	public int clickAt(int x, int y) {
		int position = x + y * WIDTH;

		if ((board[position] & BOMB_BIT) != 0) { // It's a bomb !
			// If it's the first click of the game, remove the bomb
			if (first) {
				// Remove the bomb
				board[position] = HIDDEN_BIT; 
				
				// Change the values corresponding
				// Left
				if (x > 0) {
					if ((board[position - 1] & BOMB_BIT) == 0)
						board[position - 1]--;
					else 
						board[position]++;
				}
				
				// Top Left
				if (x > 0 && y > 0) {
					if ((board[position - 1 - WIDTH] & BOMB_BIT) == 0)
						board[position - 1 - WIDTH]--;
					else 
						board[position]++;
				}
				
				// Up
				if (y > 0) {
					if ((board[position - WIDTH] & BOMB_BIT) == 0)
						board[position - WIDTH]--;
					else 
						board[position]++;
				}
				
				// Top Right
				if (x < WIDTH - 1 && y > 0) {
					if ((board[position + 1 - WIDTH] & BOMB_BIT) == 0)
						board[position + 1 - WIDTH]--;
					else 
						board[position]++;
				}
				
				// Right
				if (x < WIDTH - 1) {
					if ((board[position + 1] & BOMB_BIT) == 0)
						board[position + 1]--;
					else 
						board[position]++;
				}
				
				// Right Bottom
				if (x < WIDTH - 1 && y < HEIGHT - 1) {
					if ((board[position + 1 + WIDTH] & BOMB_BIT) == 0)
						board[position + 1 + WIDTH]--;
					else 
						board[position]++;
				}
				
				// Down
				if (y < HEIGHT - 1) {
					if ((board[position + WIDTH] & BOMB_BIT) == 0)
						board[position + WIDTH]--;
					else 
						board[position]++;
				}
				
				// Left Bottom
				if (x > 0 && y < HEIGHT - 1) {
					if ((board[position - 1 + WIDTH] & BOMB_BIT) == 0)
						board[position - 1 + WIDTH]--;
					else 
						board[position]++;
				}
				
				first = false;
				return clickAt(x, y);
			}
			
			board[position] &= REVEAL_MASK; // Set visible
			return -10;
			
		} else { // it's not a bomb
			first = false;
			
			// Do all the magic
			return reveal(x, y);
		}
	}
	
	private int reveal(int x, int y) {
		int position = x + y * WIDTH;
		int points = 0; // TODO
		
		if ((board[position] & HIDDEN_BIT) == 0) { // Already visible
			return 0;
		}
		
		board[position] &= REVEAL_MASK; // Set visible
		
		// Check for current square value
		if ((board[position] & VALUE_MASK) > 0) {
			return board[position] & VALUE_MASK;
		}
		
		// Is empty, need to search adjacent squares
		// Left
		if (x > 0) {
			if ((board[position - 1] & HIDDEN_BIT) != 0)
				points += reveal(x - 1, y);
		}
		
		// Top Left
		if (x > 0 && y > 0) {
			if ((board[position - 1 - WIDTH] & HIDDEN_BIT) != 0)
				points += reveal(x - 1, y - 1);
		}
		
		// Up
		if (y > 0) {
			if ((board[position - WIDTH] & HIDDEN_BIT) != 0)
				points += reveal(x, y - 1);
		}
		
		// Top Right
		if (x < WIDTH - 1 && y > 0) {
			if ((board[position + 1 - WIDTH] & HIDDEN_BIT) != 0)
				points += reveal(x + 1, y - 1);
		}
		
		// Right
		if (x < WIDTH - 1) {
			if ((board[position + 1] & HIDDEN_BIT) != 0)
				points += reveal(x + 1, y);
		}
		
		// Right Bottom
		if (x < WIDTH - 1 && y < HEIGHT - 1) {
			if ((board[position + 1 + WIDTH] & HIDDEN_BIT) != 0)
				points += reveal(x + 1, y + 1);
		}
		
		// Down
		if (y < HEIGHT - 1) {
			if ((board[position + WIDTH] & HIDDEN_BIT) != 0)
				points += reveal(x, y + 1);
		}
		
		// Left Bottom
		if (x > 0 && y < HEIGHT - 1) {
			if ((board[position - 1 + WIDTH] & HIDDEN_BIT) != 0)
				points += reveal(x - 1, y + 1);
		}
		
		return points;
	}
	
	public void revealAll() {
		for (int i = 0; i < board.length; i++) {
			board[i] &= REVEAL_MASK;
		}
	}
	
	public void draw(GraphicsContext gc, Color[] array, int tile, int gap) {
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.CENTER);
		gc.setFont(new Font(tile));
		
		for (int i = 0; i < HEIGHT; i++) {
			for (int j = 0; j < WIDTH; j++) {
				gc.setFill(array[j + i * WIDTH]);
				gc.fillRect(j * (tile + gap), i * (tile + gap), tile, tile);
				
				if ((board[j + i * WIDTH] & HIDDEN_BIT) == 0) {
					if ((board[j + i * WIDTH] & BOMB_BIT) != 0) {
						gc.setFill(Color.BLACK);
						gc.fillText("¤", j * (tile + gap) + (tile + gap) / 2, i * (tile + gap) + (tile + gap) / 2, tile);
					} else {
						if ((board[j + i * WIDTH] & VALUE_MASK) != 0) {
							int val = board[j + i * WIDTH] & VALUE_MASK;
							switch (val) {
							case 1:
								gc.setFill(Color.BLUE);
								break;
							case 2:
								gc.setFill(Color.GREEN);
								break;
							case 3:
								gc.setFill(Color.RED);
								break;
							case 4:
								gc.setFill(Color.DARKBLUE);
								break;
							case 5:
								gc.setFill(Color.DARKRED);
								break;
							case 6:
								gc.setFill(Color.DARKCYAN);
								break;
							case 7:
								gc.setFill(Color.BLACK);
								break;
							case 8:
								gc.setFill(Color.CHOCOLATE);
								break;
							}
							gc.fillText(Integer.toString(val), j * (tile + gap) + (tile + gap) / 2, i * (tile + gap) + (tile + gap) / 2, tile);
						}
					}
				}
				
			}
		}
	}

	public void display() {
		for (int i = 0; i < HEIGHT; i++) {
			for (int j = 0; j < WIDTH; j++) {
				if ((board[j + i * WIDTH] & HIDDEN_BIT) != 0) {
					System.out.print('#'); // Hidden
				} else {
					if ((board[j + i * WIDTH] & BOMB_BIT) != 0) {
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
	
	/*
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
				
				if (x == -1 || y == -1)
					b.revealAll();
				else
					b.clickAt(x, y);
			}
		}
		
		scanner.close();
	}*/
}
