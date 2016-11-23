package gui;

import java.util.ArrayList;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class Flags {
	private ArrayList<Coordinates> list;

	public Flags() {
		list = new ArrayList<Coordinates>();
	}

	public void reset() {
		list.clear();
	}

	public void add(int x, int y) {
		if (!this.exist(x, y)) {
			list.add(new Coordinates(x, y));
		}
	}

	public boolean exist(int x, int y) {
		return list.contains(new Coordinates(x, y));
	}

	public void remove(int x, int y) {
		list.remove(new Coordinates(x, y));
	}

	public void draw(GraphicsContext gc, int tile, int gap) {
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.CENTER);
		gc.setFont(new Font(tile));
		gc.setFill(Color.DARKRED);

		for (int i = 0; i < list.size(); i++) {
			gc.fillText("¶", list.get(i).x * (tile + gap) + (tile + gap) / 2, list.get(i).y * (tile + gap) + (tile + gap) / 2, tile);
		}
	}

	static private class Coordinates {
		public int x, y;

		public Coordinates(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj instanceof Coordinates) {
				Coordinates c = (Coordinates) obj;
				if (c.x == x && c.y == y)
					return true;
			}
			
			return false;
		}
	}

	/*
	public static void main (String[] args) {
		Flags flags = new Flags();

		flags.add(1, 1);
		flags.add(2, 9);

		System.out.println(flags.exist(1, 1));
		System.out.println(flags.exist(2, 5));
	}
	*/
} 
