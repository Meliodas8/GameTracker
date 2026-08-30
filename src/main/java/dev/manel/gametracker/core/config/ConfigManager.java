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

    public String getTheme() {
        return config.theme != null ? config.theme : "light";
    }

    /**
     * Juegos que el usuario ha quitado de las graficas. Solo afecta a la vista de
     * estadisticas: se siguen contando sesiones y siguen en Juegos e Historial.
     * La clave es el nombre porque es lo unico que guardan las sesiones.
     */
    public boolean isHiddenInStats(String gameName) {
        return config.statsHiddenGames.stream().anyMatch(g -> g.equalsIgnoreCase(gameName));
    }

    public void setHiddenInStats(String gameName, boolean hidden) {
        if (hidden == isHiddenInStats(gameName)) return;
        if (hidden) {
            config.statsHiddenGames.add(gameName);
        } else {
            config.statsHiddenGames.removeIf(g -> g.equalsIgnoreCase(gameName));
        }
        saveConfig();
    }

    public String getLanguage() {
        if (config.language == null) {
            config.language = I18n.systemLanguage();
            saveConfig();
        }
        return config.language;
    }

    public void setLanguage(String language) {
        config.language = language;
        saveConfig();
    }

    public void setTheme(String theme) {
        config.theme = theme;
        saveConfig();
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
        List<String> statsHiddenGames = new ArrayList<>();
        String theme = "light";
        String language = null;
    }
}
