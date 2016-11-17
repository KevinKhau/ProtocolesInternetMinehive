package gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import network.Client;

public class Login {
	private GridPane layout;
	private Client client;
	private String err;
	private TextField username;
	private PasswordField password;
	private TextField inetAddress;
	
	public Login(Client client) {
		this.client = client;
		
		err = "";
		
		layout = new GridPane();
		layout.setAlignment(Pos.CENTER);
		layout.setVgap(4);
		
		Text title = new Text("Title");
		title.setFont(Font.font(20));
		layout.add(title, 0, 0);
		GridPane.setHalignment(title, HPos.CENTER);

        inetAddress = new TextField();
        inetAddress.setPrefWidth(300);
        inetAddress.setPromptText("IP Address");
        inetAddress.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
        layout.add(inetAddress, 0, 1);

        username = new TextField();
        username.setPromptText("User");
        username.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");

        layout.add(username, 0, 2);
		
		password = new PasswordField();
		password.setPromptText("Password");
		password.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
		layout.add(password, 0, 3);
		
		Button button = new Button("Login");
		button.setPrefWidth(300);
		layout.add(button, 0, 5);
		
		button.setOnAction(new EventHandler<ActionEvent>() {

		    @Override
		    public void handle(ActionEvent e) {/*
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
		    	if (isValidLogin()) {*/
		    		// Change ClientApp scene to Game
		    	    Stage app = (Stage) ((Node) e.getSource()).getScene().getWindow();
		    	    Loading loading = new Loading(client);
		    	    Scene scene = new Scene(loading.getUI());
		    	    app.setScene(scene);
		    	    app.show();/*
		    	} else {
		    	    // Display error dialog
		    		Dialog.error("Login Error", "Couldn't login properly", err);
		    	}*/
		    }
		});
	}
	
	protected boolean isValidLogin() {
		//if (client.login(username.getText(), password.getText())) {
		if (username.getText().trim().equals("Tomek") && password.getText().trim().equals("password") && inetAddress.getText().trim().equals("localhost")) {
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

	public Parent getUI() {
		return layout;
	}
}
