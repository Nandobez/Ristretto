package dev.nandobez.ristretto.cmd;

import picocli.CommandLine.*;
import java.util.concurrent.Callable;

/**
 * Stop the running app and start it again with a fresh build — one command instead of `down` then `up --build`.
 * Handy after editing code: `ristretto reload` picks up the change.
 */
@Command(name = "reload", aliases = {"restart"},
    description = "Stop the running app and start it again with a fresh build.")
public class ReloadCmd implements Callable<Integer> {

    @Option(names = "--profile", defaultValue = "dev", description = "Spring profile to activate. Default: dev.")
    String profile;

    @Option(names = "--port", defaultValue = "8080", description = "Server port.")
    int port;

    @Option(names = "--solve", description = "Auto-fix common blockers before starting (passed to 'up').")
    boolean solve;

    public Integer call() throws Exception {
        int down = new DownCmd().call();
        if (down != 0) return down;

        UpCmd up = new UpCmd();
        up.profile = profile;
        up.port = port;
        up.solve = solve;
        up.build = true;                 // reload always repackages so code changes take effect
        return up.call();
    }
}
