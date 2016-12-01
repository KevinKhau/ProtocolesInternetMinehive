package gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneSetter extends Task<Void> {

	Parent current;
	Parent next;

	public SceneSetter(Parent current, Parent next) {
		super();
		this.current = current;
		this.next = next;
	}

	@Override
	protected Void call() throws Exception {

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				Stage app = (Stage) current.getScene().getWindow();
				Scene scene = new Scene(next);
				app.setScene(scene);
				app.show();
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
