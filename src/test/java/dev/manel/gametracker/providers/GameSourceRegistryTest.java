package dev.manel.gametracker.providers;

import dev.manel.gametracker.core.model.DetectedGame;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameSourceRegistryTest {

    private static final DetectedGame HOLLOW =
            new DetectedGame("Hollow Knight", "STEAM", "hollow_knight", "367520");
    private static final DetectedGame BRAVE =
            new DetectedGame("Brave", "MANUAL", "brave", null);

    private record FakeProvider(boolean available, List<DetectedGame> games)
            implements GameSourceProvider {
        @Override public String platformName() { return "FAKE"; }
        @Override public boolean isAvailable() { return available; }
        @Override public List<DetectedGame> getInstalledGames() { return games; }
    }

    private GameSourceRegistry registryWith(GameSourceProvider... providers) {
        GameSourceRegistry registry = new GameSourceRegistry();
        for (GameSourceProvider p : providers) registry.register(p);
        return registry;
    }

    @Test
    void juntaLoDeTodosLosProveedores() {
        assertEquals(List.of(HOLLOW, BRAVE), registryWith(
                new FakeProvider(true, List.of(HOLLOW)),
                new FakeProvider(true, List.of(BRAVE))).getAllGames());
    }

    @Test
    void unProveedorNoDisponibleNoAporta() {
        assertEquals(List.of(BRAVE), registryWith(
                new FakeProvider(false, List.of(HOLLOW)),
                new FakeProvider(true, List.of(BRAVE))).getAllGames());
    }

    @Test
    void sinProveedoresNoHayJuegos() {
        assertTrue(registryWith().getAllGames().isEmpty());
    }
}
