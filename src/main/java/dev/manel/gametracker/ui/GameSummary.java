package dev.manel.gametracker.ui;

import dev.manel.gametracker.core.model.GameSession;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class GameSummary {

    private final String name;
    private final String platform;
    private final List<GameSession> sessions;

    public GameSummary(String name, String platform, List<GameSession> sessions) {
        this.name = name;
        this.platform = platform;
        this.sessions = sessions;
    }

    public String getName() { return name; }
    public String getPlatform() { return platform; }
    public List<GameSession> getSessions() { return sessions; }

    public Duration getTotalDuration() {
        return sessions.stream()
                .map(GameSession::duration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    public Instant getLastPlayed() {
        return sessions.stream()
                .map(GameSession::endTime)
                .max(Instant::compareTo)
                .orElse(null);
    }

    public long getSessionCount() {
        return sessions.size();
    }

    public Duration getThisWeekDuration() {
        Instant weekAgo = Instant.now().minus(java.time.Duration.ofDays(7));
        return sessions.stream()
                .filter(s -> s.startTime().isAfter(weekAgo))
                .map(GameSession::duration)
                .reduce(Duration.ZERO, Duration::plus);
    }
}
