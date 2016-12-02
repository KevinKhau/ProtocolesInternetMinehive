package gui;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import util.Message;

public class ServerView extends BorderPane {

	private ClientApp app;

	TableView<UIDetailedHostData> hosts = new TableView<>();
	TableView<UIUser> users = new TableView<>();
	Map<String, UIUser> usersHelper = new ConcurrentHashMap<>();
	
	public ServerView(ClientApp app) {
		this.app = app;
		initTopButtons();
		initHosts();
		initUsers();
	}
	
	private void initTopButtons() {
		final Button createMatch = new Button("New Match");
		createMatch.setOnAction(e -> {
			app.createMatch();
		});
		
		final HBox buttons = new HBox();
		buttons.setPadding(new Insets(0, 10, 10, 0));
		
		buttons.getChildren().addAll(createMatch);
		
		this.setTop(buttons);
	}
	
	private void initHosts() {
		final Label label = new Label("On-going matches");
		label.setFont(new Font("Arial", 20));

		TableColumn<UIDetailedHostData, String> nameColumn = new TableColumn<>("Name");
		TableColumn<UIDetailedHostData, Integer> completionColumn = new TableColumn<>("Completion");
		TableColumn<UIDetailedHostData, Integer> playersColumn = new TableColumn<>("Players");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
		completionColumn.setCellValueFactory(new PropertyValueFactory<>("completion"));
		playersColumn.setCellValueFactory(new PropertyValueFactory<>("Players"));
		hosts.getColumns().add(nameColumn);
		hosts.getColumns().add(completionColumn);
		hosts.getColumns().add(playersColumn);
		hosts.setRowFactory(row -> new TableRow<UIDetailedHostData>() {
			@Override
			public void updateItem(UIDetailedHostData item, boolean empty){
				super.updateItem(item, empty);
				if (!(item == null || empty)) {
					setOnMousePressed(e -> {
						if (e.getButton() == MouseButton.PRIMARY) {
							if (e.getClickCount() == 2) {
								app.joinHost(item.getIP(), item.getPort());
							} else {
								//TODO Description dans panel central
								System.out.println(item.getName() + " : " + item.getIP() + "/" + item.getPort());
							}
						}
					});
				}
			}
		});
		
		Button refresh = new Button("Refresh matches");
		refresh.setOnAction(e -> {
			app.listMatches();
		});
		
		final VBox hostsBox = new VBox();
		hostsBox.setAlignment(Pos.CENTER);
		hostsBox.setSpacing(5);
		hostsBox.setPadding(new Insets(10, 0, 0, 10));
		hostsBox.getChildren().addAll(label, hosts, refresh);
		
		this.setLeft(hostsBox);
	}
	
	private void initUsers() {
		final Label label = new Label("Users");
		label.setFont(new Font("Arial", 20));
		
		TableColumn<UIUser, String> nameColumn = new TableColumn<>("Name");
		TableColumn<UIUser, Integer> pointsColumn = new TableColumn<>("Points");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
		pointsColumn.setCellValueFactory(new PropertyValueFactory<>("Points"));
		users.getColumns().add(nameColumn);
		users.getColumns().add(pointsColumn);
		users.setRowFactory(row -> new TableRow<UIUser>(){
			@Override
			public void updateItem(UIUser item, boolean empty){
				super.updateItem(item, empty);
				setEditable(false);
				if (!(item == null || empty)) {
					if (item.isOnline()) {
						setStyle("-fx-background-color:lightcoral");
					} else {
						setStyle("-fx-background-color:inherit");
					}
				}
			}
		});
		
		Button refresh = new Button("Refresh users");
		refresh.setOnAction(e -> {
			app.listUsers();
			app.listAvailable();
		});
		
		final VBox usersBox = new VBox();
		usersBox.setAlignment(Pos.CENTER);
		usersBox.setSpacing(2);
		usersBox.setPadding(new Insets(10, 0, 0, 10));
		usersBox.getChildren().addAll(label, users, refresh);
		
		this.setRight(usersBox);
	}
	
	public void activate() {
		app.listMatches();
		app.listUsers();
		app.listAvailable();
	}
	
	public void clearHosts() {
		hosts.getItems().clear();
	}
	
	public void addHost(Message msg) {
		Map<String, Integer> p = new HashMap<>();
		for (int i = 4; i < msg.getArgs().length; i += 2) {
			p.put(msg.getArg(i), msg.getArgAsInt(i + 1));
		}
		UIDetailedHostData dhd = new UIDetailedHostData(msg.getArg(2), msg.getArg(0), msg.getArgAsInt(1), msg.getArgAsInt(3), p);
		hosts.getItems().add(dhd);
	}
	
	public void clearUsers() {
		users.getItems().clear();
	}
	
	public void addUser(Message msg) {
		UIUser uu = new UIUser(msg.getArg(0), msg.getArgAsInt(1));
		users.getItems().add(uu);
		usersHelper.put(uu.getName(), uu);
	}
	
	public void addAvailable(Message msg) {
		UIUser uu = usersHelper.get(msg.getArg(0));
		if (uu == null) {
			addUser(msg);
		} else {
			uu.setPoints(msg.getArgAsInt(1));
			uu.setOnline(true);
		}
	}


}
