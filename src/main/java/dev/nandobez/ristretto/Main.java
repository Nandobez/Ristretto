package dev.nandobez.ristretto;

import dev.nandobez.ristretto.cmd.*;
import dev.nandobez.ristretto.core.PassThrough;
import dev.nandobez.ristretto.core.ToolRegistry;
import dev.nandobez.ristretto.core.ToolRegistry.Tool;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(
    name = "ristretto",
    mixinStandardHelpOptions = true,
    version = "ristretto 0.1.0",
    description = "One shot. Three espressos. (jdp + xpresso + macc)",
    subcommands = {
        NewCmd.class, ResourceCmd.class, ServeCmd.class,
        UpCmd.class, DownCmd.class, ReloadCmd.class, StatusCmd.class, LogsCmd.class,
        DoctorCmd.class, VersionCmd.class, UpdateCmd.class,
        InstallToolsCmd.class
    }
)
public class Main implements Runnable {

    public void run() { printHelp(); }

    public static void main(String[] args) {
        if (args.length == 0) { printHelp(); System.exit(0); }

        CommandLine cl = new CommandLine(new Main());

        // First, see if it's a known top-level subcommand (or help/version flag).
        if (!isKnownTopLevel(cl, args[0])) {
            // Try pass-through (forward to jdp/xpresso/macc).
            Tool route = PassThrough.routeOf(args[0]);
            if (route != null) {
                try {
                    System.out.println();
                    System.exit(PassThrough.forward(args));
                } catch (Exception e) {
                    System.err.println("ristretto: " + e.getMessage());
                    System.exit(2);
                }
            }
        }

        System.out.println();
        int rc = cl.execute(args);
        System.out.println();
        System.exit(rc);
    }

    /** Derives the accepted top-level tokens from picocli itself, so the list never drifts from @Command. */
    private static boolean isKnownTopLevel(CommandLine cl, String cmd) {
        for (var sub : cl.getSubcommands().values()) {
            var spec = sub.getCommandSpec();
            if (spec.name().equals(cmd)) return true;
            for (String alias : spec.aliases()) if (alias.equals(cmd)) return true;
        }
        return switch (cmd) {
            case "-h", "--help", "-V", "--version" -> true;
            default -> false;
        };
    }

    private static void printHelp() {
        System.out.println();
        System.out.println(BLD + "ristretto " + R + DIM + "0.1.0" + R + " — Java fullstack, three espressos");
        System.out.println(DIM + "  Bundles " + R + BLD + "jdp" + R + DIM + " (deps) + " + R + BLD + "xpresso" + R + DIM + " (backend) + " + R + BLD + "macc" + R + DIM + " (frontend)." + R);
        System.out.println();
        System.out.println("  " + DIM + "USAGE" + R);
        System.out.println();
        System.out.println("    ristretto <command>");
        System.out.println("    rist <command>            " + DIM + "// short alias" + R);
        System.out.println("    r <command>               " + DIM + "// shortest alias" + R);
        System.out.println();
        System.out.println();
        System.out.println("  " + DIM + "FULLSTACK" + R);
        System.out.println();
        System.out.println("    " + BLD + "new <name>" + R + "                       scaffold backend + frontend together");
        System.out.println("    " + BLD + "resource <Name> <fields...>" + R + "       CRUD across backend AND frontend");
        System.out.println("    " + BLD + "serve, s" + R + "                          backend + frontend in parallel");
        System.out.println("    " + BLD + "doctor [--fix]" + R + "                    CVE + outdated (via jdp)");
        System.out.println();
        System.out.println();
        System.out.println("  " + DIM + "META" + R);
        System.out.println();
        System.out.println("    " + BLD + "version, v" + R + "                        show all 4 tools + JDK + Node");
        System.out.println("    " + BLD + "update" + R + "                            update jdp + xpresso + macc to latest");
        System.out.println();
        System.out.println();
        System.out.println("  " + DIM + "PASS-THROUGH" + R + DIM + "  (forwarded to the right tool)" + R);
        System.out.println();
        System.out.println("    " + DIM + "jdp" + R + "       list, search, add, rm, why, weight, unused, diff, migrate, init, repl");
        System.out.println("    " + DIM + "xpresso" + R + "   g, server/s, console/c, build, test/t, db, routes, clean, compile,");
        System.out.println("    " + DIM + "          " + R + "install, deps, api, watch, config, health, profile, beans");
        System.out.println("    " + DIM + "macc" + R + "      codegen, dev");
        System.out.println();
        System.out.println("    " + DIM + "any verb, or target a tool directly with a prefix:" + R);
        System.out.println("    " + DIM + "  r add starter-data-jpa     " + R + "→ jdp add");
        System.out.println("    " + DIM + "  r g model User name:string " + R + "→ xpresso g model");
        System.out.println("    " + DIM + "  r xpresso beans            " + R + "→ xpresso beans");
        System.out.println("    " + DIM + "  r macc add Button          " + R + "→ macc add  (bypasses conflicts)");
        System.out.println();

        // Quick status line
        System.out.println("  " + DIM + "INSTALLED" + R);
        System.out.println();
        for (Tool t : Tool.values()) {
            String path = ToolRegistry.locate(t);
            String mark = path == null ? RED + "✗" + R : GRN + "✓" + R;
            System.out.println("    " + mark + " " + (t.name + "        ").substring(0, 8) + DIM + (path == null ? "not installed" : path) + R);
        }
        System.out.println();
    }
}
