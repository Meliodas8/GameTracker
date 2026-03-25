package dev.manel.gametracker.core.model;

import java.time.Duration;
import java.time.Instant;

public record GameSession(
        String gameName,
        String platform,
        Instant startTime,
        Instant endTime
) {
    public Duration duration() {
        return Duration.between(startTime, endTime);
    }

    public boolean isActive() {
        return endTime == null;
    }
}
