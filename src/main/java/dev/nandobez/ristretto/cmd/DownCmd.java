package dev.nandobez.ristretto.cmd;

import dev.nandobez.ristretto.core.ProjectProc;
import picocli.CommandLine.*;
import java.nio.file.*;
import java.util.concurrent.Callable;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "down", aliases = {"stop"}, description = "Stop the app started with 'up'.")
public class DownCmd implements Callable<Integer> {

    public Integer call() throws Exception {
        Long pid = ProjectProc.readPid();
        if (!ProjectProc.alive(pid)) {
            info("not running");
            Files.deleteIfExists(ProjectProc.pidFile());
            Files.deleteIfExists(ProjectProc.startFile());
            return 0;
        }
        String name = projectName();
        String uptime = ProjectProc.uptime();

        Spinner sp = Spinner.start("stopping " + name + " …");
        ProcessHandle h = ProcessHandle.of(pid).orElse(null);
        if (h != null) {
            h.destroy();
            for (int i = 0; i < 20 && h.isAlive(); i++) Thread.sleep(250);
            if (h.isAlive()) h.destroyForcibly();
        }
        Files.deleteIfExists(ProjectProc.pidFile());
        Files.deleteIfExists(ProjectProc.startFile());
        Files.deleteIfExists(ProjectProc.metaFile());
        sp.stop();

        System.out.println("  " + DIM + "│" + R);
        System.out.println("  " + DIM + "└─ " + R + tag("stopped") + DIM + " · " + name
            + " · pid " + pid + (uptime != null ? " · ran " + uptime : "") + R);
        return 0;
    }

    private static String projectName() {
        java.nio.file.Path fn = java.nio.file.Path.of("").toAbsolutePath().getFileName();
        return fn == null ? "app" : fn.toString();
    }
}
