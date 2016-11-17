package gui;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import network.Client;
import util.ColorUtils;

public class Game {
	private BorderPane layout;
	private Client client;
	private ObservableList<Player> gameMembers;

	public Game(Client client) {
		this.client = client;
		layout = new BorderPane();
		
		BoardUI canvaspane = new BoardUI(client);
		canvaspane.setPadding(new Insets(0, 5, 10, 10));
		layout.setCenter(canvaspane);
		
		HBox hbox = new HBox();
		hbox.setPadding(new Insets(15, 12, 15, 12));
		hbox.setSpacing(10);
		Label points_text = new Label("Points: ");
		points_text.setFont(new Font(20));
		Label points = new Label("0");
		points.setFont(new Font(20));
	    hbox.getChildren().addAll(points_text, points);
		layout.setTop(hbox);

		TableView<Player> table = new TableView<Player>();
		table.setRowFactory(row -> new TableRow<Player>(){
			@Override
			public void updateItem(Player item, boolean empty){
				super.updateItem(item, empty);
				setMouseTransparent(true);

				if (!(item == null || empty)) {
					if (item.getColor() != null) {
						setStyle("-fx-background-color: " + ColorUtils.colorToCSS(item.getColor()));
						setTextFill(Color.RED);        
					}
				}
			}
		});

		gameMembers = getGameMembers();
		table.setItems(gameMembers);

		TableColumn<Player, String> nameCol = new TableColumn<Player, String>("Player");
		nameCol.setCellValueFactory(new PropertyValueFactory("name"));
		TableColumn<Player, String> pointsCol = new TableColumn<Player, String>("Points");
		pointsCol.setCellValueFactory(new PropertyValueFactory("points"));

		table.getColumns().setAll(nameCol, pointsCol);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		table.resizeColumn(pointsCol, TableView.USE_COMPUTED_SIZE);
		layout.setRight(table);
	}

	private ObservableList<Player> getGameMembers() {
		ArrayList<Player> l = new ArrayList<Player>();
		l.add(new Player("Tomek", Color.CHARTREUSE));
		l.add(new Player("Kevin", Color.VIOLET));
		l.add(new Player("Machin", null));
		l.add(new Player("Truc", Color.TURQUOISE));
		ObservableList<Player> list = FXCollections.observableList(l);
		return list;
	}

	public Parent getUI() {
		return layout;
	}
}