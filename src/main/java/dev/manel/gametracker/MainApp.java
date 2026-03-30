package dev.manel.gametracker;

import dev.manel.gametracker.providers.GameSourceRegistry;
import dev.manel.gametracker.providers.ManualProvider;
import dev.manel.gametracker.providers.SteamProvider;
import dev.manel.gametracker.session.ProcessWatcher;
import dev.manel.gametracker.session.SessionManager;
import javafx.application.Application;

public class MainApp {
    public static void main(String[] args) {
        if (isDaemonMode(args)) {
            startDaemon();
        } else {
            Application.launch(GameTrackerApp.class, args);
        }
    }

    private static boolean isDaemonMode(String[] args) {
        for (String arg : args) {
            if (arg.equals("--daemon")) return true;
        }
        return false;
    }

    private static void startDaemon() {
        System.out.println("GameTracker arrancando en modo daemon...");

        GameSourceRegistry registry = new GameSourceRegistry();
        registry.register(new SteamProvider());
        registry.register(new ManualProvider());

        ProcessWatcher watcher = new ProcessWatcher(registry, SessionManager.getInstance());
        watcher.start();

        // mantiene el proceso vivo hasta que reciba SIGTERM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Apagando GameTracker...");
            watcher.stop();
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

