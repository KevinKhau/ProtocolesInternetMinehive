package gui;

import java.net.Socket;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import network.Client;

public class ClientApp extends Application {
	private Stage primaryStage;
	private ClientModel client;
	//private BorderPane rootLayout;
	
	public static void main(String[] args) {
		launch(args);
	}
	
	private void initApp() {		
		primaryStage.setTitle("Login - Minehive");
		primaryStage.setMinWidth(700);
		primaryStage.setMinHeight(350);
		primaryStage.setWidth(800);
		primaryStage.setHeight(600);
		
		Login login = new Login(client);
		Scene scene = new Scene(login);
		primaryStage.setScene(scene);		
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		initApp();		
		primaryStage.show();
	}
}
