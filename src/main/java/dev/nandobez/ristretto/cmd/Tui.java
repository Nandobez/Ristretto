package dev.nandobez.ristretto.cmd;

public class Tui {
    /** ANSI is emitted only on a real terminal and when NO_COLOR is unset (https://no-color.org). */
    static final boolean COLOR = colorEnabled();

    public static final String R   = c("\033[0m");
    public static final String BLD = c("\033[1m");
    public static final String DIM = c("\033[2m");
    public static final String RED = c("\033[31m");
    public static final String GRN = c("\033[32m");
    public static final String YLW = c("\033[33m");
    public static final String CYN = c("\033[36m");

    private static boolean colorEnabled() {
        if (System.getenv("NO_COLOR") != null) return false;
        if ("dumb".equals(System.getenv("TERM"))) return false;
        return System.console() != null;
    }

    private static String c(String code) { return COLOR ? code : ""; }

    public static void banner(String title, String subtitle) {
        System.out.println();
        System.out.println("  " + BLD + title + R + DIM + "  " + subtitle + R);
        System.out.println();
    }
    public static void info(String s)  { System.out.println("    " + DIM + s + R); }
    public static void ok(String s)    { System.out.println("    " + GRN + "✓ " + R + s); }
    public static void error(String s) { System.out.println("    " + RED + "✗ " + R + s); }
}
