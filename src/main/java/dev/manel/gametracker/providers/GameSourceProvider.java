package dev.manel.gametracker.providers;

import dev.manel.gametracker.core.model.DetectedGame;

import java.util.List;

public interface GameSourceProvider {
    String platformName();
    boolean isAvailable();
    List<DetectedGame> getInstalledGames();
}
