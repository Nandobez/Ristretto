package dev.nandobez.ristretto.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

/** Tracks a detached app process for the current project under .ristretto/. */
public class ProjectProc {

    public static Path stateDir() throws IOException {
        Path d = Path.of(".ristretto");
        Files.createDirectories(d);
        ignore(".ristretto/");
        return d;
    }

    /** Append an entry to .gitignore if the file exists and doesn't already list it. */
    private static void ignore(String entry) {
        try {
            Path gi = Path.of(".gitignore");
            if (!Files.exists(gi)) return;
            if (Files.readString(gi).lines().anyMatch(l -> l.trim().equals(entry.trim()))) return;
            Files.writeString(gi, System.lineSeparator() + entry + System.lineSeparator(),
                StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    public static Path pidFile() { return Path.of(".ristretto", "app.pid"); }
    public static Path logFile() { return Path.of(".ristretto", "app.log"); }
    public static Path startFile() { return Path.of(".ristretto", "app.start"); }
    public static Path metaFile() { return Path.of(".ristretto", "app.meta"); }

    /** Reads the running app's port from the meta file, or a fallback. */
    public static int readPort(int fallback) {
        try { return Integer.parseInt(Files.readString(metaFile()).lines().findFirst().orElse("").trim()); }
        catch (Exception e) { return fallback; }
    }

    /** Reads the running app's profile from the meta file, or a fallback. */
    public static String readProfile(String fallback) {
        try { return Files.readString(metaFile()).lines().skip(1).findFirst().orElse(fallback).trim(); }
        catch (Exception e) { return fallback; }
    }

    /** Compact human uptime from a start-epoch file, or null if unavailable. */
    public static String uptime() {
        try {
            long ms = System.currentTimeMillis() - Long.parseLong(Files.readString(startFile()).trim());
            long s = ms / 1000;
            if (s < 60) return s + "s";
            long m = s / 60; s %= 60;
            if (m < 60) return m + "m" + s + "s";
            long h = m / 60; m %= 60;
            return h + "h" + m + "m";
        } catch (Exception e) { return null; }
    }

    public static Long readPid() {
        try { return Long.parseLong(Files.readString(pidFile()).trim()); }
        catch (Exception e) { return null; }
    }

    public static boolean alive(Long pid) {
        return pid != null && ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    /** First runnable jar under target/, skipping shade leftovers and sources/javadoc artifacts. */
    public static Path findJar() throws IOException {
        Path target = Path.of("target");
        if (!Files.isDirectory(target)) return null;
        try (Stream<Path> s = Files.list(target)) {
            return s.filter(p -> {
                String n = p.getFileName().toString();
                return n.endsWith(".jar") && !n.contains("original")
                    && !n.contains("sources") && !n.contains("javadoc");
            }).findFirst().orElse(null);
        }
    }
}
