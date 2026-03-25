package dev.manel.gametracker.core.config;

import java.nio.file.Path;

public class LinuxPathStrategy implements PathStrategy{

    @Override
    public Path getConfigDir() {
        String xdgConfig = System.getenv("XDG_CONFIG_HOME");
        if (xdgConfig != null && !xdgConfig.isBlank()) {
            return Path.of(xdgConfig, "gametracker");
        }
        return Path.of(System.getProperty("user.home"), ".config", "gametracker");
    }

    @Override
    public Path getDataDir() {
        String xdgData = System.getenv("XDG_DATA_HOME");
        if (xdgData != null && !xdgData.isBlank()) {
            return Path.of(xdgData, "gametracker");
        }
        return Path.of(System.getProperty("user.home"), ".local", "share", "gametracker");
    }
}
