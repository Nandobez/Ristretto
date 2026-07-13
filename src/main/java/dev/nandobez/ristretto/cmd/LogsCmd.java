package dev.nandobez.ristretto.cmd;

import dev.nandobez.ristretto.core.ProjectProc;
import picocli.CommandLine.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "logs", description = "Show the app logs (.ristretto/app.log), reformatted Next-style.")
public class LogsCmd implements Callable<Integer> {

    @Option(names = {"-f", "--follow"}, description = "Stream new log lines until Ctrl-C.")
    boolean follow;

    @Option(names = "-n", defaultValue = "80", description = "Number of trailing lines to show.")
    int lines;

    @Option(names = "--raw", description = "Show the raw Spring log lines instead of the compact view.")
    boolean raw;

    /** Spring Boot default log line: <ts>  LEVEL pid --- [app] [thread] logger : message */
    private static final Pattern SPRING = Pattern.compile(
        "^\\S*?(\\d{2}:\\d{2}:\\d{2})\\S*\\s+(INFO|WARN|ERROR|DEBUG|TRACE)\\s+\\d+\\s+---\\s+\\[[^\\]]*\\]\\s+\\[[^\\]]*\\]\\s+\\S+\\s+:\\s+(.*)$");

    public Integer call() throws Exception {
        Path log = ProjectProc.logFile();
        if (!Files.exists(log)) { info("no logs yet — start the app with " + BLD + "ristretto up" + R); return 0; }

        if (follow) {
            Process p = new ProcessBuilder("tail", "-f", log.toString()).start();
            try (var br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String l;
                while ((l = br.readLine()) != null) System.out.println(raw ? l : fmt(l));
            }
            return p.waitFor();
        }

        List<String> all = Files.readAllLines(log);
        for (String l : all.subList(Math.max(0, all.size() - lines), all.size()))
            System.out.println(raw ? l : fmt(l));
        return 0;
    }

    private static String fmt(String line) {
        if (line.isBlank()) return "";
        var m = SPRING.matcher(line);
        if (m.matches()) {
            String time = m.group(1), level = m.group(2).toLowerCase(), msg = m.group(3);
            return "  " + DIM + time + R + "  " + lvl(level) + msg;
        }
        if (line.startsWith("[seed]"))
            return "  " + DIM + "        " + R + "  " + lvl("event") + line.substring(6).trim();
        // stack traces / continuations / anything unparsed
        return "  " + DIM + line + R;
    }

    /** Colored, width-7 level tag. */
    private static String lvl(String level) {
        String c = switch (level) {
            case "warn"  -> YLW;
            case "error" -> RED;
            case "event" -> MAG;
            default      -> DIM;
        };
        return c + level + R + " ".repeat(Math.max(1, 7 - level.length()));
    }
}
