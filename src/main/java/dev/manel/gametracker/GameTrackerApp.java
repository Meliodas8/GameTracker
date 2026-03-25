package dev.manel.gametracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class GameTrackerApp extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("GameTracker");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("GameTracker");
        stage.setScene(scene);
        stage.show();
    }
}
