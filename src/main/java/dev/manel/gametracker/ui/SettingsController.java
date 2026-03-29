package dev.manel.gametracker.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.manel.gametracker.autostart.AutostartStrategy;
import dev.manel.gametracker.core.config.ConfigManager;
import dev.manel.gametracker.core.model.DetectedGame;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsController {

    @FXML private CheckBox chkAutostart;
    @FXML private ComboBox<String> themeSelector;
    @FXML private TextField appNameField;
    @FXML private TextField appExecField;
    @FXML private ListView<DetectedGame> manualAppsList;
    @FXML private Label versionLabel;
    @FXML private Label updateStatusLabel;
    @FXML private Button checkUpdateBtn;

    private static final String RELEASES_API = "https://api.github.com/repos/Meliodas8/GameTracker/releases/latest";
    private static final String RELEASES_PAGE = "https://github.com/Meliodas8/GameTracker/releases/latest";

    private final AutostartStrategy autostart = AutostartStrategy.detect();
    private final String currentVersion = loadCurrentVersion();
    private boolean updateAvailable = false;
    private String cachedReleaseJson;

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

    @FXML
    public void onCheckUpdate() {
        if (updateAvailable) {
            performUpdate();
            return;
        }

        checkUpdateBtn.setDisable(true);
        updateStatusLabel.setText("Buscando actualizaciones...");

        Thread.ofVirtual().start(() -> {
            String status;
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(RELEASES_API))
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "GameTracker/" + currentVersion)
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                cachedReleaseJson = response.body();
                String latestVersion = parseTagName(cachedReleaseJson);
                if (latestVersion == null) {
                    status = "No se pudo obtener la última versión";
                } else if (latestVersion.equals(currentVersion)) {
                    status = "Ya tienes la última versión (" + currentVersion + ")";
                } else {
                    updateAvailable = true;
                    status = "Nueva versión disponible: " + latestVersion;
                }
            } catch (Exception e) {
                status = "Error al comprobar actualizaciones: " + e.getMessage();
            }

            String finalStatus = status;
            Platform.runLater(() -> {
                updateStatusLabel.setText(finalStatus);
                if (updateAvailable) {
                    checkUpdateBtn.setText("Actualizar ahora");
                }
                checkUpdateBtn.setDisable(false);
            });
        });
    }

    private void performUpdate() {
        checkUpdateBtn.setDisable(true);
        updateStatusLabel.setText("Preparando actualización...");

        Thread.ofVirtual().start(() -> {
            try {
                String os = System.getProperty("os.name", "").toLowerCase();
                if (os.contains("linux") && isArchLinux()) {
                    launchTerminalYayUpdate();
                } else {
                    String downloadUrl = findAssetUrl(cachedReleaseJson, os);
                    if (downloadUrl != null) {
                        downloadAndInstall(downloadUrl, os);
                    } else {
                        openBrowser(RELEASES_PAGE);
                        Platform.runLater(() -> {
                            updateStatusLabel.setText("Descarga abierta en el navegador");
                            checkUpdateBtn.setDisable(false);
                        });
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatusLabel.setText("Error al actualizar: " + e.getMessage());
                    checkUpdateBtn.setDisable(false);
                });
            }
        });
    }

    private boolean isArchLinux() {
        return Files.exists(Path.of("/etc/arch-release"));
    }

    private void launchTerminalYayUpdate() {
        List<String[]> terminals = List.of(
            new String[]{"konsole", "-e", "bash", "-c", "yay -Su gametracker; read"},
            new String[]{"gnome-terminal", "--", "bash", "-c", "yay -Su gametracker; read"},
            new String[]{"xfce4-terminal", "-e", "bash -c 'yay -Su gametracker; read'"},
            new String[]{"kitty", "bash", "-c", "yay -Su gametracker; read"},
            new String[]{"alacritty", "-e", "bash", "-c", "yay -Su gametracker; read"},
            new String[]{"xterm", "-e", "bash", "-c", "yay -Su gametracker; read"}
        );

        for (String[] cmd : terminals) {
            try {
                if (commandExists(cmd[0])) {
                    new ProcessBuilder(cmd).start();
                    Platform.runLater(() -> {
                        updateStatusLabel.setText("Actualizando en el terminal...");
                        checkUpdateBtn.setDisable(false);
                    });
                    return;
                }
            } catch (Exception ignored) {}
        }

        // No se encontró terminal, abrir navegador como fallback
        openBrowser(RELEASES_PAGE);
        Platform.runLater(() -> {
            updateStatusLabel.setText("Ejecuta manualmente: yay -Su gametracker");
            checkUpdateBtn.setDisable(false);
        });
    }

    private boolean commandExists(String command) {
        try {
            return new ProcessBuilder("which", command).start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String findAssetUrl(String json, String os) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray assets = root.getAsJsonArray("assets");
            String extension = os.contains("win") ? ".exe" : os.contains("mac") ? ".dmg" : ".AppImage";
            for (int i = 0; i < assets.size(); i++) {
                JsonObject asset = assets.get(i).getAsJsonObject();
                String name = asset.get("name").getAsString();
                if (name.endsWith(extension)) {
                    return asset.get("browser_download_url").getAsString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void downloadAndInstall(String downloadUrl, String os) throws Exception {
        String ext = os.contains("win") ? ".exe" : os.contains("mac") ? ".dmg" : ".AppImage";
        Path tempFile = Files.createTempFile("gametracker-update", ext);

        Platform.runLater(() -> updateStatusLabel.setText("Descargando actualización..."));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", "GameTracker/" + currentVersion)
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));
        tempFile.toFile().setExecutable(true);

        if (os.contains("win")) {
            new ProcessBuilder(tempFile.toString()).start();
        } else if (os.contains("mac")) {
            new ProcessBuilder("open", tempFile.toString()).start();
        } else {
            new ProcessBuilder(tempFile.toString()).start();
        }

        Platform.runLater(() -> {
            updateStatusLabel.setText("Instalador iniciado. Reinicia la aplicación.");
            checkUpdateBtn.setDisable(false);
        });
    }

    private void openBrowser(String url) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception ignored) {}
    }

    private String parseTagName(String json) {
        Matcher m = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
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
