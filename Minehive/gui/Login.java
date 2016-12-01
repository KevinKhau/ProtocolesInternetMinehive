package gui;

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
import network.ClientController;

public class Login extends GridPane {
	private ClientApp app;
	private ClientController controller;
	private String err;
	private TextField username;
	private PasswordField password;
	private TextField inetAddress;
	
	public Login(ClientApp clientApp) {
		this.app = clientApp;
		
		err = "";

		this.setAlignment(Pos.CENTER);
		this.setVgap(4);

		Text title = new Text("Connection to Minehive Server");
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
				if (!emptyFields()) {
					// Switch ClientApp scene to ServerApp
					app.joinServer(inetAddress.getText(), 5555, username.getText(), password.getText());
				}
			}
		});
	}
	
	private boolean emptyFields() {
		if (inetAddress.getText().trim().isEmpty()) {
			Dialog.warning("IP address", "IP adress missing", "Please enter an IP address");
			return true;
		}
		if (username.getText().trim().isEmpty()) {
			Dialog.warning("Username", "Username missing", "Please enter your name");
			return true;
		}
		if (password.getText().trim().isEmpty()) {
			Dialog.warning("Password", "Password missing", "Please enter your password");
			return true;
		}
		return false;
	}

}
