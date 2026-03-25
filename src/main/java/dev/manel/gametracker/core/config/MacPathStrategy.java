package dev.manel.gametracker.core.config;

import java.nio.file.Path;

public class MacPathStrategy implements PathStrategy {

    @Override
    public Path getConfigDir() {
        return Path.of(System.getProperty("user.home"),
                "Library", "Application Support", "gametracker");
    }

    @Override
    public Path getDataDir() {
        return getConfigDir(); // en Mac config y datos van juntos
    }
}
