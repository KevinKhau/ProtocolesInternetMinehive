package gui;

import java.io.FileNotFoundException;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import network.Client;

public class ClientApp extends Application {
	private Stage primaryStage;
	private Client client;
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
		
		//rootLayout = new BorderPane();
		//rootLayout.setBackground(new Background(new BackgroundFill(Color.BLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		//rootLayout.setCenter(new Button("Blabla"));
		
		Login login = new Login(client);
		Scene scene = new Scene(login.getUI());
		primaryStage.setScene(scene);		
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		initApp();		
		primaryStage.show();
	}
}
