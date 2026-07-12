package dev.nandobez.ristretto.core;

import java.nio.file.*;

/** Locates the jar paths for jdp / xpresso / macc, checking $TOOL_HOME, ~/.local/share/<tool>, /usr/local, and /tmp dev builds. */
public class ToolRegistry {

    public enum Tool {
        JDP    ("jdp",     "JDP_HOME"),
        XPRESSO("xpresso", "XPRESSO_HOME"),
        MACC   ("macc",    "MACC_HOME");

        public final String name;
        public final String envVar;
        Tool(String n, String e) { name = n; envVar = e; }
    }

    public static String locate(Tool t) {
        String env = System.getenv(t.envVar);
        if (env != null) {
            Path p = Path.of(env, t.name + ".jar");
            if (Files.exists(p)) return p.toString();
        }
        for (String prefix : new String[]{
            System.getProperty("user.home") + "/.local/share",
            "/usr/local/share",
            "/tmp",
        }) {
            Path p = Path.of(prefix, t.name, (prefix.equals("/tmp") ? "target/" : "") + t.name + ".jar");
            if (Files.exists(p)) return p.toString();
        }
        return null;
    }

    public static boolean has(Tool t) { return locate(t) != null; }

    /** Fast-startup JVM flags for short-lived CLI subprocesses (skip tiered JIT, cheap GC, use CDS). */
    private static final String[] FAST_START =
        { "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-Xshare:auto" };

    private static java.util.ArrayList<String> baseCmd(String jar, String... args) {
        var cmd = new java.util.ArrayList<String>();
        cmd.add("java");
        for (String f : FAST_START) cmd.add(f);
        cmd.add("-jar"); cmd.add(jar);
        for (String a : args) cmd.add(a);
        return cmd;
    }

    public static int invoke(Tool t, String... args) throws Exception {
        String jar = locate(t);
        if (jar == null) throw new IllegalStateException(t.name + " not installed");
        return new ProcessBuilder(baseCmd(jar, args)).inheritIO().start().waitFor();
    }

    public static int invokeIn(Tool t, Path dir, String... args) throws Exception {
        String jar = locate(t);
        if (jar == null) throw new IllegalStateException(t.name + " not installed");
        return new ProcessBuilder(baseCmd(jar, args)).directory(dir.toFile()).inheritIO().start().waitFor();
    }
}
