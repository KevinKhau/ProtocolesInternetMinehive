package gui;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import util.ColorUtils;

public class Game extends BorderPane {
	private ClientApp app;
	private ObservableList<Player> gameMembers;

	BoardUI board;
	
	public Game(ClientApp clientApp) {
		this.app = clientApp;
		
		board = new BoardUI(clientApp);
		board.setPadding(new Insets(0, 5, 10, 10));
		this.setCenter(board);
		
		HBox hbox = new HBox();
		hbox.setPadding(new Insets(15, 12, 15, 12));
		hbox.setSpacing(10);
		Label points_text = new Label("Points: ");
		points_text.setFont(new Font(20));
		Label points = new Label("0");
		points.setFont(new Font(20));
	    hbox.getChildren().addAll(points_text, points);
		this.setTop(hbox);

		TableView<Player> table = new TableView<Player>();
		table.setRowFactory(row -> new TableRow<Player>(){
			@Override
			public void updateItem(Player item, boolean empty){
				super.updateItem(item, empty);
				setMouseTransparent(true);

				if (!(item == null || empty)) {
					if (item.getColor() != null) {
						setStyle("-fx-background-color: " + ColorUtils.colorToCSS(item.getColor()));
					} else {
						setStyle("-fx-background-color: inherit");
					}
				}
			}
		});

		gameMembers = getGameMembers();
		table.setItems(gameMembers);

		TableColumn<Player, String> nameCol = new TableColumn<Player, String>("Player");
		nameCol.setCellValueFactory(new PropertyValueFactory<Player, String>("name"));
		TableColumn<Player, String> pointsCol = new TableColumn<Player, String>("Points");
		pointsCol.setCellValueFactory(new PropertyValueFactory<Player, String>("points"));

		table.getColumns().setAll(nameCol, pointsCol);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		table.resizeColumn(pointsCol, TableView.USE_COMPUTED_SIZE);
		this.setRight(table);
	}

	private ObservableList<Player> getGameMembers() { // TODO
		ArrayList<Player> l = new ArrayList<Player>();
		l.add(new Player("Tomek", Color.CHARTREUSE));
		l.add(new Player("Kevin", Color.VIOLET));
		l.add(new Player("Machin", null));
		l.add(new Player("Truc", Color.TURQUOISE));
		ObservableList<Player> list = FXCollections.observableList(l);
		return list;
	}

	public void revealSquare(int x, int y, int content, int points) {
		board.revealSquare(x, y, content);
		// TODO update scores
	}
	
}
