package dev.manel.gametracker.ui;

import dev.manel.gametracker.core.config.ConfigManager;
import dev.manel.gametracker.core.config.I18n;
import dev.manel.gametracker.core.model.GameSession;
import dev.manel.gametracker.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsController {

    /** Rango del selector. days == 0 significa "todo el histórico". */
    private enum Range {
        D7(7, "stats.range.7d"),
        D30(30, "stats.range.30d"),
        M6(182, "stats.range.6m"),
        Y1(365, "stats.range.1y"),
        ALL(0, "stats.range.all");

        final int days;
        final String key;

        Range(int days, String key) {
            this.days = days;
            this.key = key;
        }
    }

    /** Ancho de cada barra. Con rangos largos, una barra por día no se lee. */
    enum Bucket {
        DAY("stats.perDay"), MONTH("stats.perMonth"), YEAR("stats.perYear");

        final String titleKey;

        Bucket(String titleKey) {
            this.titleKey = titleKey;
        }
    }

    private static final int TOP_GAMES = 6;

    @FXML private ComboBox<Range> rangeSelector;
    @FXML private MenuButton gameFilter;
    @FXML private Label chartTitle;
    @FXML private HBox tiles;
    @FXML private BarChart<String, Number> dailyChart;
    @FXML private PieChart gameChart;

    @FXML
    public void initialize() {
        rangeSelector.setItems(FXCollections.observableArrayList(Range.values()));
        rangeSelector.setConverter(new StringConverter<>() {
            @Override public String toString(Range range) {
                return range == null ? null : I18n.get(range.key);
            }
            @Override public Range fromString(String label) {
                return java.util.Arrays.stream(Range.values())
                        .filter(r -> I18n.get(r.key).equals(label))
                        .findFirst().orElse(Range.D7);
            }
        });
        rangeSelector.setValue(Range.D7);
        buildGameFilter();
        loadData();
    }

    @FXML
    public void onRangeChanged() {
        loadData();
    }

    /**
     * Una casilla por juego con sesiones. Desmarcar solo lo quita de estas
     * graficas: se sigue contando su tiempo y sigue en Juegos e Historial.
     */
    private void buildGameFilter() {
        List<String> games = SessionManager.getInstance().getCompletedSessions().stream()
                .map(GameSession::gameName)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        gameFilter.getItems().clear();
        gameFilter.setDisable(games.isEmpty());
        for (String game : games) {
            CheckMenuItem item = new CheckMenuItem(game);
            item.setSelected(!ConfigManager.getInstance().isHiddenInStats(game));
            item.selectedProperty().addListener((obs, old, selected) -> {
                ConfigManager.getInstance().setHiddenInStats(game, !selected);
                loadData();
            });
            gameFilter.getItems().add(item);
        }
        updateFilterLabel();
    }

    private void updateFilterLabel() {
        long hidden = gameFilter.getItems().stream()
                .filter(item -> item instanceof CheckMenuItem check && !check.isSelected())
                .count();
        gameFilter.setText(hidden == 0
                ? I18n.get("stats.filter")
                : I18n.get("stats.filter.hidden", hidden));
    }

    private void loadData() {
        Range range = rangeSelector.getValue() == null ? Range.D7 : rangeSelector.getValue();

        List<GameSession> visible = SessionManager.getInstance().getCompletedSessions().stream()
                .filter(s -> !ConfigManager.getInstance().isHiddenInStats(s.gameName()))
                .toList();

        LocalDate today = LocalDate.now();
        LocalDate from = range == Range.ALL
                ? visible.stream().map(StatsController::day).min(LocalDate::compareTo).orElse(today)
                : today.minusDays(range.days - 1L);

        List<GameSession> sessions = visible.stream()
                .filter(s -> !day(s).isBefore(from))
                .toList();

        updateFilterLabel();
        buildTiles(sessions);
        buildChart(sessions, from, today);
        buildGameChart(sessions);
    }

    private static LocalDate day(GameSession s) {
        return s.startTime().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Duration total(List<GameSession> sessions) {
        return sessions.stream().map(GameSession::duration).reduce(Duration.ZERO, Duration::plus);
    }

    private void buildTiles(List<GameSession> sessions) {
        Duration total = total(sessions);
        int count = sessions.size();
        Duration average = count == 0 ? Duration.ZERO : total.dividedBy(count);
        long playedDays = sessions.stream().map(StatsController::day).distinct().count();

        tiles.getChildren().setAll(
                tile(I18n.get("stats.total"), I18n.formatDuration(total),
                        I18n.get("stats.playedDays", playedDays)),
                tile(I18n.get("stats.sessions"), String.valueOf(count),
                        I18n.get("stats.avgSession", I18n.formatDuration(average))),
                tile(I18n.get("stats.topGame"), topGameName(sessions),
                        I18n.get("stats.games", sessions.stream()
                                .map(GameSession::gameName).distinct().count()))
        );
    }

    private String topGameName(List<GameSession> sessions) {
        return byGame(sessions).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");
    }

    private VBox tile(String label, String value, String sub) {
        VBox block = new VBox(4);
        block.getStyleClass().add("stat-block");
        HBox.setHgrow(block, Priority.ALWAYS);
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Label s = new Label(sub);
        s.getStyleClass().add("stat-sub");
        block.getChildren().addAll(l, v, s);
        return block;
    }

    static Bucket bucketFor(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days <= 45) return Bucket.DAY;
        if (days <= 3 * 366) return Bucket.MONTH;
        return Bucket.YEAR;
    }

    /** Una barra por periodo, también los periodos sin jugar. */
    private void buildChart(List<GameSession> sessions, LocalDate from, LocalDate to) {
        Bucket bucket = bucketFor(from, to);
        chartTitle.setText(I18n.get(bucket.titleKey));

        Map<LocalDate, Duration> perBucket = sessions.stream().collect(Collectors.groupingBy(
                s -> startOfBucket(day(s), bucket),
                Collectors.reducing(Duration.ZERO, GameSession::duration, Duration::plus)));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        LocalDate cursor = startOfBucket(from, bucket);
        LocalDate last = startOfBucket(to, bucket);
        while (!cursor.isAfter(last)) {
            double hours = perBucket.getOrDefault(cursor, Duration.ZERO).toMinutes() / 60.0;
            series.getData().add(new XYChart.Data<>(bucketLabel(cursor, bucket), hours));
            cursor = next(cursor, bucket);
        }
        dailyChart.getData().setAll(List.of(series));
    }

    private static LocalDate startOfBucket(LocalDate date, Bucket bucket) {
        return switch (bucket) {
            case DAY -> date;
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    private static LocalDate next(LocalDate date, Bucket bucket) {
        return switch (bucket) {
            case DAY -> date.plusDays(1);
            case MONTH -> date.plusMonths(1);
            case YEAR -> date.plusYears(1);
        };
    }

    private static String bucketLabel(LocalDate date, Bucket bucket) {
        return switch (bucket) {
            case DAY -> I18n.formatDayMonth(date);
            case MONTH -> I18n.formatMonthYear(date);
            case YEAR -> String.valueOf(date.getYear());
        };
    }

    /** Top juegos por tiempo; el resto se agrupa en "Otros". */
    private void buildGameChart(List<GameSession> sessions) {
        List<Map.Entry<String, Duration>> ranked = byGame(sessions).entrySet().stream()
                .sorted(Map.Entry.<String, Duration>comparingByValue().reversed())
                .toList();

        List<PieChart.Data> slices = ranked.stream()
                .limit(TOP_GAMES)
                .map(e -> new PieChart.Data(e.getKey(), e.getValue().toMinutes()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (ranked.size() > TOP_GAMES) {
            long rest = ranked.stream().skip(TOP_GAMES)
                    .mapToLong(e -> e.getValue().toMinutes()).sum();
            slices.add(new PieChart.Data(I18n.get("stats.other"), rest));
        }
        gameChart.setData(FXCollections.observableArrayList(slices));
    }

    private Map<String, Duration> byGame(List<GameSession> sessions) {
        return sessions.stream()
                .collect(Collectors.groupingBy(GameSession::gameName, LinkedHashMap::new,
                        Collectors.reducing(Duration.ZERO, GameSession::duration, Duration::plus)));
    }
}
