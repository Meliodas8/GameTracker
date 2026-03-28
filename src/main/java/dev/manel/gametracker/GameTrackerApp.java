package dev.manel.gametracker;

import dev.manel.gametracker.core.config.ConfigManager;
import dev.manel.gametracker.providers.GameSourceRegistry;
import dev.manel.gametracker.providers.ManualProvider;
import dev.manel.gametracker.providers.SteamProvider;
import dev.manel.gametracker.session.ProcessWatcher;
import dev.manel.gametracker.session.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GameTrackerApp extends Application {

    private ProcessWatcher watcher;

    @Override
    public void start(Stage stage) throws IOException {
        GameSourceRegistry registry = new GameSourceRegistry();
        registry.register(new SteamProvider());
        registry.register(new ManualProvider());

        watcher = new ProcessWatcher(registry, SessionManager.getInstance());
        watcher.start();

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

    @Override
    public void stop() {
        if (watcher != null) {
            watcher.stop();
        }
    }
}
