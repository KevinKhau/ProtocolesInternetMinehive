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
import javafx.stage.Stage;

public class Dialog {
	public static void error(String title, String header, String text) {
		System.out.println("Error: " + text);
		try {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle(title);
			alert.setHeaderText(header);
			alert.setContentText(text);
			alert.showAndWait();
		} catch (IllegalStateException e) {
			delayedError(title, header, text);
		}
	}

	public static void warning(String title, String header, String text) {
		try {
			System.out.println("Warning: " + text);
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle(title);
			alert.setHeaderText(header);
			alert.setContentText(text);
			alert.showAndWait();
		} catch (IllegalStateException e) {
			delayedWarning(title, header, text);
		}
	}

	public static void exception(Exception e, String message) {
		System.out.println("Exception: " + message);
		if (message == null) {
			message = e.getMessage();
		}
		Alert alert;
		try {
			alert = new Alert(AlertType.ERROR);
			alert.setTitle("Exception");
			alert.setHeaderText(e.getClass().getCanonicalName());
			
			alert.setContentText(message);
			
			StringWriter writer = new StringWriter();
			PrintWriter printer = new PrintWriter(writer);
			e.printStackTrace(printer);
			String exceptionText = writer.toString();
			
			Label label = new Label("Stacktrace:");
			
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
				Dialog.exception(e, "Connection with Server lost");   
			}
		});
	}
}
