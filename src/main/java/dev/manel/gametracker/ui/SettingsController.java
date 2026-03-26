package dev.manel.gametracker.ui;

import dev.manel.gametracker.autostart.AutostartStrategy;
import dev.manel.gametracker.core.config.ConfigManager;
import dev.manel.gametracker.core.model.DetectedGame;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SettingsController {

    @FXML private CheckBox chkAutostart;
    @FXML private ComboBox<String> themeSelector;
    @FXML private TextField appNameField;
    @FXML private TextField appExecField;
    @FXML private ListView<DetectedGame> manualAppsList;

    private final AutostartStrategy autostart = AutostartStrategy.detect();

    @FXML
    public void initialize() {
        themeSelector.setItems(FXCollections.observableArrayList("Claro", "Oscuro"));
        themeSelector.setValue(
                ConfigManager.getInstance().getTheme().equals("dark") ? "Oscuro" : "Claro"
        );
        chkAutostart.setSelected(autostart.isEnabled());
        setupManualAppsList();
        loadManualApps();
    }

    private void setupManualAppsList() {
        manualAppsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DetectedGame app, boolean empty) {
                super.updateItem(app, empty);
                if (empty || app == null) {
                    setGraphic(null);
                    return;
                }
                HBox row = new HBox(8);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label name = new Label(app.name());
                name.getStyleClass().add("settings-label");

                Label exec = new Label(app.executableName());
                exec.getStyleClass().add("settings-description");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button remove = new Button("Eliminar");
                remove.getStyleClass().add("btn-danger");
                remove.setOnAction(e -> removeApp(app));

                row.getChildren().addAll(name, exec, spacer, remove);
                setGraphic(row);
            }
        });
    }

    private void loadManualApps() {
        List<DetectedGame> apps = ConfigManager.getInstance().getManualApps();
        manualAppsList.setItems(FXCollections.observableArrayList(apps));
    }

    @FXML
    public void addApp() {
        String name = appNameField.getText().trim();
        String exec = appExecField.getText().trim();

        if (name.isBlank() || exec.isBlank()) {
            showAlert("Rellena el nombre y el ejecutable.");
            return;
        }

        DetectedGame app = new DetectedGame(name, "MANUAL", exec, null);
        ConfigManager.getInstance().addManualApp(app);
        appNameField.clear();
        appExecField.clear();
        loadManualApps();
    }

    private void removeApp(DetectedGame app) {
        ConfigManager.getInstance().removeManualApp(app.executableName());
        loadManualApps();
    }

    @FXML
    public void onThemeChanged() {
        String selected = themeSelector.getValue();
        String theme = selected.equals("Oscuro") ? "dark" : "light";
        ConfigManager.getInstance().setTheme(theme);
        applyTheme(theme);
    }

    @FXML
    public void onAutostartChanged() {
        try {
            if (chkAutostart.isSelected()) {
                autostart.enable();
            } else {
                autostart.disable();
            }
        } catch (RuntimeException e) {
            showAlert("Error al cambiar el autostart: " + e.getMessage());
            chkAutostart.setSelected(autostart.isEnabled());
        }
    }

    private void applyTheme(String theme) {
        String css = theme.equals("dark") ? "styles-dark.css" : "styles.css";
        manualAppsList.getScene().getStylesheets().clear();
        manualAppsList.getScene().getStylesheets().add(
                getClass().getResource("/dev/manel/gametracker/" + css).toExternalForm()
        );
    }

    private Path getServicePath() {
        return Path.of(System.getProperty("user.home"),
                ".config", "systemd", "user", "gametracker.service");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("GameTracker");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
