package dev.manel.gametracker.providers.steam;

import java.nio.file.Path;

public interface SteamLocationStrategy {
    Path getSteamRoot();
}
