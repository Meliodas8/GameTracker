package dev.manel.gametracker.providers.steam;

import java.nio.file.Path;

public class MacSteamLocation implements SteamLocationStrategy {

    @Override
    public Path getSteamRoot() {
        return Path.of(System.getProperty("user.home"),
                "Library", "Application Support", "Steam");
    }
}
