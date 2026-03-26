package dev.manel.gametracker.ui;

import dev.manel.gametracker.GameTrackerApp;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnJuegos;
    @FXML private Button btnHistorial;
    @FXML private Button btnAjustes;

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
                    GameTrackerApp.class.getResource("/dev/manel/gametracker/" + fxml)
            );
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Error cargando vista: " + e.getMessage());
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
