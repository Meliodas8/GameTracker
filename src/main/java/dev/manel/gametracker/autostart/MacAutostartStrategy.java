package dev.manel.gametracker.autostart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MacAutostartStrategy implements AutostartStrategy {

    @Override
    public boolean isEnabled() {
        return Files.exists(getPlistPath());
    }

    @Override
    public void enable() {
        try {
            String javaPath = ProcessHandle.current()
                    .info().command().orElse("java");
            String jarPath = Path.of(System.getProperty("user.home"),
                    "Library", "Application Support",
                    "gametracker", "GameTracker.jar").toString();

            String plist = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
                  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                <dict>
                    <key>Label</key>
                    <string>dev.manel.gametracker</string>
                    <key>ProgramArguments</key>
                    <array>
                        <string>/Library/Java/path with spaces/java</string>
                        <string>-jar</string>
                        <string>/Users/user/path with spaces/GameTracker.jar</string>
                        <string>--daemon</string>
                    </array>
                    <key>RunAtLoad</key>
                    <true/>
                </dict>
                </plist>
                """.formatted(javaPath, jarPath);

            Path launchDir = Path.of(System.getProperty("user.home"),
                    "Library", "LaunchAgents");
            Files.createDirectories(launchDir);
            Files.writeString(getPlistPath(), plist);

            new ProcessBuilder("launchctl", "load", getPlistPath().toString())
                    .start().waitFor();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void disable() {
        try {
            new ProcessBuilder("launchctl", "unload", getPlistPath().toString())
                    .start().waitFor();
            Files.deleteIfExists(getPlistPath());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private Path getPlistPath() {
        return Path.of(System.getProperty("user.home"),
                "Library", "LaunchAgents", "dev.manel.gametracker.plist");
    }
}
