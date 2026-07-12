package dev.nandobez.ristretto.cmd;

import dev.nandobez.ristretto.core.ToolRegistry.Tool;
import picocli.CommandLine.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "install-tools",
    description = "Build the trio (jdp + xpresso + macc) from local source and install their jars — offline, no curl.")
public class InstallToolsCmd implements Callable<Integer> {

    /** Directory that holds the sibling source folders (jdp / Xpresso / Macchiato). */
    @Option(names = "--from", defaultValue = ".",
        description = "Base directory containing the tool source folders. Default: current dir.")
    String from;

    @Option(names = "--prefix",
        description = "Install prefix. Default: $RISTRETTO_PREFIX or ~/.local.")
    String prefix;

    /** Repo folder names to probe for each tool (GitHub casing + lowercase). */
    private static final Map<Tool, List<String>> DIR_CANDIDATES = Map.of(
        Tool.JDP,     List.of("jdp"),
        Tool.XPRESSO, List.of("Xpresso", "xpresso"),
        Tool.MACC,    List.of("Macchiato", "macchiato", "macc")
    );

    public Integer call() throws Exception {
        Path base = Path.of(from).toAbsolutePath().normalize();
        Path libRoot = installPrefix().resolve("share");
        banner("ristretto install-tools", base.toString());

        int failed = 0;
        for (Tool t : new Tool[]{ Tool.JDP, Tool.XPRESSO, Tool.MACC }) {
            Path src = findSource(base, t);
            if (src == null) {
                error(t.name + " source not found under " + base + " (looked for " + DIR_CANDIDATES.get(t) + ")");
                failed++;
                continue;
            }
            info("building " + BLD + t.name + R + "  " + DIM + src + R);
            int rc = mvnPackage(src);
            if (rc != 0) { error(t.name + " build failed (rc=" + rc + ")"); failed++; continue; }

            Path jar = src.resolve("target").resolve(t.name + ".jar");
            if (!Files.exists(jar)) { error(t.name + " produced no target/" + t.name + ".jar"); failed++; continue; }

            Path dest = libRoot.resolve(t.name).resolve(t.name + ".jar");
            Files.createDirectories(dest.getParent());
            Files.copy(jar, dest, StandardCopyOption.REPLACE_EXISTING);
            ok(t.name + " → " + dest);
        }

        System.out.println();
        if (failed == 0) ok("trio installed. try " + BLD + "ristretto version" + R);
        else             error(failed + " tool(s) failed.");
        return failed == 0 ? 0 : 1;
    }

    private Path installPrefix() {
        if (prefix != null) return Path.of(prefix);
        String env = System.getenv("RISTRETTO_PREFIX");
        if (env != null && !env.isBlank()) return Path.of(env);
        return Path.of(System.getProperty("user.home"), ".local");
    }

    /** First candidate folder under base that exists and has a pom.xml. */
    private static Path findSource(Path base, Tool t) {
        for (String name : DIR_CANDIDATES.get(t)) {
            Path p = base.resolve(name);
            if (Files.exists(p.resolve("pom.xml"))) return p;
        }
        return null;
    }

    private static int mvnPackage(Path dir) throws Exception {
        String mvn = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
        return new ProcessBuilder(mvn, "-q", "-DskipTests", "package")
            .directory(dir.toFile())
            .inheritIO()
            .start()
            .waitFor();
    }
}
