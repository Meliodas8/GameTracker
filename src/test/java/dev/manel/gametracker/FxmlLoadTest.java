package dev.manel.gametracker;

import dev.manel.gametracker.core.config.I18n;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Carga cada FXML con su controlador real: pilla claves de traducción que faltan,
 * fx:id sin campo y handlers mal escritos, que en la app solo se ven al hacer clic.
 * Necesita servidor gráfico, así que se salta en CI.
 */
class FxmlLoadTest {

    @BeforeAll
    static void arrancarToolkit() throws InterruptedException {
        assumeTrue(System.getenv("DISPLAY") != null || System.getenv("WAYLAND_DISPLAY") != null,
                "sin servidor gráfico");
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            ready.countDown();
        }
        assertTrue(ready.await(20, TimeUnit.SECONDS), "el toolkit de JavaFX no arrancó");
    }

    @Test
    void todasLasVistasCargan() throws Exception {
        for (String lang : I18n.SUPPORTED) {
            I18n.apply(lang);
            for (String fxml : new String[]{"main", "game-list", "history", "stats", "settings"}) {
                cargarEnFxThread(fxml + ".fxml", lang);
            }
        }
    }

    private void cargarEnFxThread(String fxml, String lang) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                new FXMLLoader(getClass().getResource("/dev/manel/gametracker/" + fxml),
                        I18n.bundle()).load();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(30, TimeUnit.SECONDS), "timeout cargando " + fxml);
        if (error.get() != null) {
            fail(fxml + " (" + lang + ") no carga: " + error.get(), error.get());
        }
    }
}
