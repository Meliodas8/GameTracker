package dev.manel.gametracker.ui;

import dev.manel.gametracker.core.ProcessUtils;
import dev.manel.gametracker.core.config.ConfigManager;
import dev.manel.gametracker.core.model.DetectedGame;
import dev.manel.gametracker.core.model.GameSession;
import dev.manel.gametracker.session.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameListController {

    @FXML private ListView<GameSummary> gameListView;
    @FXML private VBox detailPanel;

    @FXML
    public void initialize() {
        setupListView();
        loadGames();

        // refresca la lista cada 10 segundos para mostrar sesiones nuevas
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(10),
                        e -> loadGames()
                )
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    private void loadGames() {
        List<GameSession> all = SessionManager.getInstance().getCompletedSessions();

        Map<String, List<GameSession>> grouped = all.stream()
                .collect(Collectors.groupingBy(GameSession::gameName));

        List<GameSummary> summaries = grouped.entrySet().stream()
                .map(e -> new GameSummary(
                        e.getKey(),
                        e.getValue().get(0).platform(),
                        e.getValue()
                ))
                .sorted(Comparator.comparing(
                        GameSummary::getTotalDuration).reversed()
                )
                .toList();

        Platform.runLater(() -> {
            gameListView.getItems().setAll(summaries);
        });
    }

    private void setupListView() {
        gameListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GameSummary game, boolean empty) {
                super.updateItem(game, empty);
                if (empty || game == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(buildGameCell(game));
            }
        });

        gameListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> {
                    if (selected != null) showDetail(selected);
                });
    }

    private HBox buildGameCell(GameSummary game) {
        HBox row = new HBox(12);
        row.getStyleClass().add("game-card");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label icon = new Label(game.getPlatform().equals("MANUAL") ? "💻" : "🎮");
        icon.getStyleClass().add("game-icon");

        VBox info = new VBox(3);
        Label name = new Label(game.getName());
        name.getStyleClass().add("game-name");
        Label lastPlayed = new Label(formatLastPlayed(game.getLastPlayed()));
        lastPlayed.getStyleClass().add("game-time");
        info.getChildren().addAll(name, lastPlayed);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox right = new VBox(4);
        right.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label total = new Label(formatDuration(game.getTotalDuration()));
        total.getStyleClass().add("game-total");

        boolean isRunning = SessionManager.getInstance().isActiveByName(game.getName());
        if (isRunning) {
            Label badge = new Label("jugando");
            badge.getStyleClass().add("badge-running");
            right.getChildren().addAll(total, badge);
        } else if (game.getPlatform().equals("MANUAL")) {
            Label badge = new Label("manual");
            badge.getStyleClass().add("badge-manual");
            right.getChildren().addAll(total, badge);
        } else {
            right.getChildren().add(total);
        }

        row.getChildren().addAll(icon, info, right);
        return row;
    }

    private void showDetail(GameSummary game) {
        detailPanel.getChildren().clear();

        Label title = new Label(game.getName());
        title.getStyleClass().add("detail-title");

        VBox totalBlock = buildStatBlock("Tiempo total",
                formatDuration(game.getTotalDuration()),
                game.getSessionCount() + " sesiones");

        VBox weekBlock = buildStatBlock("Esta semana",
                formatDuration(game.getThisWeekDuration()),
                "últimos 7 días");

        Label sessionsTitle = new Label("ÚLTIMAS SESIONES");
        sessionsTitle.getStyleClass().add("detail-section-title");

        VBox sessionList = new VBox(4);
        game.getSessions().stream()
                .sorted(Comparator.comparing(GameSession::startTime).reversed())
                .limit(5)
                .forEach(s -> {
                    HBox row = new HBox();
                    Label date = new Label(formatDate(s.startTime()));
                    date.getStyleClass().add("session-date");
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    Label dur = new Label(formatDuration(s.duration()));
                    dur.getStyleClass().add("session-duration");
                    row.getChildren().addAll(date, spacer, dur);
                    row.getStyleClass().add("session-item");
                    sessionList.getChildren().add(row);
                });

        detailPanel.getChildren().addAll(
                title, totalBlock, weekBlock, sessionsTitle, sessionList
        );
    }

    private VBox buildStatBlock(String label, String value, String sub) {
        VBox block = new VBox(4);
        block.getStyleClass().add("stat-block");
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Label s = new Label(sub);
        s.getStyleClass().add("stat-sub");
        block.getChildren().addAll(l, v, s);
        return block;
    }

    @FXML
    public void addManualApp() {
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
        List<ProcessUtils.ProcessInfo> processes = ProcessUtils.getRunningProcessInfosSorted();

        TextField filterField = new TextField();
        filterField.setPromptText("Filtrar...");

        ListView<ProcessUtils.ProcessInfo> processList = new ListView<>();
        ObservableList<ProcessUtils.ProcessInfo> allProcesses = FXCollections.observableArrayList(processes);
        processList.setItems(allProcesses);
        processList.setPrefHeight(160);
        processList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ProcessUtils.ProcessInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayLabel());
            }
        });

        filterField.textProperty().addListener((obs, old, val) ->
                processList.setItems(val.isBlank()
                        ? allProcesses
                        : allProcesses.filtered(p -> p.displayLabel().toLowerCase().contains(val.toLowerCase())))
        );

        // Al seleccionar un proceso se rellena automáticamente el campo ejecutable
        processList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> { if (selected != null) execField.setText(selected.suggestedExecName()); }
        );

        Label nameLabel = new Label("Nombre");
        nameLabel.getStyleClass().add("settings-label");
        Label execLabel = new Label("Ejecutable");
        execLabel.getStyleClass().add("settings-label");
        Label processLabel = new Label("Procesos en ejecución — clic para usar, o escribe parte de la ruta para apps Java/Python");
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
        nameField.textProperty().addListener((obs, old, val) ->
                addButton.setDisable(val.isBlank() || execField.getText().isBlank()));
        execField.textProperty().addListener((obs, old, val) ->
                addButton.setDisable(val.isBlank() || nameField.getText().isBlank()));

        dialog.setResultConverter(bt -> bt == addButtonType
                ? new DetectedGame(nameField.getText().trim(), "MANUAL", execField.getText().trim(), null)
                : null
        );

        dialog.showAndWait().ifPresent(app -> {
            ConfigManager.getInstance().addManualApp(app);
            loadGames();
        });
    }


    private String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }

    private String formatLastPlayed(Instant instant) {
        if (instant == null) return "Sin sesiones";
        LocalDate sessionDay = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        long days = ChronoUnit.DAYS.between(sessionDay, LocalDate.now());
        if (days == 0) return "Última sesión: hoy";
        if (days == 1) return "Última sesión: ayer";
        return "Última sesión: hace " + days + " días";
    }

    private String formatDate(Instant instant) {
        return DateTimeFormatter.ofPattern("dd/MM")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}
