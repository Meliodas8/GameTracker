package dev.manel.gametracker.autostart;

import java.io.IOException;
import java.nio.file.Path;

public class WindowsAutostartStrategy implements AutostartStrategy {

    private static final String REG_KEY =
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";

    @Override
    public boolean isEnabled() {
        try {
            Process p = new ProcessBuilder("reg", "query", REG_KEY,
                    "/v", "GameTracker").start();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Override
    public void enable() {
        try {
            String javaPath = getJavaPath();
            String jarPath = Path.of(System.getenv("LOCALAPPDATA"),
                    "gametracker", "GameTracker.jar").toString();

            String command = "\"%s\" -jar \"%s\" --daemon"
                    .formatted(javaPath, jarPath);

            new ProcessBuilder("reg", "add", REG_KEY,
                    "/v", "GameTracker",
                    "/t", "REG_SZ",
                    "/d", command,
                    "/f").start().waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void disable() {
        try {
            new ProcessBuilder("reg", "delete", REG_KEY,
                    "/v", "GameTracker", "/f").start().waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private String getJavaPath() {
        // primero intenta obtener la ruta completa del proceso actual
        String fromProcess = ProcessHandle.current()
                .info().command().orElse(null);
        if (fromProcess != null) return fromProcess;

        // fallback: busca java en JAVA_HOME
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            return Path.of(javaHome, "bin", "javaw.exe").toString();
        }

        // último recurso
        return "javaw.exe";
    }
}