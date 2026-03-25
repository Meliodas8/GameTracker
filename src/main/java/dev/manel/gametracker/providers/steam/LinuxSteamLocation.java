package dev.manel.gametracker.providers.steam;

import java.nio.file.Files;
import java.nio.file.Path;

public class LinuxSteamLocation implements SteamLocationStrategy {

    @Override
    public Path getSteamRoot() {
        Path native_ = Path.of(System.getProperty("user.home"), ".steam", "steam");
        if (Files.exists(native_)) return native_;

        Path flatpak = Path.of(System.getProperty("user.home"),
                ".var", "app", "com.valvesoftware.Steam", ".steam", "steam");
        if (Files.exists(flatpak)) return flatpak;

        return native_;
    }
}
