package dev.manel.gametracker.ui;

import java.time.Duration;

public record HistoryEntry(
        String date,
        String gameName,
        Duration duration,
        long sessions
) {
}
