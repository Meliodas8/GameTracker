package dev.manel.gametracker.session;

import dev.manel.gametracker.core.model.DetectedGame;
import dev.manel.gametracker.core.model.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cubre los fallos que ya se dieron en producción: sesiones duplicadas,
 * sesiones perdidas al apagar y sesiones fantasma de un solo ciclo de escaneo.
 */
class SessionManagerTest {

    /** Reloj que solo avanza cuando el test lo dice. */
    private static class TestClock extends Clock {
        private Instant now = Instant.parse("2026-08-30T10:00:00Z");
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    private static final DetectedGame HOLLOW =
            new DetectedGame("Hollow Knight", "STEAM", "hollow_knight", "367520");
    private static final DetectedGame BRAVE =
            new DetectedGame("Brave", "MANUAL", "brave", null);

    @TempDir Path tmp;
    private Path file;
    private TestClock clock;
    private SessionManager manager;

    /** Más largo que un ciclo de escaneo: por debajo, la sesión se descarta. */
    private static final Duration REAL = Duration.ofSeconds(ProcessWatcher.SCAN_INTERVAL_SECONDS + 1);

    @BeforeEach
    void setUp() {
        file = tmp.resolve("sessions.json");
        clock = new TestClock();
        manager = new SessionManager(file, clock);
    }

    @Test
    void unaPartidaJugadaSeGuardaYSeRelee() throws IOException {
        manager.startSession(HOLLOW);
        clock.advance(Duration.ofMinutes(90));
        manager.endSession(HOLLOW);

        List<GameSession> sessions = manager.getCompletedSessions();
        assertEquals(1, sessions.size());
        assertEquals("Hollow Knight", sessions.get(0).gameName());
        assertEquals(Duration.ofMinutes(90), sessions.get(0).duration());
        assertFalse(manager.isActive(HOLLOW));

        assertTrue(Files.exists(file), "la sesión debe persistirse al cerrarla");
        SessionManager recargado = new SessionManager(file, clock);
        assertEquals(1, recargado.getCompletedSessions().size());
        assertEquals(Duration.ofMinutes(90), recargado.getCompletedSessions().get(0).duration());
    }

    @Test
    void arrancarDosVecesElMismoJuegoNoDuplicaLaSesion() {
        manager.startSession(HOLLOW);
        clock.advance(Duration.ofMinutes(10));
        manager.startSession(HOLLOW);   // un segundo escaneo ve el proceso otra vez
        clock.advance(Duration.ofMinutes(10));
        manager.endSession(HOLLOW);

        assertEquals(1, manager.getCompletedSessions().size());
        // el inicio es el primero: el segundo start no debe reiniciar el contador
        assertEquals(Duration.ofMinutes(20), manager.getCompletedSessions().get(0).duration());
    }

    @Test
    void unaSesionMasCortaQueUnCicloDeEscaneoSeDescarta() {
        manager.startSession(HOLLOW);
        clock.advance(Duration.ofSeconds(ProcessWatcher.SCAN_INTERVAL_SECONDS));
        manager.endSession(HOLLOW);

        assertTrue(manager.getCompletedSessions().isEmpty(),
                "los parpadeos de detección no deben ensuciar el histórico");
    }

    @Test
    void cerrarSinHaberArrancadoNoHaceNada() {
        manager.endSession(HOLLOW);
        assertTrue(manager.getCompletedSessions().isEmpty());
        assertFalse(Files.exists(file), "no hay nada que guardar");
    }

    @Test
    void apagarConSesionesAbiertasLasGuardaTodas() {
        manager.startSession(HOLLOW);
        manager.startSession(BRAVE);
        clock.advance(Duration.ofMinutes(45));

        manager.endAllActive();

        assertEquals(2, manager.getCompletedSessions().size());
        assertFalse(manager.isActive(HOLLOW));
        assertFalse(manager.isActive(BRAVE));
        assertEquals(2, new SessionManager(file, clock).getCompletedSessions().size(),
                "apagar debe persistir, no solo vaciar la memoria");
    }

    @Test
    void apagarSinSesionesAbiertasNoTocaElFichero() {
        manager.endAllActive();
        assertFalse(Files.exists(file));
    }

    @Test
    void juegoEnCursoSeReconocePorNombreDeEjecutable() {
        assertFalse(manager.isActiveByName("brave"));
        manager.startSession(BRAVE);
        assertTrue(manager.isActiveByName("brave"));
        assertTrue(manager.isActiveByName("BRAVE"), "la comparación ignora mayúsculas");
        clock.advance(REAL);
        manager.endSession(BRAVE);
        assertFalse(manager.isActiveByName("brave"));
    }

    @Test
    void dosJuegosALaVezSeCuentanPorSeparado() {
        manager.startSession(HOLLOW);
        clock.advance(Duration.ofMinutes(30));
        manager.startSession(BRAVE);
        clock.advance(Duration.ofMinutes(30));
        manager.endSession(HOLLOW);
        clock.advance(Duration.ofMinutes(30));
        manager.endSession(BRAVE);

        List<GameSession> sessions = manager.getCompletedSessions();
        assertEquals(2, sessions.size());
        assertEquals(Duration.ofMinutes(60),
                sessions.stream().filter(s -> s.gameName().equals("Hollow Knight"))
                        .findFirst().orElseThrow().duration());
        assertEquals(Duration.ofMinutes(60),
                sessions.stream().filter(s -> s.gameName().equals("Brave"))
                        .findFirst().orElseThrow().duration());
    }

    @Test
    void unSessionsJsonCorruptoNoTumbaLaCargaYSeAparta() throws IOException {
        Files.writeString(file, "{ esto no es json valido");

        SessionManager recuperado = assertDoesNotThrow(() -> new SessionManager(file, clock));
        assertTrue(recuperado.getCompletedSessions().isEmpty());

        Path apartado = tmp.resolve("sessions.json.corrupt");
        assertTrue(Files.exists(apartado), "el fichero ilegible se conserva para inspección");
        assertFalse(Files.exists(file), "y se quita de en medio para poder volver a escribir");

        // y a partir de ahí se puede seguir jugando y guardando con normalidad
        recuperado.startSession(HOLLOW);
        clock.advance(REAL);
        recuperado.endSession(HOLLOW);
        assertEquals(1, recuperado.getCompletedSessions().size());
    }
}
