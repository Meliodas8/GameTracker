package dev.manel.gametracker.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ProcessUtils {

    /**
     * comm  : nombre corto del proceso (lo que ProcessWatcher compara por defecto)
     * exePath: ruta completa al ejecutable (para matching por ruta y para mostrar en la UI)
     *
     * El executableName del usuario se compara contra:
     *   1. comm exacto (case-insensitive)          → ej. "brave" matchea comm="brave"
     *   2. exePath contiene el string (case-insens) → ej. "intellij-idea" matchea
     *      "/opt/intellij-idea-ultimate-edition/jbr/bin/java"
     */
    public record ProcessInfo(String comm, String exePath) {
        /** Texto que se muestra en la lista del diálogo. */
        public String displayLabel() {
            if (exePath.isBlank() || exePath.equals(comm)) return comm;
            // Muestra las últimas partes de la ruta para dar contexto sin ocupar demasiado
            String normalized = exePath.replace('\\', '/');
            String[] parts = normalized.split("/");
            String suffix = parts.length >= 3
                    ? "…/" + parts[parts.length - 3] + "/" + parts[parts.length - 2] + "/" + parts[parts.length - 1]
                    : exePath;
            return comm + "   [" + suffix + "]";
        }

        /** Valor sugerido para el campo ejecutable al hacer clic en este proceso. */
        public String suggestedExecName() {
            return comm;
        }
    }

    private static final String OS = System.getProperty("os.name", "").toLowerCase();
    private static final boolean IS_WINDOWS = OS.contains("win");
    private static final boolean IS_LINUX   = OS.contains("linux") || OS.contains("nix");

    /**
     * Devuelve todos los procesos en ejecución con su comm y exePath.
     * - Windows : tasklist (comm) + ProcessHandle (exePath para procesos del usuario)
     * - Linux   : /proc/[pid]/{comm,exe}
     * - macOS   : ps (comm) + ProcessHandle (exePath para procesos del usuario)
     */
    public static Set<ProcessInfo> getRunningProcessInfos() {
        if (IS_LINUX)   return getInfosFromProc();
        if (IS_WINDOWS) return mergeWithProcessHandle(getInfosFromTasklist());
        return mergeWithProcessHandle(getInfosFromPs());  // macOS
    }

    /** Solo los nombres (comm), para compatibilidad con código que no necesita la ruta. */
    public static Set<String> getRunningProcessNames() {
        return getRunningProcessInfos().stream()
                .map(ProcessInfo::comm)
                .collect(Collectors.toSet());
    }

    /** Lista ordenada de ProcessInfo para mostrar en la UI. */
    public static List<ProcessInfo> getRunningProcessInfosSorted() {
        return getRunningProcessInfos().stream()
                .sorted(Comparator.comparing(ProcessInfo::comm, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ProcessInfo::exePath))
                .collect(Collectors.toList());
    }

    // ── Linux ─────────────────────────────────────────────────────────────────
    private static Set<ProcessInfo> getInfosFromProc() {
        try (var procDir = Files.list(Path.of("/proc"))) {
            return procDir
                    .filter(p -> p.getFileName().toString().matches("\\d+"))
                    .map(p -> {
                        String comm = "";
                        String exePath = "";
                        try { comm = Files.readString(p.resolve("comm")).trim(); }
                        catch (IOException ignored) {}
                        try { exePath = Files.readSymbolicLink(p.resolve("exe")).toString(); }
                        catch (IOException ignored) {}  // procesos de otros usuarios: no problem
                        return new ProcessInfo(comm, exePath);
                    })
                    .filter(info -> !info.comm().isBlank())
                    .collect(Collectors.toCollection(HashSet::new));
        } catch (IOException e) {
            return getInfosFromProcessHandle();  // fallback
        }
    }

    // ── Windows ───────────────────────────────────────────────────────────────
    // tasklist da nombres de todos los procesos; ProcessHandle añade rutas para los del usuario
    private static Set<ProcessInfo> getInfosFromTasklist() {
        try {
            Process p = new ProcessBuilder("tasklist", "/fo", "csv", "/nh").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                return reader.lines()
                        .filter(line -> !line.isBlank())
                        .map(line -> {
                            String raw = line.split(",")[0].replace("\"", "").trim();
                            if (raw.toLowerCase().endsWith(".exe"))
                                raw = raw.substring(0, raw.length() - 4);
                            return new ProcessInfo(raw, "");
                        })
                        .filter(info -> !info.comm().isBlank())
                        .collect(Collectors.toCollection(HashSet::new));
            }
        } catch (IOException e) {
            return getInfosFromProcessHandle();
        }
    }

    // ── macOS ─────────────────────────────────────────────────────────────────
    private static Set<ProcessInfo> getInfosFromPs() {
        try {
            Process p = new ProcessBuilder("ps", "-e", "-o", "comm=").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isBlank())
                        .map(line -> {
                            int lastSlash = line.lastIndexOf('/');
                            String comm = lastSlash >= 0 ? line.substring(lastSlash + 1) : line;
                            return new ProcessInfo(comm, "");
                        })
                        .collect(Collectors.toCollection(HashSet::new));
            }
        } catch (IOException e) {
            return getInfosFromProcessHandle();
        }
    }

    // ── Fallback / complemento de rutas ──────────────────────────────────────
    private static Set<ProcessInfo> getInfosFromProcessHandle() {
        return ProcessHandle.allProcesses()
                .map(p -> {
                    String cmd = p.info().command().orElse("");
                    if (cmd.isBlank()) return null;
                    int lastSlash = Math.max(cmd.lastIndexOf('/'), cmd.lastIndexOf('\\'));
                    String name = cmd.substring(lastSlash + 1);
                    if (name.toLowerCase().endsWith(".exe"))
                        name = name.substring(0, name.length() - 4);
                    return new ProcessInfo(name, cmd);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Combina infos de un método de nombres (sin rutas) con ProcessHandle
     * para añadir rutas a los procesos del usuario actual.
     */
    private static Set<ProcessInfo> mergeWithProcessHandle(Set<ProcessInfo> base) {
        Set<ProcessInfo> fromHandle = getInfosFromProcessHandle();
        // Para procesos que ya estaban en 'base' sin ruta, añade los que ProcessHandle sí tiene
        Set<String> baseNames = base.stream().map(ProcessInfo::comm).collect(Collectors.toSet());
        fromHandle.stream()
                .filter(info -> baseNames.contains(info.comm()) || !base.contains(info))
                .forEach(base::add);
        return base;
    }
}
