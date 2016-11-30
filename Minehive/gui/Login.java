package gui;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import network.Client;
import util.TFSocket;

public class Login extends GridPane {
	private ClientModel client;
	private String err;
	private TextField username;
	private PasswordField password;
	private TextField inetAddress;
	
	private TFSocket socket;

	public Login(ClientModel client) {
		this.client = client;

		err = "";

		this.setAlignment(Pos.CENTER);
		this.setVgap(4);

		Text title = new Text("Title");
		title.setFont(Font.font(20));
		this.add(title, 0, 0);
		GridPane.setHalignment(title, HPos.CENTER);

		inetAddress = new TextField();
		inetAddress.setPrefWidth(300);
		inetAddress.setPromptText("IP Address");
		inetAddress.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
		this.add(inetAddress, 0, 1);

		username = new TextField();
		username.setPromptText("User");
		username.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
		this.add(username, 0, 2);

		password = new PasswordField();
		password.setPromptText("Password");
		password.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
		this.add(password, 0, 3);

		Button button = new Button("Server Login");
		button.setPrefWidth(300);
		this.add(button, 0, 5);

		inetAddress.setText("localhost"); // TEST
		username.setText("Kevin");
		password.setText("Khau");

		button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				if (inetAddress.getText().trim().isEmpty()) {
					Dialog.warning("IP address", "IP adress missing", "Please enter an IP address");
					return;
				}
				if (username.getText().trim().isEmpty()) {
					Dialog.warning("Username", "Username missing", "Please enter your name");
					return;
				}
				if (password.getText().trim().isEmpty()) {
					Dialog.warning("Password", "Password missing", "Please enter your password");
					return;
				}
				if (isValidLogin() ) {
					// Switch ClientApp scene to ServerApp
					Stage app = (Stage) ((Node) e.getSource()).getScene().getWindow();
					Loading loading = new Loading(client);
					Scene scene = new Scene(loading.getUI());
					app.setScene(scene);
					app.show();
				} else {
					// Display error dialog
					Dialog.error("Login Error", "Couldn't login properly.", err);
				}
			}
		});
	}

	protected boolean isValidLogin() {
		// if (client.login(username.getText(), password.getText())) {
		if (username.getText().trim().equals("Tomek") && password.getText().trim().equals("password")
				&& inetAddress.getText().trim().equals("localhost")) {
			return true;
		} else {
			if (!inetAddress.getText().trim().equals("localhost")) {
				err = "Could not reach server.";
			} else {
				err = "Bad password";
			}
			return false;
		}
	}

	private boolean connect() {
		InetAddress IP = null;
		try {
			IP = InetAddress.getByName(inetAddress.getText());
		} catch (UnknownHostException e) {
			Dialog.error("Connection Error", "IP introuvable.", err);
			return false;
		}
		try {
			socket = new TFSocket(IP, 5555);
		} catch (IOException e) {
			Dialog.error("Connection Error", "Serveur injoignable.", err);
			return false;
		}
		return true;
	}
	
}