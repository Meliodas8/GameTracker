package dev.manel.gametracker.session;

import dev.manel.gametracker.core.ProcessUtils;
import dev.manel.gametracker.core.config.PathManager;
import dev.manel.gametracker.core.model.DetectedGame;
import dev.manel.gametracker.providers.GameSourceRegistry;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProcessWatcher {

    static final int SCAN_INTERVAL_SECONDS = 5;

    private final GameSourceRegistry registry;
    private final SessionManager sessionManager;
    private final ScheduledExecutorService scheduler;

    // Se mantienen abiertos mientras viva el proceso: cerrarlos libera el lock.
    private FileChannel lockChannel;
    private FileLock lock;

    public ProcessWatcher(GameSourceRegistry registry, SessionManager sessionManager) {
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "process-watcher");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Arranca la vigilancia solo si esta instancia consigue el lock exclusivo.
     * Dos vigilantes a la vez (daemon + GUI) llevan cada uno su propia lista de
     * sesiones en memoria y reescriben sessions.json entero: gana el ultimo en
     * escribir y el otro pierde sus sesiones sin avisar.
     */
    public void start() {
        if (!acquireLock()) {
            System.out.println("Ya hay otra instancia vigilando procesos; "
                    + "esta no contara sesiones");
            return;
        }
        scheduler.scheduleAtFixedRate(this::scan, 0, SCAN_INTERVAL_SECONDS, TimeUnit.SECONDS);
        System.out.println("ProcessWatcher iniciado");
    }

    public void stop() {
        scheduler.shutdown();
        sessionManager.endAllActive();
        releaseLock();
        System.out.println("ProcessWatcher detenido");
    }

    private boolean acquireLock() {
        try {
            lockChannel = FileChannel.open(
                    PathManager.getInstance().getDataDir().resolve("watcher.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = lockChannel.tryLock();   // null si otro proceso lo tiene
            if (lock == null) {
                lockChannel.close();
                lockChannel = null;
            }
            return lock != null;
        } catch (IOException e) {
            // Sin lock utilizable, es mas seguro no vigilar que duplicar sesiones
            System.err.println("No se pudo obtener el lock: " + e.getMessage());
            return false;
        }
    }

    private void releaseLock() {
        try {
            if (lock != null) lock.release();
            if (lockChannel != null) lockChannel.close();
        } catch (IOException e) {
            System.err.println("Error liberando el lock: " + e.getMessage());
        }
    }

    private void scan() {
        try {
            Set<ProcessUtils.ProcessInfo> runningProcesses = ProcessUtils.getRunningProcessInfos();
            Set<String> runningSteamAppIds = getRunningSteamAppIds();
            List<DetectedGame> knownGames = registry.getAllGames();

            for (DetectedGame game : knownGames) {
                String execName = game.executableName().toLowerCase();
                boolean isRunning = runningProcesses.stream().anyMatch(info ->
                                info.comm().equalsIgnoreCase(game.executableName())
                                || (!info.exePath().isBlank()
                                    && info.exePath().toLowerCase().contains(execName)))
                        || ("STEAM".equals(game.platform())
                            && game.platformId() != null
                            && runningSteamAppIds.contains(game.platformId()));

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

    // Detecta juegos de Steam corriendo bajo Proton buscando procesos "reaper"
    // que contienen el AppId en sus argumentos: reaper SteamLaunch AppId=XXXXX --
    private Set<String> getRunningSteamAppIds() {
        return ProcessHandle.allProcesses()
                .filter(p -> p.info().command()
                        .map(cmd -> cmd.endsWith("/reaper") || cmd.endsWith("\\reaper.exe"))
                        .orElse(false))
                .flatMap(p -> {
                    String cmdLine = p.info().commandLine().orElse("");
                    int idx = cmdLine.indexOf("AppId=");
                    if (idx < 0) return Stream.empty();
                    String rest = cmdLine.substring(idx + 6);
                    int end = rest.indexOf(' ');
                    String appId = end > 0 ? rest.substring(0, end) : rest;
                    return appId.isBlank() ? Stream.empty() : Stream.of(appId.trim());
                })
                .collect(Collectors.toSet());
    }
}
