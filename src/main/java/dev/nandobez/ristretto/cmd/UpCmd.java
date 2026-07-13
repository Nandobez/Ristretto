package dev.nandobez.ristretto.cmd;

import dev.nandobez.ristretto.core.ProjectProc;
import picocli.CommandLine.*;
import java.nio.file.*;
import java.util.concurrent.Callable;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "up", aliases = {"start"},
    description = "Build (if needed) and run the app detached; pid + logs go under .ristretto/.")
public class UpCmd implements Callable<Integer> {

    @Option(names = "--profile", defaultValue = "dev", description = "Spring profile to activate. Default: dev (seeds run here).")
    String profile;

    @Option(names = "--port", defaultValue = "8080", description = "Server port.")
    int port;

    @Option(names = "--build", description = "Force a fresh package before starting.")
    boolean build;

    public Integer call() throws Exception {
        Long existing = ProjectProc.readPid();
        if (ProjectProc.alive(existing)) {
            info("already running (pid " + existing + "). stop it with " + BLD + "ristretto down" + R);
            return 0;
        }

        System.out.println();
        System.out.println("  " + DIM + "╭─ " + R + BLD + AppBanner.projectName() + R + DIM + " · " + profile + R);

        Path jar = ProjectProc.findJar();
        if (jar == null || build) {
            long pkg = System.nanoTime();
            Spinner sp = Spinner.start(BAR + tcell("packaging"), "building jar …");
            Process mp = new ProcessBuilder("mvn", "-q", "-DskipTests", "package")
                .redirectErrorStream(true).start();
            String out = new String(mp.getInputStream().readAllBytes());
            int rc = mp.waitFor();
            if (rc != 0) { sp.fail("build failed"); printMvnErrors(out); return rc; }
            sp.stop();
            System.out.println(BAR + tcell("packaging")
                + DIM + String.format(java.util.Locale.US, "done in %.1fs", (System.nanoTime() - pkg) / 1e9) + R);
            jar = ProjectProc.findJar();
        }
        if (jar == null) { error("no runnable jar in target/ — is this a built Spring project?"); return 2; }

        ProjectProc.stateDir();
        Path log = ProjectProc.logFile();
        Process p = new ProcessBuilder("java", "-jar", jar.toString(),
                "--spring.profiles.active=" + profile, "--server.port=" + port)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.to(log.toFile()))
            .start();
        Files.writeString(ProjectProc.pidFile(), String.valueOf(p.pid()));
        Files.writeString(ProjectProc.startFile(), String.valueOf(System.currentTimeMillis()));
        Files.writeString(ProjectProc.metaFile(), port + "\n" + profile);

        long t0 = System.nanoTime();
        Spinner sp = Spinner.start(BAR + tcell("wait"), "starting on :" + port + " …");
        boolean ready = false;
        for (int i = 0; i < 60 && p.isAlive(); i++) {
            if (Files.exists(log) && Files.readString(log).contains("Started ")) { ready = true; break; }
            Thread.sleep(1000);
        }
        if (!p.isAlive()) {
            sp.fail("app exited early");
            printExitReason(log);
            return 1;
        }
        sp.stop();
        if (ready) {
            Integer seeded = seededCount(log);
            if (seeded != null) System.out.println(BAR + tcell("event") + DIM + "seeded " + seeded + " rows" + R);
            readyBanner(t0, p.pid());
        } else {
            info("still starting — check " + BLD + "ristretto logs -f" + R);
        }
        return 0;
    }

    private static final String BAR = "  " + DIM + "│  " + R;
    private static String tcell(String name) { return tag(name) + " ".repeat(Math.max(1, 10 - name.length())); }

    private static Integer seededCount(java.nio.file.Path log) {
        try {
            var m = java.util.regex.Pattern.compile("\\[seed\\] inserted (\\d+)")
                .matcher(java.nio.file.Files.readString(log));
            int total = 0; boolean any = false;
            while (m.find()) { total += Integer.parseInt(m.group(1)); any = true; }
            return any ? total : null;
        } catch (Exception e) { return null; }
    }

    /** Vite-style ready banner (shared with `status`). */
    private void readyBanner(long t0, long pid) {
        String headline = GRN + String.format(java.util.Locale.US, "ready in %.1fs", (System.nanoTime() - t0) / 1e9) + R;
        AppBanner.render(AppBanner.projectName(), headline, profile + " · pid " + pid, port);
    }

    /** Surface why the app died — Spring's failure block or the last "Caused by", never raw stack frames. */
    private static void printExitReason(java.nio.file.Path log) {
        try {
            var lines = java.nio.file.Files.readAllLines(log);
            int start = -1;
            for (int i = 0; i < lines.size(); i++)
                if (lines.get(i).contains("APPLICATION FAILED TO START")) start = i;
            if (start >= 0) {
                for (int i = start + 1; i < lines.size(); i++) {
                    String l = lines.get(i).strip();
                    if (l.isEmpty() || l.matches("\\*+") || l.startsWith("at ") || l.startsWith("...")) continue;
                    System.out.println("  " + DIM + l + R);
                }
                return;
            }
            String cause = null;
            for (String l : lines) {
                String s = l.strip();
                if (s.startsWith("Caused by:") || (s.contains("Exception:") && !s.startsWith("at ")))
                    cause = s.replaceFirst("^Caused by:\\s*", "");
            }
            if (cause != null) System.out.println("  " + RED + cause + R);
            else System.out.println("  " + DIM + "see " + BLD + "ristretto logs" + R + DIM + " for details" + R);
        } catch (Exception ignore) {}
    }

    /** Extract compiler errors from captured maven output; no raw [INFO]/[ERROR] noise. */
    private static void printMvnErrors(String out) {
        var seen = new java.util.LinkedHashSet<String>();
        var err = java.util.regex.Pattern.compile("(\\S+\\.java):\\[(\\d+),(\\d+)\\]\\s+(.*)");
        boolean any = false;
        for (String raw : out.split("\n")) {
            String s = raw.replaceAll("\\u001B\\[[0-9;]*m", "").replaceFirst("^\\[[A-Z]+\\]\\s?", "").strip();
            var m = err.matcher(s);
            if (m.find()) {
                if (seen.add(s)) {
                    int i = m.group(1).lastIndexOf("/java/");
                    String f = i >= 0 ? m.group(1).substring(i + 6) : m.group(1);
                    System.out.println("  " + RED + f + ":" + m.group(2) + R + "  " + m.group(4));
                    any = true;
                }
            } else if (any && (s.startsWith("both ") || s.startsWith("symbol:") || s.startsWith("location:"))) {
                System.out.println("      " + DIM + s + R);
            }
        }
        if (!any) System.out.println("  " + DIM + "see " + BLD + "ristretto logs" + R + DIM + " for details" + R);
    }

    private static String tail(String s, int n) {
        String[] lines = s.split("\n");
        var sb = new StringBuilder();
        for (int i = Math.max(0, lines.length - n); i < lines.length; i++) sb.append(lines[i]).append("\n");
        return sb.toString().stripTrailing();
    }
}
