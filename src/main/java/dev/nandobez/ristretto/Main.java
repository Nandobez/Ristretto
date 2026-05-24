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
        DoctorCmd.class, VersionCmd.class, UpdateCmd.class
    }
)
public class Main implements Runnable {

    public void run() { printHelp(); }

    public static void main(String[] args) {
        if (args.length == 0) { printHelp(); System.exit(0); }

        // First, see if it's a known top-level subcommand.
        boolean known = isKnownTopLevel(args[0]);
        if (!known) {
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
        int rc = new CommandLine(new Main()).execute(args);
        System.out.println();
        System.exit(rc);
    }

    private static boolean isKnownTopLevel(String cmd) {
        return switch (cmd) {
            case "new", "resource", "serve", "s", "doctor", "version", "v", "update", "-h", "--help", "-V", "--version" -> true;
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
        System.out.println("    " + DIM + "jdp" + R + "       list, search, add, rm, why, weight, unused, diff, migrate");
        System.out.println("    " + DIM + "xpresso" + R + "   g, server, console, build, test, db, routes, clean, compile, install");
        System.out.println("    " + DIM + "macc" + R + "      codegen, dev");
        System.out.println();
        System.out.println("    " + DIM + "examples:" + R);
        System.out.println("    " + DIM + "  r add starter-data-jpa     " + R + "→ jdp add");
        System.out.println("    " + DIM + "  r g model User name:string " + R + "→ xpresso g model");
        System.out.println("    " + DIM + "  r codegen                  " + R + "→ macc codegen");
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
