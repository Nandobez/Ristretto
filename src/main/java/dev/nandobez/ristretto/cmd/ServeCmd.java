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
        if (Files.exists(Path.of("src/main/frontend/package.json"))
            && ToolRegistry.has(Tool.MACC)) {
            return ToolRegistry.invoke(Tool.MACC, "serve");
        }
        if (!ToolRegistry.has(Tool.XPRESSO)) { error("xpresso not installed"); return 2; }
        return ToolRegistry.invoke(Tool.XPRESSO, "s");
    }
}
