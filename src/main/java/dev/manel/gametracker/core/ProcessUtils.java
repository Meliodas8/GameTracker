package dev.manel.gametracker.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProcessUtils {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    /**
     * Devuelve los nombres de procesos en ejecución sin extensión ni ruta,
     * usando la misma lógica que ProcessWatcher para que los valores sean
     * directamente válidos como executableName.
     *
     * En Windows usa `tasklist` porque ProcessHandle.info().command() devuelve
     * vacío para muchos procesos sin privilegios de administrador.
     */
    public static Set<String> getRunningProcessNames() {
        if (IS_WINDOWS) {
            Set<String> fromTasklist = getFromTasklist();
            if (!fromTasklist.isEmpty()) return fromTasklist;
        }
        return getFromProcessHandle();
    }

    /** Versión ordenada para mostrar en la UI. */
    public static List<String> getRunningProcessNamesSorted() {
        return getRunningProcessNames().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    // tasklist /fo csv /nh → "chrome.exe","1234","Console","1","50,000 K"
    private static Set<String> getFromTasklist() {
        try {
            Process p = new ProcessBuilder("tasklist", "/fo", "csv", "/nh").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                return reader.lines()
                        .filter(line -> !line.isBlank())
                        .map(line -> {
                            // El primer campo siempre es el nombre del ejecutable, entre comillas
                            String raw = line.split(",")[0].replace("\"", "").trim();
                            if (raw.toLowerCase().endsWith(".exe")) {
                                raw = raw.substring(0, raw.length() - 4);
                            }
                            return raw;
                        })
                        .filter(name -> !name.isBlank())
                        .collect(Collectors.toSet());
            }
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    private static Set<String> getFromProcessHandle() {
        return ProcessHandle.allProcesses()
                .map(p -> p.info().command().orElse(""))
                .filter(cmd -> !cmd.isBlank())
                .map(cmd -> {
                    int lastSlash = Math.max(cmd.lastIndexOf('/'), cmd.lastIndexOf('\\'));
                    String name = cmd.substring(lastSlash + 1);
                    if (name.toLowerCase().endsWith(".exe")) {
                        name = name.substring(0, name.length() - 4);
                    }
                    return name;
                })
                .collect(Collectors.toSet());
    }
}
