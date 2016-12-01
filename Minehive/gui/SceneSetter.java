package gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneSetter extends Task<Void> {

	Stage stage;
	Parent next;

	public SceneSetter(Parent current, Parent next) {
		super();
		this.stage = (Stage) current.getScene().getWindow();
		this.next = next;
	}
	
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
				Scene scene = new Scene(next);
				stage.setScene(scene);
				stage.show();
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

}
