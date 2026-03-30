package dev.manel.gametracker.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.manel.gametracker.autostart.AutostartStrategy;
import dev.manel.gametracker.core.config.ConfigManager;
import dev.manel.gametracker.core.model.DetectedGame;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.stream.Collectors;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class SettingsController {

    @FXML private CheckBox chkAutostart;
    @FXML private ComboBox<String> themeSelector;
    @FXML private ListView<DetectedGame> manualAppsList;
    @FXML private Label versionLabel;
    @FXML private Label updateStatusLabel;
    @FXML private Button checkUpdateBtn;

    private static final String RELEASES_API = "https://api.github.com/repos/Meliodas8/GameTracker/releases?per_page=20";

    private final AutostartStrategy autostart = AutostartStrategy.detect();
    private final String currentVersion = loadCurrentVersion();

    @FXML
    public void initialize() {
        themeSelector.setItems(FXCollections.observableArrayList("Claro", "Oscuro"));
        themeSelector.setValue(
                ConfigManager.getInstance().getTheme().equals("dark") ? "Oscuro" : "Claro"
        );
        chkAutostart.setSelected(autostart.isEnabled());
        versionLabel.setText("Versión actual: " + currentVersion);
        updateStatusLabel.setText("Comprueba si hay una nueva versión disponible");
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
    public void openAddAppDialog() {
        Dialog<DetectedGame> dialog = new Dialog<>();
        dialog.setTitle("Añadir aplicación manual");
        dialog.setHeaderText(null);

        // Aplicar el tema actual al diálogo
        String css = ConfigManager.getInstance().getTheme().equals("dark") ? "styles-dark.css" : "styles.css";
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/dev/manel/gametracker/" + css).toExternalForm()
        );

        ButtonType addButtonType = new ButtonType("Añadir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Campos del formulario
        TextField nameField = new TextField();
        nameField.setPromptText("Nombre (ej: Brave)");

        TextField execField = new TextField();
        execField.setPromptText("Ejecutable (ej: brave)");

        // Lista de procesos en ejecución — misma lógica que ProcessWatcher
        List<String> processes = getRunningProcessNames();

        TextField filterField = new TextField();
        filterField.setPromptText("Filtrar...");

        ListView<String> processList = new ListView<>();
        ObservableList<String> allProcesses = FXCollections.observableArrayList(processes);
        processList.setItems(allProcesses);
        processList.setPrefHeight(160);

        filterField.textProperty().addListener((obs, old, val) ->
                processList.setItems(val.isBlank()
                        ? allProcesses
                        : allProcesses.filtered(p -> p.toLowerCase().contains(val.toLowerCase())))
        );

        // Al seleccionar un proceso se rellena automáticamente el campo ejecutable
        processList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> { if (selected != null) execField.setText(selected); }
        );

        Label nameLabel = new Label("Nombre");
        nameLabel.getStyleClass().add("settings-label");
        Label execLabel = new Label("Ejecutable");
        execLabel.getStyleClass().add("settings-label");
        Label processLabel = new Label("Procesos en ejecución (haz clic para usar)");
        processLabel.getStyleClass().add("settings-description");

        VBox content = new VBox(8,
                nameLabel, nameField,
                execLabel, execField,
                processLabel, filterField, processList
        );
        content.setPadding(new Insets(16));
        content.setPrefWidth(400);
        dialog.getDialogPane().setContent(content);

        // El botón Añadir se activa solo cuando ambos campos tienen texto
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        ChangeListener<String> validator = (obs, old, val) ->
                addButton.setDisable(nameField.getText().isBlank() || execField.getText().isBlank());
        nameField.textProperty().addListener(validator);
        execField.textProperty().addListener(validator);

        dialog.setResultConverter(bt -> bt == addButtonType
                ? new DetectedGame(nameField.getText().trim(), "MANUAL", execField.getText().trim(), null)
                : null
        );

        dialog.showAndWait().ifPresent(app -> {
            ConfigManager.getInstance().addManualApp(app);
            loadManualApps();
        });
    }

    // Misma lógica que ProcessWatcher.getRunningProcessNames() para que los nombres coincidan
    private List<String> getRunningProcessNames() {
        return ProcessHandle.allProcesses()
                .map(p -> p.info().command().orElse(""))
                .filter(cmd -> !cmd.isBlank())
                .map(cmd -> {
                    int lastSlash = Math.max(cmd.lastIndexOf('/'), cmd.lastIndexOf('\\'));
                    String name = cmd.substring(lastSlash + 1);
                    if (name.toLowerCase().endsWith(".exe")) {
                        name = name.substring(0, name.length() - 4);
                    }
                    return name;
                })
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
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

    @FXML
    public void onCheckUpdate() {
        checkUpdateBtn.setDisable(true);
        updateStatusLabel.setText("Buscando actualizaciones...");

        HttpClient client = buildHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RELEASES_API))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GameTracker/" + currentVersion)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    List<String> newer = parseNewerVersions(response.body());
                    String status = newer.isEmpty()
                            ? "Ya tienes la última versión (" + currentVersion + ")"
                            : "Actualizaciones disponibles: " + String.join(", ", newer);
                    Platform.runLater(() -> {
                        updateStatusLabel.setText(status);
                        checkUpdateBtn.setDisable(false);
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        updateStatusLabel.setText("Error al comprobar actualizaciones: " + e.getMessage());
                        checkUpdateBtn.setDisable(false);
                    });
                    return null;
                });
    }

    private HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private List<String> parseNewerVersions(String json) {
        List<String> newer = new ArrayList<>();
        try {
            JsonArray releases = JsonParser.parseString(json).getAsJsonArray();
            for (int i = 0; i < releases.size(); i++) {
                JsonObject release = releases.get(i).getAsJsonObject();
                if (release.get("draft").getAsBoolean() || release.get("prerelease").getAsBoolean()) continue;
                String tag = release.get("tag_name").getAsString();
                String version = tag.startsWith("v") ? tag.substring(1) : tag;
                if (isNewerThan(version, currentVersion)) {
                    newer.add(version);
                }
            }
        } catch (Exception ignored) {}
        return newer;
    }

    private boolean isNewerThan(String candidate, String current) {
        try {
            int[] c = Arrays.stream(candidate.split("\\.")).mapToInt(Integer::parseInt).toArray();
            int[] cur = Arrays.stream(current.split("\\.")).mapToInt(Integer::parseInt).toArray();
            for (int i = 0; i < Math.min(c.length, cur.length); i++) {
                if (c[i] > cur[i]) return true;
                if (c[i] < cur[i]) return false;
            }
            return c.length > cur.length;
        } catch (Exception e) {
            return false;
        }
    }

    private String loadCurrentVersion() {
        try (InputStream in = getClass().getResourceAsStream("/dev/manel/gametracker/version.properties")) {
            if (in == null) return "desconocida";
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version", "desconocida");
        } catch (IOException e) {
            return "desconocida";
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("GameTracker");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
