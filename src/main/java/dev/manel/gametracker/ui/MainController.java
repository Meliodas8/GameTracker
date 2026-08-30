package dev.manel.gametracker.ui;

import dev.manel.gametracker.GameTrackerApp;
import dev.manel.gametracker.core.config.I18n;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnJuegos;
    @FXML private Button btnHistorial;
    @FXML private Button btnAjustes;

    /** Carga main.fxml con el bundle del idioma activo. */
    public static FXMLLoader mainLoader() {
        return new FXMLLoader(
                GameTrackerApp.class.getResource("/dev/manel/gametracker/main.fxml"),
                I18n.bundle()
        );
    }

    /**
     * Reconstruye toda la interfaz con el idioma actual y vuelve a Ajustes.
     * Es la forma de aplicar un cambio de idioma sin reiniciar la app.
     */
    public static void reloadInto(Scene scene) {
        try {
            FXMLLoader loader = mainLoader();
            Parent root = loader.load();
            scene.setRoot(root);
            loader.<MainController>getController().showSettings();
        } catch (IOException e) {
            System.err.println(I18n.get("error.loadView") + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        showGames();
    }

    @FXML
    public void showGames() {
        setActiveButton(btnJuegos);
        loadView("game-list.fxml");
    }

    @FXML
    public void showHistory() {
        setActiveButton(btnHistorial);
        loadView("history.fxml");
    }

    @FXML
    public void showSettings() {
        setActiveButton(btnAjustes);
        loadView("settings.fxml");
    }

    private void loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    GameTrackerApp.class.getResource("/dev/manel/gametracker/" + fxml),
                    I18n.bundle()
            );
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println(I18n.get("error.loadView") + e.getMessage());
        }
    }

    private void setActiveButton(Button active) {
        btnJuegos.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("active"), false);
        btnHistorial.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("active"), false);
        btnAjustes.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("active"), false);
        active.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("active"), true);
    }
}
