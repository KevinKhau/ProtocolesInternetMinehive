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

public class Loading extends StackPane {
	private ClientModel client;

	public Loading(ClientModel client2) {
		this.client = client2;
		this.setAlignment(Pos.CENTER);

		ProgressIndicator p = new ProgressIndicator();
		p.setMaxWidth(100);
		p.setMaxHeight(100);
		
		Label label = new Label("Loading");

		this.getChildren().add(p);
		this.getChildren().add(label);

		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				
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
		return this;
	}

	protected void startGame() {
		Stage app = (Stage) this.getScene().getWindow();
	    Game game = new Game(client);
	    Scene scene = new Scene(game);
	    app.setScene(scene);
	    app.show();
	}
	
	protected void error(String message) {
		Dialog.error("Error", "An error occured", message);
		
		Stage app = (Stage) this.getScene().getWindow();
	    Login login = new Login(client);
	    Scene scene = new Scene(login);
	    app.setScene(scene);
	    app.show();
	}
}
