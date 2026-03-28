package dev.manel.gametracker.autostart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LinuxAutostartStrategy implements AutostartStrategy {

    @Override
    public boolean isEnabled() {
        return Files.exists(getServicePath());
    }

    @Override
    public void enable() {
        try {
            String execStart = buildExecStart();

            String service = """
                [Unit]
                Description=GameTracker daemon
                After=graphical-session.target

                [Service]
                Type=simple
                ExecStart=%s
                Restart=on-failure
                RestartSec=5

                [Install]
                WantedBy=default.target
            """.formatted(execStart);

            Path serviceDir = Path.of(System.getProperty("user.home"),
                    ".config", "systemd", "user");
            Files.createDirectories(serviceDir);
            Files.writeString(getServicePath(), service);

            new ProcessBuilder("systemctl", "--user", "daemon-reload")
                    .start().waitFor();
            new ProcessBuilder("systemctl", "--user", "enable",
                    "--now", "gametracker.service").start().waitFor();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void disable() throws RuntimeException {
        try {
            new ProcessBuilder("systemctl", "--user", "disable",
                    "--now", "gametracker.service").start().waitFor();
            Files.deleteIfExists(getServicePath());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private String buildExecStart() {
        // .deb (jpackage): launcher con JRE bundleado
        Path debLauncher = Path.of("/opt/gametracker/bin/GameTracker");
        if (Files.exists(debLauncher)) {
            return debLauncher + " --daemon";
        }
        // AppImage instalado via yay/paquete
        Path appImageLauncher = Path.of("/usr/bin/gametracker");
        if (Files.exists(appImageLauncher)) {
            return appImageLauncher + " --daemon";
        }
        // Fallback: modo desarrollo (java + jar)
        String javaPath = ProcessHandle.current().info().command().orElse("java");
        String jarPath = Path.of(System.getProperty("user.home"),
                ".local", "share", "gametracker", "GameTracker.jar").toString();
        return "\"" + javaPath + "\" -jar \"" + jarPath + "\" --daemon";
    }

    private Path getServicePath() {
        return Path.of(System.getProperty("user.home"),
                ".config", "systemd", "user", "gametracker.service");
    }
}