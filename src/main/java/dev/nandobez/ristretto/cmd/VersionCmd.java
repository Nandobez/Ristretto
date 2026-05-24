package dev.nandobez.ristretto.cmd;

import dev.nandobez.ristretto.core.ToolRegistry;
import dev.nandobez.ristretto.core.ToolRegistry.Tool;
import picocli.CommandLine.*;
import java.util.concurrent.Callable;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "version", aliases = {"v"},
    description = "Print versions of ristretto + jdp + xpresso + macc + JDK + Node.")
public class VersionCmd implements Callable<Integer> {
    public Integer call() throws Exception {
        banner("ristretto · version", "0.1.0");
        for (Tool t : Tool.values()) {
            String path = ToolRegistry.locate(t);
            if (path == null) System.out.println("    " + RED + "✗ " + R + t.name() + DIM + "  not installed" + R);
            else               System.out.println("    " + GRN + "✓ " + R + pad(t.name().toLowerCase(), 9) + DIM + path + R);
        }
        System.out.println();
        printOnce("java",  "java -version 2>&1 | head -1");
        printOnce("mvn",   "mvn -v 2>/dev/null | head -1");
        printOnce("node",  "node -v 2>/dev/null");
        printOnce("npm",   "npm -v 2>/dev/null");
        return 0;
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
