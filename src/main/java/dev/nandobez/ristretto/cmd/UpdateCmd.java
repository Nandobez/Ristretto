package dev.nandobez.ristretto.cmd;

import picocli.CommandLine.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.concurrent.Callable;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "update", description = "Re-run each tool's installer to update to latest.")
public class UpdateCmd implements Callable<Integer> {
    public Integer call() throws Exception {
        banner("ristretto update", "fetching latest installers");
        runInstaller("jdp",     "https://raw.githubusercontent.com/Nandobez/jdp/main/install.sh");
        runInstaller("xpresso", "https://raw.githubusercontent.com/Nandobez/Xpresso/main/install.sh");
        runInstaller("macc",    "https://raw.githubusercontent.com/Nandobez/Macchiato/main/install.sh");
        return 0;
    }
    private static void runInstaller(String name, String url) throws Exception {
        info("updating " + BLD + name + R);
        var p = new ProcessBuilder("bash", "-c", "curl -fsSL " + url + " | bash").inheritIO().start();
        int rc = p.waitFor();
        if (rc == 0) ok(name + " updated"); else error(name + " update failed (rc=" + rc + ")");
    }
}
