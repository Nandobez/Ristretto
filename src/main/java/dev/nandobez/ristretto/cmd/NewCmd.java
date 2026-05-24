package dev.nandobez.ristretto.cmd;

import dev.nandobez.ristretto.core.ToolRegistry;
import dev.nandobez.ristretto.core.ToolRegistry.Tool;
import picocli.CommandLine.*;
import java.nio.file.*;
import java.util.concurrent.Callable;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "new",
    description = "Scaffold a fullstack project: xpresso backend + macc frontend.")
public class NewCmd implements Callable<Integer> {

    @Parameters(arity = "1", description = "Project name.")
    String name;

    @Option(names = "--group", defaultValue = "com.example")
    String group;

    @Option(names = "--java", defaultValue = "17")
    String javaVersion;

    @Option(names = "--gradle", description = "Use Gradle instead of Maven.")
    boolean useGradle;

    @Option(names = "--no-frontend", description = "Backend only.")
    boolean noFrontend;

    public Integer call() throws Exception {
        banner("ristretto new " + name, group + (useGradle ? " · gradle" : " · maven"));

        if (!ToolRegistry.has(Tool.XPRESSO)) { error("xpresso not installed. Run: ristretto install-tools"); return 2; }

        var args = new java.util.ArrayList<String>();
        args.add("new"); args.add(name); args.add("--group"); args.add(group); args.add("--java"); args.add(javaVersion);
        if (useGradle) args.add("--gradle");
        int rc = ToolRegistry.invoke(Tool.XPRESSO, args.toArray(String[]::new));
        if (rc != 0) return rc;

        if (!noFrontend) {
            if (!ToolRegistry.has(Tool.MACC)) {
                error("macc not installed — skipping frontend. Run: ristretto install-tools");
            } else {
                Path projectRoot = Path.of(name).toAbsolutePath();
                info("setting up Macc frontend in " + name + "/src/main/frontend");
                rc = ToolRegistry.invokeIn(Tool.MACC, projectRoot, "new", "src/main/frontend");
                if (rc != 0) return rc;
            }
        }

        System.out.println();
        ok("ready. next:");
        System.out.println("    " + BLD + "cd " + name + R);
        System.out.println("    " + BLD + "ristretto resource Post title:string body:text" + R);
        System.out.println("    " + BLD + "ristretto serve" + R);
        return 0;
    }
}
