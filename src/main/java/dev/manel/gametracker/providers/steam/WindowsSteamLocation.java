package dev.manel.gametracker.providers.steam;

import java.nio.file.Path;

public class WindowsSteamLocation implements SteamLocationStrategy {

    @Override
    public Path getSteamRoot() {
        String programFiles = System.getenv("ProgramFiles(x86)");
        if (programFiles == null) {
            programFiles = System.getenv("ProgramFiles");
        }
        return Path.of(programFiles, "Steam");
    }
}
