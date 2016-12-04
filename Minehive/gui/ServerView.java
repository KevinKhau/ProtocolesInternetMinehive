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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import util.Message;
import util.StringUtil;

public class ServerView extends BorderPane {

	private ClientApp app;

	TableView<UIDetailedHostData> hosts = new TableView<>();
	TableView<UIUser> users = new TableView<>();
	Map<String, UIUser> usersHelper = new ConcurrentHashMap<>();

	private HBox hbox;
	private Button join;

	public ServerView(ClientApp app) {
		this.app = app;

		hbox = new HBox();
		hbox.setSpacing(10);
		hbox.setPadding(new Insets(10, 10, 10, 10));

		initHosts();
		initUsers();

		this.setCenter(hbox);
		GridPane.setFillWidth(hbox, true);
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

		hosts.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

		//hosts.setPrefSize( Double.MAX_VALUE, Double.MAX_VALUE );
		//hosts.setPrefWidth(Double.MAX_VALUE);

		final HBox buttons = new HBox();
		//buttons.setPadding(new Insets(0, 10, 0, 0));

		final Button createMatch = new Button("New Match");
		createMatch.setOnAction(e -> {
			app.createMatch();
		});

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		Button refresh = new Button("Refresh matches");
		refresh.setOnAction(e -> {
			app.listMatches();
		});

		join = new Button("Join");
		join.setPrefSize(70, USE_PREF_SIZE);
		join.setOnAction(e -> {
			UIDetailedHostData h = hosts.getSelectionModel().getSelectedItem();
			if (h != null) {
				app.joinHost(h.getIP(), h.getPort());
			} else {
				Dialog.warning("No match selected", "Cannot join match...", "Please select a match");
			}
		});

		buttons.getChildren().addAll(createMatch, refresh, spacer, join);
		buttons.setSpacing(5);

		final VBox hostsBox = new VBox();
		hostsBox.setAlignment(Pos.CENTER);
		hostsBox.setSpacing(5);
		hostsBox.getChildren().addAll(label, hosts, buttons);
		hostsBox.setFillWidth(true);
		VBox.setVgrow(hosts, Priority.ALWAYS);

		hbox.getChildren().addAll(hostsBox);
		HBox.setHgrow(hostsBox, Priority.ALWAYS);
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

		users.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

		Button refresh = new Button("Refresh users");
		refresh.setOnAction(e -> {
			app.listUsers();
			app.listAvailable();
		});

		final VBox usersBox = new VBox();
		usersBox.setAlignment(Pos.CENTER);
		usersBox.setSpacing(5);
		usersBox.getChildren().addAll(label, users, refresh);
		usersBox.setFillWidth(true);
		VBox.setVgrow(users, Priority.ALWAYS);

		hbox.getChildren().addAll(usersBox);
		HBox.setHgrow(usersBox, Priority.ALWAYS);
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
		String IP = StringUtil.truncateAddress(msg.getArg(0));
		UIDetailedHostData dhd = new UIDetailedHostData(msg.getArg(2), IP, msg.getArgAsInt(1), msg.getArgAsInt(3), p);
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
