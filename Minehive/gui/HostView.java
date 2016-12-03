package gui;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import util.ColorUtils;
import util.Params;

public class HostView extends BorderPane {
	private ClientApp app;

	TableView<UIInGamePlayer> players = new TableView<>();
	Map<String, UIInGamePlayer> playersHelper = new ConcurrentHashMap<>();

	BoardUI board;
	Clip clip;

	public HostView(ClientApp clientApp) {
		this.app = clientApp;
		try {
			clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(Params.MINE_EXPLOSION.toFile()));
			clip.setFramePosition(clip.getFrameLength());
		} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
			Dialog.exception(e, e.getMessage());
		}
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

		players = new TableView<UIInGamePlayer>();
		players.setRowFactory(row -> new TableRow<UIInGamePlayer>(){
			@Override
			public void updateItem(UIInGamePlayer item, boolean empty){
				super.updateItem(item, empty);
				setMouseTransparent(true);

				if (!(item == null || empty)) {
					if (item.active) {
						// TODO démarquation si joueurs actifs/inactifs
					} else {
					}
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

		players.getColumns().add(nameCol);
		players.getColumns().add(IGPointsCol);
		players.getColumns().add(totalPointsCol);
		players.getColumns().add(safeCol);
		players.getColumns().add(minesCol);

		players.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		players.resizeColumn(IGPointsCol, TableView.USE_COMPUTED_SIZE);

		this.setRight(players);
	}

	/**
	 * Ajoute un nouveau joueur si première connexion, ou remet à actif si
	 * reconnexion.
	 */
	public void addPlayer(String username, int inGamePoints, int totalPoints, int safeSquares, int foundMines) {
		UIInGamePlayer p = playersHelper.get(username);
		if (p == null) {
			UIInGamePlayer igp = new UIInGamePlayer(username, inGamePoints, totalPoints, safeSquares, foundMines);
			players.getItems().add(igp);
			playersHelper.put(igp.getUsername(), igp);
		} else {
			p.setActive();
		}
	}

	public void setInactive(String username) {
		UIInGamePlayer p = playersHelper.get(username);
		if (p != null) {
			p.setInactive();
		}
	}

	public void revealSquare(int x, int y, int content, int points, String username) {
		UIInGamePlayer p = playersHelper.get(username);
		if (p == null) {
			System.err.println("Joueur " + username + " introuvable.");
			board.revealSquare(x, y, content, null);
			return;
		}
		p.incInGamePoints(points);
		p.incTotalPoints(points);
		if (content < 0) {
			p.incFoundMines(1);
			if (username.equals(app.username)) {
				clip.loop(1);
			}
		} else {
			p.incSafeSquares(1);
		}
		board.revealSquare(x, y, content, p.getColor());
	}

}
