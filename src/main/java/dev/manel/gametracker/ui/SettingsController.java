package dev.manel.gametracker.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.manel.gametracker.autostart.AutostartStrategy;
import dev.manel.gametracker.core.config.ConfigManager;
import dev.manel.gametracker.core.config.I18n;
import dev.manel.gametracker.core.model.DetectedGame;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;

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
    @FXML private ComboBox<String> languageSelector;
    @FXML private ListView<DetectedGame> manualAppsList;
    @FXML private Label versionLabel;
    @FXML private Label updateStatusLabel;
    @FXML private Button checkUpdateBtn;

    private static final String RELEASES_API = "https://api.github.com/repos/Meliodas8/GameTracker/releases?per_page=20";

    private final AutostartStrategy autostart = AutostartStrategy.detect();
    private final String currentVersion = loadCurrentVersion();

    @FXML
    public void initialize() {
        themeSelector.setItems(FXCollections.observableArrayList(
                I18n.get("theme.light"), I18n.get("theme.dark")));
        themeSelector.setValue(
                ConfigManager.getInstance().getTheme().equals("dark")
                        ? I18n.get("theme.dark") : I18n.get("theme.light")
        );

        // el ComboBox guarda códigos ("en", "es") y muestra el nombre traducido
        languageSelector.setItems(FXCollections.observableArrayList(I18n.SUPPORTED));
        languageSelector.setConverter(new StringConverter<>() {
            @Override public String toString(String code) {
                return code == null ? null : I18n.get("lang." + code);
            }
            @Override public String fromString(String label) {
                return I18n.SUPPORTED.stream()
                        .filter(c -> I18n.get("lang." + c).equals(label))
                        .findFirst().orElse("en");
            }
        });
        languageSelector.setValue(ConfigManager.getInstance().getLanguage());

        chkAutostart.setSelected(autostart.isEnabled());
        versionLabel.setText(I18n.get("settings.version", currentVersion));
        updateStatusLabel.setText(I18n.get("settings.update.hint"));
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

                Button remove = new Button(I18n.get("common.remove"));
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
        AddAppDialog.show().ifPresent(app -> {
            ConfigManager.getInstance().addManualApp(app);
            loadManualApps();
        });
    }

    private void removeApp(DetectedGame app) {
        ConfigManager.getInstance().removeManualApp(app.executableName());
        loadManualApps();
    }

    @FXML
    public void onThemeChanged() {
        String selected = themeSelector.getValue();
        String theme = selected.equals(I18n.get("theme.dark")) ? "dark" : "light";
        ConfigManager.getInstance().setTheme(theme);
        applyTheme(theme);
    }

    @FXML
    public void onLanguageChanged() {
        String selected = languageSelector.getValue();
        // setValue() en initialize() también dispara este handler: ignorar si no cambia nada
        if (selected == null || selected.equals(ConfigManager.getInstance().getLanguage())) return;

        ConfigManager.getInstance().setLanguage(selected);
        I18n.apply(selected);
        MainController.reloadInto(languageSelector.getScene());
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
            showAlert(I18n.get("error.autostart", e.getMessage()));
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
        updateStatusLabel.setText(I18n.get("update.checking"));

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
                            ? I18n.get("update.upToDate", currentVersion)
                            : I18n.get("update.available", String.join(", ", newer));
                    Platform.runLater(() -> {
                        updateStatusLabel.setText(status);
                        checkUpdateBtn.setDisable(false);
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        updateStatusLabel.setText(I18n.get("update.error", e.getMessage()));
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
            if (in == null) return I18n.get("settings.version.unknown");
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version", I18n.get("settings.version.unknown"));
        } catch (IOException e) {
            return I18n.get("settings.version.unknown");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(I18n.get("common.appName"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
