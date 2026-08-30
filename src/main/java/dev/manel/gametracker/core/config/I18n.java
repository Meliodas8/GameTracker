package dev.manel.gametracker.core.config;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/** Textos y formatos dependientes del idioma. */
public final class I18n {

    public static final List<String> SUPPORTED = List.of("en", "es");
    private static final String BUNDLE = "dev.manel.gametracker.messages";

    private static Locale locale;
    private static ResourceBundle bundle;

    static {
        apply(ConfigManager.getInstance().getLanguage());
    }

    private I18n() {}

    /** Cambia el idioma en caliente. La UI ya cargada debe recargarse. */
    public static void apply(String language) {
        String lang = SUPPORTED.contains(language) ? language : "en";
        locale = Locale.of(lang);
        Locale.setDefault(locale);           // botones estándar de JavaFX, MessageFormat, WeekFields
        bundle = ResourceBundle.getBundle(BUNDLE, locale);
    }

    public static Locale locale() {
        return locale;
    }

    public static ResourceBundle bundle() {
        return bundle;
    }

    public static String get(String key, Object... args) {
        String value = bundle.getString(key);
        return args.length == 0 ? value : MessageFormat.format(value, args);
    }

    /** Idioma por defecto la primera vez: el del sistema si lo soportamos. */
    public static String systemLanguage() {
        String lang = Locale.getDefault().getLanguage();
        return SUPPORTED.contains(lang) ? lang : "en";
    }

    /** Fecha corta localizada (30/08/2026 vs 8/30/26). */
    public static String formatDate(TemporalAccessor temporal) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(temporal);
    }

    /** Día y mes, sin año, en el orden propio del idioma. */
    public static String formatDayMonth(Instant instant) {
        String pattern = java.time.format.DateTimeFormatterBuilder
                .getLocalizedDateTimePattern(FormatStyle.SHORT, null, java.time.chrono.IsoChronology.INSTANCE, locale)
                .replaceAll("[/.\\-]?\\s?y+[/.\\-]?", "")
                .trim();
        return DateTimeFormatter.ofPattern(pattern, locale)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    public static String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        if (hours > 0) return get("duration.hm", hours, minutes);
        if (minutes > 0) return get("duration.m", minutes);
        return get("duration.s", d.toSecondsPart());
    }
}
