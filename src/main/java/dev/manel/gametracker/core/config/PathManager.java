package dev.manel.gametracker.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathManager {

    private static final PathManager INSTANCE = new PathManager();
    private final PathStrategy strategy;

    private PathManager() {
        this.strategy = detectStrategy();
    }

    public static PathManager getInstance() {
        return INSTANCE;
    }

    private static PathStrategy detectStrategy() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))  return new WindowsPathStrategy();
        if (os.contains("mac"))  return new MacPathStrategy();
        return new LinuxPathStrategy();
    }

    public Path getConfigDir() {
        return ensureExists(strategy.getConfigDir());
    }

    public Path getDataDir() {
        return ensureExists(strategy.getDataDir());
    }

    public Path getConfigFile() {
        return getConfigDir().resolve("config.json");
    }

    public Path getSessionsFile() {
        return getDataDir().resolve("sessions.json");
    }

    private Path ensureExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio: " + dir, e);
        }
        return dir;
    }
}
