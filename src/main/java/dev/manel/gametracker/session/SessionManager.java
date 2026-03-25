package dev.manel.gametracker.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.manel.gametracker.core.config.PathManager;
import dev.manel.gametracker.core.model.DetectedGame;
import dev.manel.gametracker.core.model.GameSession;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final Gson gson;
    private final Path sessionsFile;
    private final Map<String, Instant> activeSessions = new HashMap<>();
    private final List<GameSession> completedSessions = new ArrayList<>();

    private SessionManager() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .setPrettyPrinting()
                .create();
        this.sessionsFile = PathManager.getInstance().getSessionsFile();
        loadSessions();
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void startSession(DetectedGame game) {
        if (!activeSessions.containsKey(game.executableName())) {
            activeSessions.put(game.executableName(), Instant.now());
            System.out.println("Sesión iniciada: " + game.name());
        }
    }

    public void endSession(DetectedGame game) {
        Instant start = activeSessions.remove(game.executableName());
        if (start != null) {
            GameSession session = new GameSession(
                    game.name(),
                    game.platform(),
                    start,
                    Instant.now()
            );
            completedSessions.add(session);
            saveSessions();
            System.out.println("Sesión terminada: " + game.name());
        }
    }

    public List<GameSession> getCompletedSessions() {
        return List.copyOf(completedSessions);
    }

    public boolean isActive(DetectedGame game) {
        return activeSessions.containsKey(game.executableName());
    }

    private void loadSessions() {
        if (!Files.exists(sessionsFile)) return;
        try {
            String json = Files.readString(sessionsFile);
            Type listType = new TypeToken<List<GameSession>>() {}.getType();
            List<GameSession> loaded = gson.fromJson(json, listType);
            if (loaded != null) completedSessions.addAll(loaded);
        } catch (IOException e) {
            System.err.println("Error cargando sesiones: " + e.getMessage());
        }
    }

    private void saveSessions() {
        try {
            String json = gson.toJson(completedSessions);
            Files.writeString(sessionsFile, json);
        } catch (IOException e) {
            System.err.println("Error guardando sesiones: " + e.getMessage());
        }
    }
}
