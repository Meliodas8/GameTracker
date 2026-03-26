package dev.manel.gametracker.ui;

import dev.manel.gametracker.core.model.GameSession;
import dev.manel.gametracker.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HistoryController {

    @FXML private ComboBox<String> gameFilter;
    @FXML private ToggleButton btnDay;
    @FXML private ToggleButton btnWeek;
    @FXML private TableView<HistoryEntry> historyTable;
    @FXML private TableColumn<HistoryEntry, String> colDate;
    @FXML private TableColumn<HistoryEntry, String> colGame;
    @FXML private TableColumn<HistoryEntry, String> colDuration;
    @FXML private TableColumn<HistoryEntry, Long> colSessions;

    @FXML
    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        btnDay.setToggleGroup(group);
        btnWeek.setToggleGroup(group);
        btnWeek.setSelected(true);

        setupTable();
        setupFilter();
        loadData();
    }

    private void setupTable() {
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colDate.setStyle("-fx-alignment: CENTER;");
        colGame.setStyle("-fx-alignment: CENTER;");
        colDuration.setStyle("-fx-alignment: CENTER;");
        colSessions.setStyle("-fx-alignment: CENTER;");
        colDate.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().date()));
        colGame.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().gameName()));
        colDuration.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        formatDuration(data.getValue().duration())));
        colSessions.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue().sessions()));
    }

    private void setupFilter() {
        List<String> games = SessionManager.getInstance().getCompletedSessions()
                .stream()
                .map(GameSession::gameName)
                .distinct()
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        games.add(0, "Todos los juegos");
        gameFilter.setItems(FXCollections.observableArrayList(games));
        gameFilter.setValue("Todos los juegos");
        gameFilter.setOnAction(e -> loadData());
    }

    @FXML
    public void onPeriodChanged() {
        loadData();
    }

    private void loadData() {
        List<GameSession> sessions = SessionManager.getInstance().getCompletedSessions();

        String selectedGame = gameFilter.getValue();
        if (selectedGame != null && !selectedGame.equals("Todos los juegos")) {
            sessions = sessions.stream()
                    .filter(s -> s.gameName().equals(selectedGame))
                    .toList();
        }

        boolean byDay = btnDay.isSelected();
        List<HistoryEntry> entries = byDay ? groupByDay(sessions) : groupByWeek(sessions);

        historyTable.setItems(FXCollections.observableArrayList(entries));
    }

    private List<HistoryEntry> groupByDay(List<GameSession> sessions) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return sessions.stream()
                .collect(Collectors.groupingBy(s -> {
                    LocalDate date = s.startTime()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                    return date + "|" + s.gameName();
                }))
                .entrySet().stream()
                .map(e -> {
                    String[] parts = e.getKey().split("\\|", 2);
                    LocalDate date = LocalDate.parse(parts[0]);
                    Duration total = e.getValue().stream()
                            .map(GameSession::duration)
                            .reduce(Duration.ZERO, Duration::plus);
                    return new HistoryEntry(
                            date.format(fmt), parts[1], total, e.getValue().size());
                })
                .sorted(Comparator.comparing(HistoryEntry::date).reversed())
                .toList();
    }

    private List<HistoryEntry> groupByWeek(List<GameSession> sessions) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        return sessions.stream()
                .collect(Collectors.groupingBy(s -> {
                    LocalDate date = s.startTime()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                    int week = date.get(weekFields.weekOfWeekBasedYear());
                    int year = date.get(weekFields.weekBasedYear());
                    return year + "-W" + String.format("%02d", week) + "|" + s.gameName();
                }))
                .entrySet().stream()
                .map(e -> {
                    String[] parts = e.getKey().split("\\|", 2);
                    Duration total = e.getValue().stream()
                            .map(GameSession::duration)
                            .reduce(Duration.ZERO, Duration::plus);
                    return new HistoryEntry(
                            parts[0], parts[1], total, e.getValue().size());
                })
                .sorted(Comparator.comparing(HistoryEntry::date).reversed())
                .toList();
    }

    private String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }

}
