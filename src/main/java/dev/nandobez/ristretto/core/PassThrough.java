package dev.nandobez.ristretto.core;

import dev.nandobez.ristretto.core.ToolRegistry.Tool;

import java.util.*;

/** Forwards unknown commands to the right tool. */
public class PassThrough {

    /** Some keywords map deterministically — extend as needed. */
    private static final Map<String, Tool> ROUTES = new LinkedHashMap<>();
    static {
        // jdp
        for (String c : new String[]{"list","ls","search","add","rm","remove","unused","weight","why","diff","migrate","init"})
            ROUTES.put(c, Tool.JDP);
        // xpresso
        for (String c : new String[]{"g","generate","server","s","console","c","build","test","t","db","routes","clean","compile","install","deps","dependencies"})
            ROUTES.put(c, Tool.XPRESSO);
        // macc
        for (String c : new String[]{"codegen","dev"})
            ROUTES.put(c, Tool.MACC);
    }

    /** Resolves which tool owns this verb. */
    public static Tool routeOf(String cmd) {
        return ROUTES.get(cmd.toLowerCase());
    }

    /** Forwards args[0..] to the matched tool. Returns -1 when no route exists. */
    public static int forward(String[] args) throws Exception {
        if (args.length == 0) return -1;
        Tool t = routeOf(args[0]);
        if (t == null) return -1;
        if (!ToolRegistry.has(t)) {
            System.err.println("ristretto: " + t.name + " not installed for command '" + args[0] + "'. Run: ristretto update");
            return 2;
        }
        return ToolRegistry.invoke(t, args);
    }
}
