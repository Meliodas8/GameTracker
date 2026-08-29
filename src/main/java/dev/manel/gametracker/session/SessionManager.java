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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    /** Guarda el juego junto al inicio para poder cerrar sesiones al apagar. */
    private record Active(DetectedGame game, Instant start) {}

    private final Gson gson;
    private final Path sessionsFile;
    private final Map<String, Active> activeSessions = new HashMap<>();
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
            activeSessions.put(game.executableName(), new Active(game, Instant.now()));
            System.out.println("Sesión iniciada: " + game.name());
        }
    }

    public void endSession(DetectedGame game) {
        Active active = activeSessions.remove(game.executableName());
        if (active != null) {
            record(active, Instant.now());
            saveSessions();
        }
    }

    /**
     * Cierra y persiste todas las sesiones abiertas. Sin esto, apagar el proceso
     * perdia el tiempo acumulado: activeSessions solo vive en memoria.
     */
    public void endAllActive() {
        if (activeSessions.isEmpty()) return;
        Instant now = Instant.now();
        activeSessions.values().forEach(active -> record(active, now));
        activeSessions.clear();
        saveSessions();
    }

    /**
     * Una sesion que no dura mas de un ciclo de escaneo es un artefacto de
     * deteccion: el proceso aparece al lanzar el juego, desaparece mientras
     * carga y vuelve. Se descarta en vez de ensuciar el historico.
     */
    private void record(Active active, Instant end) {
        String name = active.game().name();
        if (Duration.between(active.start(), end).getSeconds()
                <= ProcessWatcher.SCAN_INTERVAL_SECONDS) {
            System.out.println("Sesión descartada (demasiado corta): " + name);
            return;
        }
        completedSessions.add(new GameSession(
                name, active.game().platform(), active.start(), end));
        System.out.println("Sesión terminada: " + name);
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

    public boolean isActiveByName(String gameName) {
        return activeSessions.keySet().stream()
                .anyMatch(key -> key.equalsIgnoreCase(gameName));
    }
}
