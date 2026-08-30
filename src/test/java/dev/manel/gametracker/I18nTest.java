package dev.manel.gametracker;

import dev.manel.gametracker.core.config.I18n;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class I18nTest {

    private static ResourceBundle bundleFor(String lang) {
        return ResourceBundle.getBundle("dev.manel.gametracker.messages", Locale.of(lang));
    }

    @Test
    void todosLosIdiomasTienenLasMismasClaves() {
        Set<String> en = bundleFor("en").keySet();
        Set<String> es = bundleFor("es").keySet();
        assertEquals(en, es, "las claves de en/es deben coincidir");
        assertFalse(en.isEmpty());
    }

    @Test
    void lasClavesUsadasEnLosFxmlExisten() throws IOException {
        Path dir = Path.of("src/main/resources/dev/manel/gametracker");
        Pattern key = Pattern.compile("\"%([\\w.]+)\"");
        Set<String> used = new HashSet<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                Matcher m = key.matcher(Files.readString(f));
                while (m.find()) used.add(m.group(1));
            }
        }
        assertFalse(used.isEmpty(), "no se encontró ninguna clave en los FXML");
        Set<String> known = bundleFor("en").keySet();
        for (String k : used) {
            assertTrue(known.contains(k), "clave usada en FXML pero ausente del bundle: " + k);
        }
    }

    @Test
    void fechasYDuracionesCambianConElIdioma() {
        Instant instant = Instant.parse("2026-08-30T10:15:00Z");

        I18n.apply("es");
        String fechaEs = I18n.formatDate(instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        String diaMesEs = I18n.formatDayMonth(instant);

        I18n.apply("en");
        String fechaEn = I18n.formatDate(instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        String diaMesEn = I18n.formatDayMonth(instant);

        assertNotEquals(fechaEs, fechaEn, "la fecha corta debe seguir el orden del idioma");
        assertFalse(diaMesEs.contains("2026"), "día/mes no debe llevar año: " + diaMesEs);
        assertFalse(diaMesEn.contains("2026"), "día/mes no debe llevar año: " + diaMesEn);
        assertTrue(List.of(diaMesEs, diaMesEn).stream().allMatch(s -> s.matches(".*\\d.*")));

        assertEquals("2h 5m", I18n.formatDuration(Duration.ofMinutes(125)));
        assertEquals("Week 34 · 2026", I18n.get("history.week", 34, "2026"));

        I18n.apply("es");
        assertEquals("2h 5m", I18n.formatDuration(Duration.ofMinutes(125)));
        // el año va como String: con int, MessageFormat lo escribiría "2.026"
        assertEquals("Semana 34 · 2026", I18n.get("history.week", 34, "2026"));
    }
}
