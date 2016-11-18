package game;

import static game.Square.MAX_VALUE;
import static game.Square.MIN_VALUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import util.Message;
import util.Points;

public class Board {
	public static final int WIDTH = 30;
	private static final int HEIGHT = 16;
	private static final int NB_BOMBS = 99;
	
	/* En cas d'implémentation future où ces données peuvent varier */
	public final int width = WIDTH;
	public final int height = HEIGHT;
	public final int nb_bombs = NB_BOMBS;
	
	public static final int BOMB_VAL = -1;

	private static final byte VALUE_MASK = 	(byte) 0b00001111;
	
	private static final byte HIDDEN_BIT = 	(byte) 0b00100000;
	private static final byte REVEAL_MASK = (byte) 0b11011111;
	
	private static final byte BOMB_BIT = 	(byte) 0b01000000;
	private static final byte DEFUSE_BOMB = (byte) 0b10111111;
	
	private boolean first;
	
	/** FUTURE Rendre volatile chaque élément de board */
	private byte board[];
	
	public Board() {
		board = new byte[width * height];
		reset();
	}
	
	public boolean isHiddenAt(int x, int y) {
		return isHiddenFrom(board[x + y * width]);
	}
	public boolean isHiddenFrom(byte square) {
		return (square & HIDDEN_BIT) != 0;
	}
	
	public boolean isBombAt(int x, int y) {
		return isBombFrom(board[x + y * width]);
	}
	public boolean isBombFrom(byte square) {
		return (square & BOMB_BIT) != 0;
	}
	
	/**
	 * Renvoie la valeur de la case, indépendamment des autres bits (mine, caché, etc.)
	 * @param x
	 * @param y
	 * @return
	 */
	public int numberAt(int x, int y) {
		return numberFrom(board[x + y * width]);
	}
	public int numberFrom(byte square) {
		int res = square & VALUE_MASK;
		if (res > MAX_VALUE) {
			throw new ArrayIndexOutOfBoundsException("Valeur de case invalide. Maximum : " + Square.MAX_VALUE + ".");
		}
		return res;
	}
	
	/**
	 * Obtenir la valeur d'une case case, qu'elle soit cachée ou non.
	 * S'il y a une mine, renvoie -1.
	 */
	public int valueAt(int x, int y) {
		if (isBombAt(x, y)) {
			return -1;
		}
		return numberAt(x, y);
	}

	/**
	 * Mettre à jour une case du tableau. Attention à bien affecter une case du tableau, et non une copie.
	 * @param x
	 * @param y
	 * @param content
	 */
	public synchronized void updateValueAt(int x, int y, int content) {
		if (content < MIN_VALUE || content > MAX_VALUE) {
			System.err.println("Mise à jour de case (" + x + ", " + y + ") impossible.");
			throw new ArrayIndexOutOfBoundsException("Valeur de case invalide. Minimum : " + MIN_VALUE + ", Maximum : " + MAX_VALUE + ".");
		}
		if (content == -1) {
			board[x + y * width] = BOMB_BIT;
		} else {
			/* Update number */
			board[x + y * width] = (byte) content;
		}
	}
	
	public String contentAt(int x, int y) {
		return contentFrom(board[x + y * width]);
	}
	/**
	 * Obtenir le contenu d'une case. 
	 * @param square
	 * @return Si cachée : X. Si mine : -1. Sinon, valeur.
	 */
	public synchronized String contentFrom(byte square) {
		if (isHiddenFrom(square)) {
			return "X";
		} else if (isBombFrom(square)) {
			return "-1";
		} else {
			return String.valueOf(numberFrom(square));
		}
	}
	
