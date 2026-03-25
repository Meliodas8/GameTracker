package dev.manel.gametracker.providers;

import dev.manel.gametracker.core.config.ConfigManager;
import dev.manel.gametracker.core.model.DetectedGame;

import java.util.List;

public class ManualProvider implements GameSourceProvider {
    @Override
    public String platformName() {
        return "MANUAL";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<DetectedGame> getInstalledGames() {
        return ConfigManager.getInstance().getManualApps();
    }
}
