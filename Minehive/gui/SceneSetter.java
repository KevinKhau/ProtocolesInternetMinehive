package gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class SceneSetter extends Task<Void> {

	Stage stage;
	Parent next;

	public SceneSetter(Stage stage, Parent next) {
		super();
		this.stage = stage;
		this.next = next;
	}
	
	@Override
	protected Void call() throws Exception {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				ClientApp.setScene(stage, next);
			}
		});
		return null;
	}

	protected void failed() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				Dialog.error("Error", "An error occured", "Attempt to set scene failed");
			}
		});
	}
	
	public static void delayedScene(Stage stage, Parent next) {
		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						ClientApp.setScene(stage, next);
					}
				});
				return null;
			}
		};
		new Thread(task).start();
	}

}
