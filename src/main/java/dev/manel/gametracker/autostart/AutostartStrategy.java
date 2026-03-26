package dev.manel.gametracker.autostart;

public interface AutostartStrategy {
    void enable();
    void disable();
    boolean isEnabled();

    static AutostartStrategy detect() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return new WindowsAutostartStrategy();
        if (os.contains("mac")) return new MacAutostartStrategy();
        return new LinuxAutostartStrategy();
    }
}
