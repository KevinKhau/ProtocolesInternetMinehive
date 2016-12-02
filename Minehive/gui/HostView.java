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

public class HostView extends BorderPane {
	private ClientApp app;
	private ObservableList<UIInGamePlayer> gameMembers;

	BoardUI board;
	
	public HostView(ClientApp clientApp) {
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

		TableView<UIInGamePlayer> table = new TableView<UIInGamePlayer>();
		table.setRowFactory(row -> new TableRow<UIInGamePlayer>(){
			@Override
			public void updateItem(UIInGamePlayer item, boolean empty){
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

		TableColumn<UIInGamePlayer, String> nameCol = new TableColumn<>("Player");
		TableColumn<UIInGamePlayer, Integer> IGPointsCol = new TableColumn<>("In-game Points");
		TableColumn<UIInGamePlayer, Integer> totalPointsCol = new TableColumn<>("Total Points");
		TableColumn<UIInGamePlayer, Integer> safeCol = new TableColumn<>("Safe Squares");
		TableColumn<UIInGamePlayer, Integer> minesCol = new TableColumn<>("Mines");
		nameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
		IGPointsCol.setCellValueFactory(new PropertyValueFactory<>("inGamePoints"));
		totalPointsCol.setCellValueFactory(new PropertyValueFactory<>("totalPoints"));
		safeCol.setCellValueFactory(new PropertyValueFactory<>("safeSquares"));
		minesCol.setCellValueFactory(new PropertyValueFactory<>("foundMines"));

		table.getColumns().add(nameCol);
		table.getColumns().add(IGPointsCol);
		table.getColumns().add(totalPointsCol);
		table.getColumns().add(safeCol);
		table.getColumns().add(minesCol);
		
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		table.resizeColumn(IGPointsCol, TableView.USE_COMPUTED_SIZE);
		
		gameMembers = getGameMembers(); //TEST delete
		table.setItems(gameMembers);

		this.setRight(table);
	}

	private ObservableList<UIInGamePlayer> getGameMembers() { // TODO
		ArrayList<UIInGamePlayer> l = new ArrayList<UIInGamePlayer>();
		l.add(new UIInGamePlayer("Tomek", 2, 20, 0, 0));
		l.add(new UIInGamePlayer("Kevin", 2, 20, 0, 0));
		l.add(new UIInGamePlayer("Machin", 2, 20, 0, 0));
		l.add(new UIInGamePlayer("Truc", 2, 20, 0, 0));
		ObservableList<UIInGamePlayer> list = FXCollections.observableList(l);
		return list;
	}

	public void revealSquare(int x, int y, int content, int points) {
		board.revealSquare(x, y, content);
		// TODO update scores
	}
	
}
