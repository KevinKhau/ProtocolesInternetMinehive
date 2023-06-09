package gui;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class Dialog {
	public static void info(String title, String header, String text) {
		try {
			Alert alert = new Alert(AlertType.INFORMATION);
			System.err.println("Info: " + text);
			alert.setTitle(title);
			alert.setHeaderText(header);
			alert.setContentText(text);
			alert.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> ((Label)node).setMinHeight(Region.USE_PREF_SIZE));
			alert.showAndWait();
		} catch (IllegalStateException e) {
			delayedInfo(title, header, text);
		}
	}
	
	public static void error(String title, String header, String text) {
		try {
			Alert alert = new Alert(AlertType.ERROR);
			System.err.println("Error: " + text);
			alert.setTitle(title);
			alert.setHeaderText(header);
			alert.setContentText(text);
			alert.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> ((Label)node).setMinHeight(Region.USE_PREF_SIZE));
			alert.showAndWait();
		} catch (IllegalStateException e) {
			delayedError(title, header, text);
		}
	}

	public static void warning(String title, String header, String text) {
		try {
			Alert alert = new Alert(AlertType.WARNING);
			System.out.println("Warning: " + text);
			alert.setTitle(title);
			alert.setHeaderText(header);
			alert.setContentText(text);
			alert.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> ((Label)node).setMinHeight(Region.USE_PREF_SIZE));
			alert.showAndWait();
		} catch (IllegalStateException e) {
			delayedWarning(title, header, text);
		}
	}

	public static void exception(Exception e, String message) {
		if (message == null) {
			message = e.getMessage();
		}
		Alert alert;
		try {
			alert = new Alert(AlertType.ERROR);
			System.err.println("Exception: " + message);
			alert.setGraphic(ClientApp.getLogo());
			alert.setTitle("Exception");
			alert.setHeaderText(e.getClass().getCanonicalName());
			
			alert.setContentText(message);
			
			StringWriter writer = new StringWriter();
			PrintWriter printer = new PrintWriter(writer);
			e.printStackTrace(printer);
			String exceptionText = writer.toString();
			
			Label label = new Label("StackTrace:");
			
			TextArea textArea = new TextArea(exceptionText);
			textArea.setEditable(false);
			textArea.setWrapText(true);
			textArea.setStyle("-fx-text-fill: red;");
			
			textArea.setMaxWidth(Double.MAX_VALUE);
			textArea.setMaxHeight(Double.MAX_VALUE);
			GridPane.setVgrow(textArea, Priority.ALWAYS);
			GridPane.setHgrow(textArea, Priority.ALWAYS);
			
			GridPane expContent = new GridPane();
			expContent.setMaxWidth(Double.MAX_VALUE);
			expContent.add(label, 0, 0);
			expContent.add(textArea, 0, 1);
			
			alert.getDialogPane().setExpandableContent(expContent);
			
			alert.getDialogPane().expandedProperty().addListener((l) -> {
				Platform.runLater(() -> {
					alert.getDialogPane().requestLayout();
					Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
					stage.sizeToScene();
				});
			});
			
			alert.showAndWait();
		} catch (IllegalStateException e1) {
			delayedException(e, message);
		}
	}

	/** CHECK possibilités de boucles infinies ? */
	private static void delayedInfo(String title, String header, String text) {
		Platform.runLater(new Runnable() {
			@Override public void run() {
				Dialog.info(title, header, text);   
			}
		});
	}
	
	private static void delayedError(String title, String header, String text) {
		Platform.runLater(new Runnable() {
			@Override public void run() {
				Dialog.error(title, header, text);   
			}
		});
	}
	
	private static void delayedWarning(String title, String header, String text) {
		Platform.runLater(new Runnable() {
			@Override public void run() {
				Dialog.warning(title, header, text);   
			}
		});
	}
	
	private static void delayedException(Exception e, String message) {
		Platform.runLater(new Runnable() {
			@Override public void run() {
				Dialog.exception(e, message);   
			}
		});
	}
}
