package dev.nandobez.ristretto.cmd;

public class Tui {
    public static final String R   = "[0m";
    public static final String BLD = "[1m";
    public static final String DIM = "[2m";
    public static final String RED = "[31m";
    public static final String GRN = "[32m";
    public static final String YLW = "[33m";
    public static final String CYN = "[36m";

    public static void banner(String title, String subtitle) {
        System.out.println();
        System.out.println("  " + BLD + title + R + DIM + "  " + subtitle + R);
        System.out.println();
    }
    public static void info(String s)  { System.out.println("    " + DIM + s + R); }
    public static void ok(String s)    { System.out.println("    " + GRN + "✓ " + R + s); }
    public static void error(String s) { System.out.println("    " + RED + "✗ " + R + s); }
}
