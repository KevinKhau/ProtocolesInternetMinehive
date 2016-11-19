package gui;

import java.util.Arrays;

import game.Board;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import network.Client;

public class BoardUI extends Pane {
	private static final Color HIDDEN = Color.GRAY;
	private static final Color PRESSED = Color.rgb(0, 0, 0, 0.4);
	private static final Color SELECTED = Color.rgb(255, 255, 255, 0.4);
	private static final Color TEST = Color.WHITE;

	private final Canvas board;
	private final Canvas ui;
	private GraphicsContext board_gc;
	private GraphicsContext ui_gc;

	private Client client;
	private int tile_size;
	private int gap;
	private Color array[];
	private Board game;
	private Flags flags;

	public BoardUI(Client client) {
		this.client = client;
		game = new Board();
		game.reset();

		board = new Canvas(10, 10);
		ui = new Canvas(10, 10);

		flags = new Flags();

		getChildren().add(board);
		getChildren().add(ui);

		board_gc = board.getGraphicsContext2D();
		ui_gc = ui.getGraphicsContext2D();

		tile_size = 0;
		gap = 0;

		array = new Color[game.height * game.width];
		Arrays.fill(array, HIDDEN);

		initEvents();
	}

	protected void clearSelection() {
		ui_gc.clearRect(0, 0, ui.getWidth(), ui.getHeight());
	}

	protected void pressAt(int x, int y) {
		ui_gc.clearRect(0, 0, ui.getWidth(), ui.getHeight());

		if (!flags.exist(x, y)) {
			if (array[x + y * game.width] == HIDDEN) {
				ui_gc.setFill(PRESSED);
				ui_gc.fillRect(x * (tile_size + gap), y * (tile_size + gap), tile_size, tile_size);
			}
		}
	}

	protected void selectAt(int x, int y) {
		ui_gc.clearRect(0, 0, ui.getWidth(), ui.getHeight());
		if (array[x + y * game.width] == HIDDEN) {
			ui_gc.setFill(SELECTED);
			ui_gc.fillRect(x * (tile_size + gap), y * (tile_size + gap), tile_size, tile_size);
		}
	}

	protected void revealAt(int x, int y) {
		if (!flags.exist(x, y)) {
			ui_gc.clearRect(0, 0, ui.getWidth(), ui.getHeight());
			//array[x + y * Board.WIDTH] = test;
			//redrawAt(x, y, test);
			//game.clickAt(x, y);
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

		game.draw(board_gc, array, tile_size, gap);
		flags.draw(board_gc, tile_size, gap);
	}

	public void updateAt(int x, int y) {

	}

	private void redrawAt(int x, int y, Color color) {
		board_gc.setFill(color);
		board_gc.fillRect(x * (tile_size + gap), y * (tile_size + gap), tile_size, tile_size);
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
						revealAt(tx, ty);
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
}

