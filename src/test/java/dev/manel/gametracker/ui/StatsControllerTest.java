package dev.manel.gametracker.ui;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** El ancho de barra elegido para cada rango: 365 barras diarias no se leen. */
class StatsControllerTest {

    private static final LocalDate HOY = LocalDate.of(2026, 8, 30);

    private static StatsController.Bucket paraUltimos(int dias) {
        return StatsController.bucketFor(HOY.minusDays(dias - 1L), HOY);
    }

    @Test
    void losRangosCortosVanPorDia() {
        assertEquals(StatsController.Bucket.DAY, paraUltimos(1));
        assertEquals(StatsController.Bucket.DAY, paraUltimos(7));
        assertEquals(StatsController.Bucket.DAY, paraUltimos(30));
        assertEquals(StatsController.Bucket.DAY, paraUltimos(45));
    }

    @Test
    void mesesYUnAnyoVanPorMes() {
        assertEquals(StatsController.Bucket.MONTH, paraUltimos(46));
        assertEquals(StatsController.Bucket.MONTH, paraUltimos(182));
        assertEquals(StatsController.Bucket.MONTH, paraUltimos(365));
    }

    @Test
    void unHistoricoMuyLargoPasaAAnyos() {
        assertEquals(StatsController.Bucket.MONTH, paraUltimos(3 * 366));
        assertEquals(StatsController.Bucket.YEAR, paraUltimos(3 * 366 + 1));
        assertEquals(StatsController.Bucket.YEAR,
                StatsController.bucketFor(LocalDate.of(2015, 1, 1), HOY));
    }
}
