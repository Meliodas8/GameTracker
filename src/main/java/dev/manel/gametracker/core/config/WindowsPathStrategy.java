package dev.manel.gametracker.core.config;

import java.nio.file.Path;

public class WindowsPathStrategy implements PathStrategy{

    @Override
    public Path getConfigDir() {
        return Path.of(System.getenv("APPDATA"), "gametracker");
    }

    @Override
    public Path getDataDir() {
        return Path.of(System.getenv("LOCALAPPDATA"), "gametracker");
    }
}
