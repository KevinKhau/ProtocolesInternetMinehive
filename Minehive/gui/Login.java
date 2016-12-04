package gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class Login extends GridPane {
	private ClientApp app;
	private TextField username;
	private PasswordField password;
	private TextField inetAddress;
	private TextField port;
	private GridPane pane;
	private Button button;
	private CheckBox checkbox;
	
	public Login(ClientApp clientApp) {
		this.app = clientApp;
		
		port = new TextField();
		inetAddress = new TextField();
		username = new TextField();
		button = new Button("Server Login");
		password = new PasswordField();
		checkbox = new CheckBox("Connect to Host directly");
		
		init();
		initEvent();
		
		// TODO remove
		username.setText("Tomek");
		password.setText("Lecocq");
	}
	
	private void init() {
		this.setAlignment(Pos.CENTER);
		this.setVgap(4);

		Text title = new Text("Connection to Minehive Server");
		title.setFont(Font.font(20));
		this.add(title, 0, 0);
		GridPane.setHalignment(title, HPos.CENTER);
		
		pane = new GridPane();
		pane.setHgap(4);
		
		inetAddress.setMinWidth(226);
		inetAddress.setMaxWidth(226);
		inetAddress.setPromptText("IP Address");
		inetAddress.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
		pane.add(inetAddress, 0, 0);
		
		port.setMinWidth(70);
		port.setMaxWidth(70);
		port.setPromptText("Port");
		port.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
		pane.add(port, 1, 0);
		
		this.add(pane, 0, 1);

		username.setPromptText("User");
		username.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
		username.setMinWidth(300);
		username.setMaxWidth(300);
		this.add(username, 0, 2);

		password.setPromptText("Password");
		password.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
		password.setMinWidth(300);
		password.setMaxWidth(300);
		this.add(password, 0, 3);
		
		checkbox.setIndeterminate(false);
		checkbox.setSelected(false);
		this.add(checkbox, 0, 5);

		button.setMinWidth(300);
		button.setMaxWidth(300);
		this.add(button, 0, 7);

		inetAddress.setText("localhost");
		port.setText("5555");
	}
	
	private void initEvent() {
		button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				connect();
			}
		});
		
		button.setOnKeyPressed(new EventHandler<KeyEvent>() {
	        @Override
	        public void handle(KeyEvent key) {
	            if (key.getCode() == KeyCode.ENTER) {
	            	connect();
	            }
	        }
	    });
		
		inetAddress.setOnKeyPressed(new EventHandler<KeyEvent>() {
	        @Override
	        public void handle(KeyEvent key) {
	            if (key.getCode() == KeyCode.ENTER) {
	            	connect();
	            }
	        }
	    });
		
		port.setOnKeyPressed(new EventHandler<KeyEvent>() {
	        @Override
	        public void handle(KeyEvent key) {
	            if (key.getCode() == KeyCode.ENTER) {
	            	connect();
	            }
	        }
	    });
		
		password.setOnKeyPressed(new EventHandler<KeyEvent>() {
	        @Override
	        public void handle(KeyEvent key) {
	            if (key.getCode() == KeyCode.ENTER) {
	            	connect();
	            }
	        }
	    });
		
		username.setOnKeyPressed(new EventHandler<KeyEvent>() {
	        @Override
	        public void handle(KeyEvent key) {
	            if (key.getCode() == KeyCode.ENTER) {
	            	connect();
	            }
	        }
	    });
		
		checkbox.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				if (checkbox.isSelected()) {
					// Change Port to host port
					port.setText("7777");
				} else {
					port.setText("5555");
				}
			}
		});
		
		checkbox.setOnKeyPressed(new EventHandler<KeyEvent>() {
	        @Override
	        public void handle(KeyEvent key) {
	            if (key.getCode() == KeyCode.ENTER) {
	            	connect();
	            }
	        }
	    });
	}

	private void connect() {
		if (!emptyFields()) {
			// Switch ClientApp scene to ServerApp
			if (checkbox.isSelected())
				app.joinHost(inetAddress.getText(), Integer.parseInt(port.getText()), username.getText(), password.getText());
			else
				app.joinServer(inetAddress.getText(), Integer.parseInt(port.getText()), username.getText(), password.getText());
		}
	}
	
	private boolean emptyFields() {
		if (inetAddress.getText().trim().isEmpty()) {
			Dialog.warning("IP address", "IP adress missing", "Please enter the server IP address");
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
		if (port.getText().trim().isEmpty()) {
			Dialog.warning("Port", "Port number missing", "Please enter the server port");
			return true;
		}
		return false;
	}

}
