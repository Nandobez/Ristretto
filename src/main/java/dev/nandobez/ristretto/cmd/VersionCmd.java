package dev.nandobez.ristretto.cmd;

import dev.nandobez.ristretto.Main;
import dev.nandobez.ristretto.core.ToolRegistry;
import dev.nandobez.ristretto.core.ToolRegistry.Tool;
import picocli.CommandLine.*;
import java.util.concurrent.Callable;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "version", aliases = {"v"},
    description = "Print versions of ristretto + jdp + xpresso + macc + JDK + Node.")
public class VersionCmd implements Callable<Integer> {
    public Integer call() throws Exception {
        banner("ristretto · version", ownVersion());
        for (Tool t : Tool.values()) {
            String path = ToolRegistry.locate(t);
            if (path == null) { System.out.println("    " + RED + "✗ " + R + t.name().toLowerCase() + DIM + "  not installed" + R); continue; }
            String ver = jarVersion(path);
            System.out.println("    " + GRN + "✓ " + R + pad(t.name().toLowerCase(), 9)
                + (ver.isEmpty() ? "" : ver + "  ") + DIM + path + R);
        }
        System.out.println();
        printOnce("java",  "java -version 2>&1 | head -1");
        printOnce("mvn",   "mvn -v 2>/dev/null | head -1");
        printOnce("node",  "node -v 2>/dev/null");
        printOnce("npm",   "npm -v 2>/dev/null");
        return 0;
    }

    /** Reads ristretto's own version from the picocli @Command spec — no hardcoded literal. */
    private static String ownVersion() {
        Command c = Main.class.getAnnotation(Command.class);
        String[] v = c == null ? null : c.version();
        return (v == null || v.length == 0) ? "unknown" : v[0];
    }

    /** Runs `java -jar <jar> --version` and returns the first non-empty line, or "" on failure. */
    private static String jarVersion(String jar) {
        try {
            var p = new ProcessBuilder("java", "-jar", jar, "--version")
                .redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            for (String line : out.split("\n")) {
                String s = line.strip();
                if (!s.isEmpty()) return s;
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String pad(String s, int w) {
        return s + " ".repeat(Math.max(0, w - s.length()));
    }

    private static void printOnce(String label, String shell) {
        try {
            var p = new ProcessBuilder("bash", "-c", shell).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            if (out.isEmpty()) System.out.println("    " + RED + "✗ " + R + label + DIM + "  not found" + R);
            else               System.out.println("    " + GRN + "✓ " + R + pad(label, 9) + DIM + out + R);
        } catch (Exception ignored) {}
    }
}
