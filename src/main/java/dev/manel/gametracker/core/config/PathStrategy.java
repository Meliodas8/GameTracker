package dev.manel.gametracker.core.config;

import java.nio.file.Path;

public interface PathStrategy {
    Path getConfigDir();
    Path getDataDir();
}
