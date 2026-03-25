package dev.manel.gametracker.providers;

import dev.manel.gametracker.core.model.DetectedGame;

import java.util.ArrayList;
import java.util.List;

public class GameSourceRegistry {
    private final List<GameSourceProvider> providers = new ArrayList<>();

    public void register(GameSourceProvider provider) {
        providers.add(provider);
    }

    public List<DetectedGame> getAllGames() {
        return providers.stream()
                .filter(GameSourceProvider::isAvailable)
                .flatMap(provider -> provider.getInstalledGames().stream())
                .toList();
    }
}
