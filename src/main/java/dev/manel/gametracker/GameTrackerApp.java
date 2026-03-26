package dev.manel.gametracker;

import dev.manel.gametracker.core.config.ConfigManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GameTrackerApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/dev/manel/gametracker/main.fxml")
        );

        Scene scene = new Scene(loader.load(), 900, 580);

        String theme = ConfigManager.getInstance().getTheme();
        String css = theme.equals("dark") ? "styles-dark.css" : "styles.css";
        scene.getStylesheets().add(
                getClass().getResource("/dev/manel/gametracker/" + css).toExternalForm()
        );

        stage.setTitle("GameTracker");
        stage.setMinWidth(700);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }
}
