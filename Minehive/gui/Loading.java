package gui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

public class Loading extends StackPane {
	ClientApp app;
	Parent onSuccess;
	Parent onFailure;
	
	public Loading(ClientApp clientApp, Parent success, Parent failure) {
		super();
		this.app = clientApp;
		this.onSuccess = success;
		this.onFailure = failure;
		
		this.setAlignment(Pos.CENTER);
		ProgressIndicator p = new ProgressIndicator();
		p.setMaxWidth(100);
		p.setMaxHeight(100);
		this.getChildren().add(p);
		
		Label label = new Label("Loading");
		this.getChildren().add(label);
		
	}

	public void next() {
		SceneSetter task = new SceneSetter(app.primaryStage, onSuccess);
		new Thread(task).start();
	}
	
	public void error(String message) {
//		Dialog.error("Error", "An error occured", message);
		previous();
	}
	
	public void previous() {
	    SceneSetter task = new SceneSetter(app.primaryStage, onFailure);
		new Thread(task).start();
	}
}
