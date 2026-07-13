package dev.nandobez.ristretto.cmd;

import dev.nandobez.ristretto.core.ProjectProc;
import picocli.CommandLine.*;
import java.util.concurrent.Callable;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "status", description = "Show whether the app started with 'up' is running.")
public class StatusCmd implements Callable<Integer> {

    public Integer call() throws Exception {
        Long pid = ProjectProc.readPid();
        if (ProjectProc.alive(pid)) {
            String uptime = ProjectProc.uptime();
            String headline = GRN + "running" + (uptime != null ? " " + uptime : "") + R;
            AppBanner.render(AppBanner.projectName(), headline,
                ProjectProc.readProfile("?") + " · pid " + pid, ProjectProc.readPort(8080));
            return 0;
        }
        AppBanner.stopped(AppBanner.projectName());
        return 0;
    }
}
