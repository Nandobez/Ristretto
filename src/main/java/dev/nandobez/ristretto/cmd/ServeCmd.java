package dev.nandobez.ristretto.cmd;

import dev.nandobez.ristretto.core.ToolRegistry;
import dev.nandobez.ristretto.core.ToolRegistry.Tool;
import picocli.CommandLine.*;
import java.nio.file.*;
import java.util.concurrent.Callable;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "serve", aliases = {"s"},
    description = "Dev mode: backend + frontend in parallel.")
public class ServeCmd implements Callable<Integer> {
    public Integer call() throws Exception {
        boolean hasFrontend = Files.exists(Path.of("src/main/frontend/package.json")) && ToolRegistry.has(Tool.MACC);
        boolean hasBackend  = ToolRegistry.has(Tool.XPRESSO);

        if (!hasBackend && !hasFrontend) { error("nothing to serve — xpresso/macc not installed"); return 2; }
        if (hasBackend && !hasFrontend)  return ToolRegistry.invoke(Tool.XPRESSO, "s");
        if (hasFrontend && !hasBackend)  return ToolRegistry.invoke(Tool.MACC, "serve");

        // Both present: run backend and frontend concurrently, wait for both.
        info("serving backend (xpresso :8080) + frontend (macc/vite) in parallel — Ctrl-C to stop");
        var rc = new java.util.concurrent.atomic.AtomicInteger(0);
        Thread backend  = serverThread(Tool.XPRESSO, rc, "s");
        Thread frontend = serverThread(Tool.MACC,    rc, "serve");
        backend.start();
        frontend.start();
        backend.join();
        frontend.join();
        return rc.get();
    }

    private static Thread serverThread(Tool t, java.util.concurrent.atomic.AtomicInteger rc, String... args) {
        Thread th = new Thread(() -> {
            try { int r = ToolRegistry.invoke(t, args); if (r != 0) rc.compareAndSet(0, r); }
            catch (Exception e) { rc.compareAndSet(0, 1); error(t.name + ": " + e.getMessage()); }
        }, t.name + "-serve");
        th.setDaemon(true);
        return th;
    }
}
