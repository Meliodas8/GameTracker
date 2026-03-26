package dev.manel.gametracker.session;

import dev.manel.gametracker.core.model.DetectedGame;
import dev.manel.gametracker.providers.GameSourceRegistry;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ProcessWatcher {

    private static final int SCAN_INTERVAL_SECONDS = 5;

    private final GameSourceRegistry registry;
    private final SessionManager sessionManager;
    private final ScheduledExecutorService scheduler;

    public ProcessWatcher(GameSourceRegistry registry, SessionManager sessionManager) {
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "process-watcher");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::scan, 0, SCAN_INTERVAL_SECONDS, TimeUnit.SECONDS);
        System.out.println("ProcessWatcher iniciado");
    }

    public void stop() {
        scheduler.shutdown();
        System.out.println("ProcessWatcher detenido");
    }

    private void scan() {
        try {
            Set<String> runningProcesses = getRunningProcessNames();
            List<DetectedGame> knownGames = registry.getAllGames();

            for (DetectedGame game : knownGames) {
                boolean isRunning = runningProcesses.stream()
                        .anyMatch(p -> p.equalsIgnoreCase(game.executableName()));

                if (isRunning && !sessionManager.isActive(game)) {
                    sessionManager.startSession(game);
                } else if (!isRunning && sessionManager.isActive(game)) {
                    sessionManager.endSession(game);
                }
            }
        } catch (Exception e) {
            System.err.println("Error en scan: " + e.getMessage());
        }
    }

    private Set<String> getRunningProcessNames() {
        return ProcessHandle.allProcesses()
                .map(p -> p.info().command().orElse(""))
                .filter(cmd -> !cmd.isBlank())
                .map(cmd -> {
                    int lastSlash = Math.max(cmd.lastIndexOf('/'), cmd.lastIndexOf('\\'));
                    String name = cmd.substring(lastSlash + 1);
                    // quita la extensión .exe en Windows
                    if (name.toLowerCase().endsWith(".exe")) {
                        name = name.substring(0, name.length() - 4);
                    }
                    return name;
                })
                .collect(Collectors.toSet());
    }
}
