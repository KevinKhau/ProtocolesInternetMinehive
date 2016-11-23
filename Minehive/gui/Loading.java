package gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import network.Client;

public class Loading {
	private Client client;
	private StackPane layout;

	public Loading(Client client) {
		this.client = client;
		layout = new StackPane();
		layout.setAlignment(Pos.CENTER);

		ProgressIndicator p = new ProgressIndicator();
		p.setMaxWidth(100);
		p.setMaxHeight(100);
		
		Label label = new Label("Loading");

		layout.getChildren().add(p);
		layout.getChildren().add(label);

		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				//while(true) {
				// Wait for client to load everything
				Thread.sleep(200);
				//}
				
				Platform.runLater(new Runnable() {
					@Override public void run() {
						startGame();
					}
				});
				
				return null;
			}
			
			protected void failed() {
				Platform.runLater(new Runnable() {
					@Override public void run() {
						error("Task failed");
					}
				});
			}
		};

		new Thread(task).start();
	}

	public Parent getUI() {
		return layout;
	}

	protected void startGame() {
		Stage app = (Stage) layout.getScene().getWindow();
	    Game game = new Game(client);
	    Scene scene = new Scene(game.getUI());
	    app.setScene(scene);
	    app.show();
	}
	
	protected void error(String message) {
		Dialog.error("Error", "An error occured", message);
		
		Stage app = (Stage) layout.getScene().getWindow();
	    Login login = new Login(client);
	    Scene scene = new Scene(login.getUI());
	    app.setScene(scene);
	    app.show();
	}
}
