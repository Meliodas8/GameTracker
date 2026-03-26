package dev.manel.gametracker.providers;

import dev.manel.gametracker.core.model.DetectedGame;
import dev.manel.gametracker.providers.steam.LinuxSteamLocation;
import dev.manel.gametracker.providers.steam.MacSteamLocation;
import dev.manel.gametracker.providers.steam.SteamLocationStrategy;
import dev.manel.gametracker.providers.steam.WindowsSteamLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SteamProvider implements GameSourceProvider{

    private final SteamLocationStrategy location;

    public SteamProvider() {
        this.location = detectStrategy();
    }

    private static SteamLocationStrategy detectStrategy() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return new WindowsSteamLocation();
        if (os.contains("mac")) return new MacSteamLocation();
        return new LinuxSteamLocation();
    }

    @Override
    public String platformName() {
        return "STEAM";
    }

    @Override
    public boolean isAvailable() {
        return Files.exists(location.getSteamRoot());
    }

    public List<Path> getLibraryPaths() {
        Path vdfPath = location.getSteamRoot().resolve("steamapps/libraryfolders.vdf");
        Set<Path> paths = new LinkedHashSet<>();
        paths.add(location.getSteamRoot().resolve("steamapps"));

        try {
            for (String line : Files.readAllLines(vdfPath)) {
                String trimmed = line.strip();
                if (trimmed.startsWith("\"path\"")) {
                    String path = trimmed.replaceAll("\"path\"\\s+\"(.+)\"", "$1");
                    path = path.replace("\\\\", "\\"); // normaliza \\ a \ en Windows
                    paths.add(Path.of(path, "steamapps"));
                }
            }
        } catch (IOException e) {}

        return new ArrayList<>(paths);
    }

    private boolean isRealGame(Map<String, String> fields) {
        String name = fields.getOrDefault("name", "").toLowerCase();
        String[] filters = {
                "proton", "steam linux runtime", "steamworks",
                "redistributable", "runtime"
        };
        for (String filter : filters) {
            if (name.contains(filter)) return false;
        }
        return true;
    }

    private Optional<DetectedGame> parseManifest(Path acfPath) {
        try {
            Map<String, String> fields = new HashMap<>();
            Pattern pattern = Pattern.compile("\"(\\w+)\"\\s+\"([^\"]+)\"");

            for (String line : Files.readAllLines(acfPath)) {
                Matcher m = pattern.matcher(line.strip());
                if (m.matches()) {
                    fields.put(m.group(1), m.group(2));
                }
            }

            String name = fields.get("name");
            String appId = fields.get("appid");
            String installDir = fields.get("installdir");

            if (name == null || appId == null) return Optional.empty();
            if (!isRealGame(fields)) return Optional.empty();

            return Optional.of(new DetectedGame(
                    name,
                    "STEAM",
                    installDir != null ? installDir.toLowerCase() : appId,
                    appId
            ));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<DetectedGame> getInstalledGames() {
        return getLibraryPaths().stream()
                .filter(Files::isDirectory)
                .flatMap(lib -> {
                    try {
                        return Files.list(lib)
                                .filter(p -> p.getFileName().toString().startsWith("appmanifest_"))
                                .filter(p -> p.toString().endsWith(".acf"));

                    } catch (IOException e) {
                        return Stream.empty();
                    }
                })
                .map(this::parseManifest)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
