package dev.manel.gametracker.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.manel.gametracker.core.model.DetectedGame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private static final ConfigManager INSTANCE = new ConfigManager();

    private final Gson gson;
    private final Path configFile;
    private Config config;

    private ConfigManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configFile = PathManager.getInstance().getConfigFile();
        this.config = loadConfig();
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    public List<DetectedGame> getManualApps() {
        return List.copyOf(config.manualApps);
    }

    public void addManualApp(DetectedGame game) {
        config.manualApps.add(game);
        saveConfig();
    }

    public void removeManualApp(String executableName) {
        config.manualApps.removeIf(g -> g.executableName().equals(executableName));
        saveConfig();
    }

    private Config loadConfig() {
        if (!Files.exists(configFile)) return new Config();
        try {
            String json = Files.readString(configFile);
            Config loaded = gson.fromJson(json, Config.class);
            return loaded != null ? loaded : new Config();
        } catch (IOException e) {
            System.err.println("Error cargando config: " + e.getMessage());
            return new Config();
        }
    }

    private void saveConfig() {
        try {
            Files.writeString(configFile, gson.toJson(config));
        } catch (IOException e) {
            System.err.println("Error guardando config: " + e.getMessage());
        }
    }

    // clase interna que representa la estructura del JSON
    private static class Config {
        List<DetectedGame> manualApps = new ArrayList<>();
    }
}
