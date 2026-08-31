package dev.manel.gametracker.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessWatcherTest {

    @Test
    void discoNoMatcheaDiscord() {
        assertFalse(ProcessWatcher.pathMatches(
                "/home/user/.config/discord/app-1.0.155/Discord", "disco"));
    }

    @Test
    void matcheaElEjecutableDelJuego() {
        assertTrue(ProcessWatcher.pathMatches(
                "/mnt/games/SteamLibrary/steamapps/common/Disco Elysium/disco.exe", "disco"));
    }

    @Test
    void matcheaRutaConSufijoNoAlfanumerico() {
        assertTrue(ProcessWatcher.pathMatches(
                "/opt/intellij-idea-ultimate/jbr/bin/java", "intellij-idea"));
    }

    @Test
    void rutaVaciaNoMatchea() {
        assertFalse(ProcessWatcher.pathMatches("", "disco"));
    }
}