	public synchronized LinkedList<String> lineContentAt(int ordinate) {
		byte[] line = line(ordinate);
		LinkedList<String> res = new LinkedList<>();
		for (byte e : line) {
			res.add(contentFrom(e));
		}
		return res;
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
		int bombs = nb_bombs;
		
		// Get bomb positions
		for (int i = 0; i < bombs; i++) {
			int x = rand.nextInt(width);
			int y = rand.nextInt(height);
			int position = x + y * width;
			
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
					board[position - 1 - width]++;
				}
				
				// Up
				if (y > 0) {
					board[position - width]++;
				}
				
				// Top Right
				if (x < width - 1 && y > 0) {
					board[position + 1 - width]++;
				}
				
				// Right
				if (x < width - 1) {
					board[position + 1]++;
				}
				
				// Right Bottom
				if (x < width - 1 && y < height - 1) {
					board[position + 1 + width]++;
				}
				
				// Down
				if (y < height - 1) {
					board[position + width]++;
				}
				
				// Left Bottom
				if (x > 0 && y < height - 1) {
					board[position - 1 + width]++;
				}
			}
		}
	}

	/** Retourne les points du joueur à ajouter ou retirer lors du clic */
	public ArrayList<String> clickAt(int x, int y, String user) {
		int position = x + y * width;
		MessageList list = new MessageList(user);

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
					if ((board[position - 1 - width] & BOMB_BIT) == 0)
						board[position - 1 - width]--;
					else 
						board[position]++;
				}
				
				// Up
				if (y > 0) {
					if ((board[position - width] & BOMB_BIT) == 0)
						board[position - width]--;
					else 
						board[position]++;
				}
				
				// Top Right
				if (x < width - 1 && y > 0) {
					if ((board[position + 1 - width] & BOMB_BIT) == 0)
						board[position + 1 - width]--;
					else 
						board[position]++;
				}
				
				// Right
				if (x < width - 1) {
					if ((board[position + 1] & BOMB_BIT) == 0)
						board[position + 1]--;
					else 
						board[position]++;
				}
				
				// Right Bottom
				if (x < width - 1 && y < height - 1) {
					if ((board[position + 1 + width] & BOMB_BIT) == 0)
						board[position + 1 + width]--;
					else 
						board[position]++;
				}
				
				// Down
				if (y < height - 1) {
					if ((board[position + width] & BOMB_BIT) == 0)
						board[position + width]--;
					else 
						board[position]++;
				}
				
				// Left Bottom
				if (x > 0 && y < height - 1) {
					if ((board[position - 1 + width] & BOMB_BIT) == 0)
						board[position - 1 + width]--;
					else 
						board[position]++;
				}
				
				first = false;
				return clickAt(x, y, user);
			}
			list.add(x, y, Points.BOMB, -1);
			board[position] &= REVEAL_MASK; // Set visible
			return list.getList();
			
		} else { // it's not a bomb
			first = false;
			
			// Do all the magic
			reveal(x, y, list);
			return list.getList();
		}
	}
	
	private void reveal(int x, int y, MessageList list) {
		int position = x + y * width;
		
		if ((board[position] & HIDDEN_BIT) == 0) { // Already visible
			return;
		}
		
		board[position] &= REVEAL_MASK; // Set visible
		
		// Check for current square value
		if ((board[position] & VALUE_MASK) > 0) {
			list.add(x, y, board[position] & VALUE_MASK, board[position] & VALUE_MASK);
			return;
		}
		
		list.add(x, y, 1, board[position] & VALUE_MASK);
		
		// Is empty, need to search adjacent squares
		// Left
		if (x > 0) {
			if ((board[position - 1] & HIDDEN_BIT) != 0)
				reveal(x - 1, y, list);
		}
		
		// Top Left
		if (x > 0 && y > 0) {
			if ((board[position - 1 - width] & HIDDEN_BIT) != 0)
				reveal(x - 1, y - 1, list);
		}
		
		// Up
		if (y > 0) {
			if ((board[position - width] & HIDDEN_BIT) != 0)
				reveal(x, y - 1, list);
		}
		
		// Top Right
		if (x < width - 1 && y > 0) {
			if ((board[position + 1 - width] & HIDDEN_BIT) != 0)
				reveal(x + 1, y - 1, list);
		}
		
		// Right
		if (x < width - 1) {
			if ((board[position + 1] & HIDDEN_BIT) != 0)
				reveal(x + 1, y, list);
		}
		
		// Right Bottom
		if (x < width - 1 && y < height - 1) {
			if ((board[position + 1 + width] & HIDDEN_BIT) != 0)
				reveal(x + 1, y + 1, list);
		}
		
		// Down
		if (y < height - 1) {
			if ((board[position + width] & HIDDEN_BIT) != 0)
				reveal(x, y + 1, list);
		}
		
		// Left Bottom
		if (x > 0 && y < height - 1) {
			if ((board[position - 1 + width] & HIDDEN_BIT) != 0)
				reveal(x - 1, y + 1, list);
		}
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
		
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				gc.setFill(array[j + i * width]);
				gc.fillRect(j * (tile + gap), i * (tile + gap), tile, tile);
				
				if ((board[j + i * width] & HIDDEN_BIT) == 0) {
					if ((board[j + i * width] & BOMB_BIT) != 0) {
						gc.setFill(Color.BLACK);
						gc.fillText("¤", j * (tile + gap) + (tile + gap) / 2, i * (tile + gap) + (tile + gap) / 2, tile);
					} else {
						if ((board[j + i * width] & VALUE_MASK) != 0) {
							int val = board[j + i * width] & VALUE_MASK;
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

	/**
	 * Affiche le plateau sans dévoiler les cases, tel qu'un utilisateur devrait le voir.
	 */
	public void display() {
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if ((board[j + i * width] & HIDDEN_BIT) != 0) {
					System.out.print('#'); // Hidden
				} else {
					if ((board[j + i * width] & BOMB_BIT) != 0) {
						System.out.print('X'); // Bomb
					} else {
						if ((board[j + i * width] & VALUE_MASK) == 0) {
							System.out.print(' '); // Empty
						} else {
							/* UNIX only
							int val = board[j + i * width] & VALUE_MASK;
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
							System.out.print((board[j + i * width] & VALUE_MASK) + "\u001B[0m");*/
							System.out.print((board[j + i * width] & VALUE_MASK)); // Number
						}
					}
				}
			}
			System.out.println();
		}
	}
	
	/**
	 * Affiche chaque byte du plateau.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < height; i++) {
			sb.append(Arrays.toString(line(i)));
			sb.append("\n");
		}
		return sb.toString();
	}
	
	private byte[] line(int ordinate) {
		int start = ordinate * width;
		return Arrays.copyOfRange(board, start, start + width);
	}
	
	public boolean validAbscissa(int abscissa) {
		return abscissa >= 0 && abscissa < width;
	}
	public boolean validOrdinate(int ordinate) {
		return ordinate >= 0 && ordinate < height;
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
	
	private static class MessageList {
		private String user;
		private ArrayList<String> list;
		
		public MessageList(String username) {
			list = new ArrayList<String>();
			user = username;
		}
		
		public void add(int x, int y, int points, int content) {
			list.add(Message.SQRD + ' ' + x + ' ' + y + ' ' + content + ' ' + points + ' ' + user);
		}
		
		public ArrayList<String> getList() {
			return list;
		}
	}
}
