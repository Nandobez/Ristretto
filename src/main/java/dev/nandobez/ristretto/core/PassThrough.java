package dev.nandobez.ristretto.core;

import dev.nandobez.ristretto.core.ToolRegistry.Tool;

import java.util.*;

/** Forwards unknown commands to the right tool. */
public class PassThrough {

    /** Single-owner verbs → the tool that runs them. Conflicting verbs (g, doctor, add, watch,
     *  new, serve, install, deps) are resolved to their primary owner here; reach the other tool's
     *  version with an explicit prefix (see TOOL_PREFIX), e.g. `r macc add Button`. */
    private static final Map<String, Tool> ROUTES = new LinkedHashMap<>();
    static {
        // jdp — dependency management
        for (String c : new String[]{"list","ls","search","add","rm","remove","unused","weight","why","diff","migrate","init","repl"})
            ROUTES.put(c, Tool.JDP);
        // xpresso — backend
        for (String c : new String[]{"g","generate","server","s","console","c","build","test","t","db","routes",
                                     "clean","compile","install","deps","dependencies","api","watch","config",
                                     "health","profile","beans"})
            ROUTES.put(c, Tool.XPRESSO);
        // macc — frontend
        for (String c : new String[]{"codegen","dev"})
            ROUTES.put(c, Tool.MACC);
    }

    /** Explicit tool prefixes: `r <tool> <args...>` forwards verbatim, bypassing verb routing. */
    private static final Map<String, Tool> TOOL_PREFIX = Map.of(
        "jdp", Tool.JDP,
        "xpresso", Tool.XPRESSO, "xp", Tool.XPRESSO,
        "macc", Tool.MACC, "macchiato", Tool.MACC
    );

    /** Resolves which tool a leading token routes to (a verb or an explicit tool prefix). */
    public static Tool routeOf(String cmd) {
        String c = cmd.toLowerCase();
        Tool t = ROUTES.get(c);
        return t != null ? t : TOOL_PREFIX.get(c);
    }

    /** Forwards args to the matched tool. Strips a leading tool prefix. Returns -1 when no route exists. */
    public static int forward(String[] args) throws Exception {
        if (args.length == 0) return -1;
        String head = args[0].toLowerCase();

        // Explicit prefix: `r xpresso g model ...` -> invoke xpresso with ["g","model",...]
        Tool prefixed = TOOL_PREFIX.get(head);
        if (prefixed != null) {
            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            return invoke(prefixed, rest, rest.length == 0 ? head : rest[0]);
        }

        Tool t = ROUTES.get(head);
        if (t == null) return -1;
        return invoke(t, args, head);
    }

    private static int invoke(Tool t, String[] args, String label) throws Exception {
        if (!ToolRegistry.has(t)) {
            System.err.println("ristretto: " + t.name + " not installed for '" + label
                + "'. Install the trio with: ristretto install-tools  (or: ristretto update)");
            return 2;
        }
        return ToolRegistry.invoke(t, args);
    }
}
