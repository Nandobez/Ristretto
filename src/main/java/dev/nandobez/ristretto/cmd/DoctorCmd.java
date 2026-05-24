package dev.nandobez.ristretto.cmd;

import dev.nandobez.ristretto.core.ToolRegistry;
import dev.nandobez.ristretto.core.ToolRegistry.Tool;
import picocli.CommandLine.*;
import java.util.concurrent.Callable;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "doctor", description = "Health check across the stack (delegates to jdp).")
public class DoctorCmd implements Callable<Integer> {
    @Option(names = "--fix") boolean fix;

    public Integer call() throws Exception {
        if (!ToolRegistry.has(Tool.JDP)) { error("jdp not installed"); return 2; }
        if (fix) return ToolRegistry.invoke(Tool.JDP, "doctor", "--fix");
        return ToolRegistry.invoke(Tool.JDP, "doctor");
    }
}
