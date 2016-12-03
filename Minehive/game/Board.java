package game;

import static game.Square.MAX_VALUE;
import static game.Square.MIN_VALUE;
import static java.lang.String.valueOf;

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

public class Board {
	public static final int WIDTH = 30;
	public static final int HEIGHT = 16;
	private static final int NB_BOMBS = 99;
	
	/* En cas d'implémentation future où ces données peuvent varier */
	public final int width = WIDTH;
	public final int height = HEIGHT;
	public final int nbMines = NB_BOMBS;

	/** Nombre total de cases découvertes, celles avec des mines incluses */
	public int totalVisible = 0;
	
	public static final int BOMB_VAL = -1;

	private static final byte VALUE_MASK = 	(byte) 0b00001111;
	
	private static final byte HIDDEN_BIT = 	(byte) 0b00100000;
	private static final byte REVEAL_MASK = (byte) 0b11011111;
	
	private static final byte BOMB_BIT = 	(byte) 0b01000000;
	private static final byte DEFUSE_BOMB = (byte) 0b10111111;
	
	private boolean first;
	
	/** Count revealed squares that are not mines */
	private int nbRevealed;
	
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
	 * Renvoie le numéro de la case, indépendamment des autres bits (mine, caché, etc.)
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
		return valueFrom(board[x + y * width]);
	}
	public int valueFrom(byte square) {
		if (isBombFrom(square)) {
			return -1;
		}
		return numberFrom(square);
	}

	/**
	 * Mettre à jour une case du tableau. La case mise à jour est révélée 
	 * Attention à bien affecter une case du tableau, et non une copie.
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
	
	/**
	 * Obtenir le contenu d'une case. 
	 * @param square
	 * @return Si cachée : X. Si mine : -1. Sinon, valeur.
	 */
	public synchronized String contentAt(int x, int y) {
		return contentFrom(board[x + y * width]);
	}
	public synchronized String contentFrom(byte square) {
		if (isHiddenFrom(square)) {
			return "X";
		} else if (isBombFrom(square)) {
			return "-1";
		} else {
			return String.valueOf(numberFrom(square));
		}
	}
	
	public synchronized void setVisible(int position) {
		board[position] &= REVEAL_MASK;
		totalVisible++;
	}
	
	public synchronized LinkedList<String> lineContentAt(int ordinate) {
		byte[] line = line(ordinate);
		LinkedList<String> res = new LinkedList<>();
		for (byte e : line) {
			res.add(contentFrom(e));
		}
		return res;
	}
	
	public void empty() {
		Arrays.fill(board, HIDDEN_BIT);
		nbRevealed = 0;
	}
	
	public void reset() {
		// Reset board
		empty();
		
		// Place new bombs
		initBombs();
		
		// Next player will be first
		first = true;
	}
	
	// Returns true if the game has ended
	public boolean isFinished() {
		return nbRevealed == board.length - nbMines;
	}
	
	private void initBombs() {
		Random rand = new Random();
		
		// Get number of bombs to place
		/*int bombs = 0;
		while (bombs <= 5) {
			bombs = rand.nextInt(board.length / 5);
		}*/
		int bombs = nbMines;
		
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
				// Place mine
				placeMine(position, x, y);

			}
		}
	}

	private void placeMine(int position, int x, int y) {
		// Put the mine on the board
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

	/** 
	 * Retourne les points du joueur à ajouter ou retirer lors du clic
	 * @return Une liste de tableau de Strings, chaque tableau correspondant à :
	 * x y valeur points username
	 */
	public List<String[]> clickAt(int x, int y, String user) {
		if (x < 0 || x >= width) {
			throw new ArrayIndexOutOfBoundsException("Abscisse invalide : " + x);
		}
		if (y < 0 || y >= height) {
			throw new ArrayIndexOutOfBoundsException("Ordonnée invalide : " + y);
		}
		int position = x + y * width;
		MessageList list = new MessageList(user);

		/* Si case déjà découverte, renvoie un message vide */
		if (!isHiddenAt(x, y)) {
			return null;
		}
		
		if ((board[position] & BOMB_BIT) != 0) { // It's a bomb !
			// If it's the first click of the game, remove the bomb
			if (first) {
				removeMine(x, y);
				first = false;
				return clickAt(x, y, user);
			}
			list.add(x, y, -1, Square.Points.MINE);
			setVisible(position);
			return list.getList();
			
		} else { // it's not a bomb
			first = false;
			reveal(x, y, list);
			return list.getList();
		}
	}
	
	private void removeMine(int x, int y) {
		int position = x + y * width;
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
		
		/* We need to add this mine elsewhere to have 
		 * the same amount of mines as nb_bombs */
		Random rand = new Random();
		
		int b_x;
		int b_y;
		int b_position;
		
		do {
			b_x = rand.nextInt(width);
			b_y = rand.nextInt(height);
			b_position = b_x + b_y * width;
		} while (position == b_position); // We can't put the mine at the same position it was removed
		
		// Place the new mine
		placeMine(b_position, b_x, b_y);
	}
	
	private void reveal(int x, int y, MessageList list) {
		int position = x + y * width;
		
		if ((board[position] & HIDDEN_BIT) == 0) { // Already visible
			return;
		}
		
		setVisible(position);
		
		// Check for current square value
		int value = valueFrom(board[position]);
		
		if (value > 0) {
			list.add(x, y, value, value);
			return;
		} else if (value == 0) {
			list.add(x, y, value, Square.Points.EMPTY);
		}
		
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
			setVisible(i);
		}
	}
	
	public void draw(GraphicsContext gc, Color[] colors, int tile, int gap) {
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.CENTER);
		gc.setFont(new Font(tile));
		
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				gc.setFill(colors[j + i * width]);
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
							/* UNIX only, colored output in terminal
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
	
	public int getTotalVisible() {
		return totalVisible;
	}
	public int getTotalSquares() {
		return width * height;
	}
	
	/** @return Pourcentage entier */
	public int getCompletion() {
		int completion = 0;
		try {
			completion = (getTotalVisible() * 100) / getTotalSquares();
		} catch (ArithmeticException e) {
			return 0;
		}
		return completion;
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
		private List<String[]> list = new ArrayList<>();
		
		public MessageList(String username) {
			user = username;
		}
		
		public void add(int x, int y, int content, int points) {
			list.add(new String[]{valueOf(x), valueOf(y), valueOf(content), valueOf(points), user});
		}
		
		public List<String[]> getList() {
			return list;
		}
	}

}
