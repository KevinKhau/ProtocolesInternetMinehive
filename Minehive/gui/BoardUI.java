package gui;

import java.util.Arrays;

import game.Board;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/* FUTURE Temps constants... */
public class BoardUI extends Pane {
	private static final Color HIDDEN = Color.GRAY;
	private static final Color PRESSED = Color.rgb(0, 0, 0, 0.4);
	private static final Color SELECTED = Color.rgb(255, 255, 255, 0.4);
	private static final Color DEFAULT_REVEALED = Color.GHOSTWHITE;
	
	private final Canvas board;
	private final Canvas ui;
	private GraphicsContext board_gc;
	private GraphicsContext ui_gc;

	private ClientApp app;
	private int tile_size;
	private int gap;
	private Color colors[];
	private Board game;
	private Flags flags;

	public BoardUI(ClientApp clientApp) {
		this.app = clientApp;
		game = new Board();
		game.empty();

		board = new Canvas(10, 10);
		ui = new Canvas(10, 10);

		flags = new Flags();

		getChildren().add(board);
		getChildren().add(ui);

		board_gc = board.getGraphicsContext2D();
		ui_gc = ui.getGraphicsContext2D();

		tile_size = 0;
		gap = 0;

		colors = new Color[game.height * game.width];
		Arrays.fill(colors, HIDDEN);

		initEvents();
	}

	protected void clearSelection() {
		ui_gc.clearRect(0, 0, ui.getWidth(), ui.getHeight());
	}

	protected void pressAt(int x, int y) {
		ui_gc.clearRect(0, 0, ui.getWidth(), ui.getHeight());

		if (!flags.exist(x, y)) {
			if (colors[x + y * game.width] == HIDDEN) {
				tmpColor(x, y, PRESSED);
			}
		}
	}

	protected void selectAt(int x, int y) {
		ui_gc.clearRect(0, 0, ui.getWidth(), ui.getHeight());
		if (colors[x + y * game.width] == HIDDEN) {
			tmpColor(x, y, SELECTED);
		}
	}
	
	/**
	 * Colorier temporairement une case
	 * @param x
	 * @param y
	 * @param color
	 */
	private void tmpColor(int x, int y, Color color) {
		ui_gc.setFill(color);
		ui_gc.fillRect(x * (tile_size + gap), y * (tile_size + gap), tile_size, tile_size);
	}
	
	/**
	 * Rendre permanente la couleur d'une case
	 * @param x
	 * @param y
	 * @param color
	 */
	private void permaColor(int x, int y, Color color) {
		colors[x + y * Board.WIDTH] = color;
	}

	protected void clickAt(int x, int y) {
		if (!flags.exist(x, y)) {
			ui_gc.clearRect(0, 0, ui.getWidth(), ui.getHeight());
			app.click(x, y);
			drawBoard();
		}
	}

	protected void placeFlag(int x, int y) {
		if (flags.exist(x, y)) {
			flags.remove(x, y);
		} else {
			flags.add(x, y);
		}

		drawBoard();
	}

	public void drawBoard() {
		board_gc.clearRect(0, 0, board.getWidth(), board.getHeight());

		game.draw(board_gc, colors, tile_size, gap);
		flags.draw(board_gc, tile_size, gap);
	}

	public void updateAt(int x, int y) {

	}

	@Override
	protected void layoutChildren() {
		final double x = snappedLeftInset();
		final double y = snappedTopInset();
		final double w = snapSize(getWidth()) - x - snappedRightInset();
		final double h = snapSize(getHeight()) - y - snappedBottomInset();

		board.setLayoutX(x);
		board.setLayoutY(y);
		board.setWidth(w);
		board.setHeight(h);

		ui.setLayoutX(x);
		ui.setLayoutY(y);
		ui.setWidth(w);
		ui.setHeight(h);

		int gap_hz = (int) (w / 400);
		int gap_vt = (int) (h / 200);

		if (gap_hz < gap_vt)
			gap = gap_hz;
		else
			gap = gap_vt;

		int previous = 0;
		for (int tile = 0; !((tile + gap) * game.height > h || (tile + gap) * game.width > w); tile++) {
			previous = tile;
		}

		tile_size = previous;

		drawBoard();
		ui_gc.clearRect(0, 0, ui.getWidth(), ui.getHeight());
	}

	private void initEvents() {
		ui.addEventHandler(MouseEvent.MOUSE_MOVED, 
				new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent t) {  
				int tx = (int) (t.getX() / (tile_size + gap));
				int ty = (int) (t.getY() / (tile_size + gap));

				if (tx >= 0 && tx < game.width && ty >= 0 && ty < game.height) {
					selectAt(tx, ty);
				} else {
					clearSelection();
				}
			}
		});

		ui.addEventHandler(MouseEvent.MOUSE_DRAGGED, 
				new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent t) {  
				int tx = (int) (t.getX() / (tile_size + gap));
				int ty = (int) (t.getY() / (tile_size + gap));

				if (tx >= 0 && tx < game.width && ty >= 0 && ty < game.height) {
					pressAt(tx, ty);
				} else {
					clearSelection();
				}
			}
		});

		ui.addEventHandler(MouseEvent.MOUSE_RELEASED, 
				new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent t) {
				int tx = (int) (t.getX() / (tile_size + gap));
				int ty = (int) (t.getY() / (tile_size + gap));

				//
				switch (t.getButton()) {
				case PRIMARY:
					if (tx >= 0 && tx < game.width && ty >= 0 && ty < game.height) {
						clickAt(tx, ty);
					} else {
						clearSelection();
					}
					break;

				case SECONDARY:
					if (tx >= 0 && tx < game.width && ty >= 0 && ty < game.height) {
						placeFlag(tx, ty);
					}
					break;

				default:
				}

			}
		});

		ui.addEventHandler(MouseEvent.MOUSE_PRESSED, 
				new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent t) {            
				int tx = (int) (t.getX() / (tile_size + gap));
				int ty = (int) (t.getY() / (tile_size + gap));

				if (tx >= 0 && tx < game.width && ty >= 0 && ty < game.height) {
					pressAt(tx, ty);
				} else {
					clearSelection();
				}
			}
		});
	}
	
	public void revealSquare(int x, int y, int content, Color color) {
		if (color == null) {
			permaColor(x, y, DEFAULT_REVEALED);
		} else {
			permaColor(x, y, color);
		}
		game.updateValueAt(x, y, content);
		drawBoard();
	}
	
	/**
	 * Mettre à jour toute une ligne du plateau.
	 * 
	 * @param lineNumber
	 * @param contents
	 *            Les valeurs de chaque ordonnée de la ligne révélée. Si
	 *            l'argument n'est pas un entier, alors la case n'est pas à
	 *            révéler.
	 */
	public void revealLine(int lineNumber, String[] contents) {
		for (int x = 0; x < contents.length; x++) {
			try {
				revealSquare(x, lineNumber, Integer.parseInt(contents[x]), null);
			} catch (NumberFormatException e) {
				// Le contenu n'est pas un entier, donc probablement X : case cachée !
			}
		}
	}
}

