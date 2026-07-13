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
    public static final String MAG = c("\033[35m");

    private static boolean colorEnabled() {
        if (System.getenv("NO_COLOR") != null) return false;
        if ("dumb".equals(System.getenv("TERM"))) return false;
        return System.console() != null;
    }

    private static String c(String code) { return COLOR ? code : ""; }

    /** Next.js-style colored log tag (ready/wait/event/packaging/warn/error/info). */
    public static String tag(String name) {
        String col = switch (name) {
            case "ready" -> GRN;
            case "wait"  -> YLW;
            case "event" -> MAG;
            case "packaging", "compiling", "compiled" -> CYN;
            case "warn"  -> YLW;
            case "error" -> RED;
            default      -> DIM;
        };
        return col + name + R;
    }

    /** Braille loading spinner on a daemon thread. On a non-tty it degrades to one static line. */
    public static final class Spinner {
        private static final String[] FRAMES = {"⠋","⠙","⠹","⠸","⠼","⠴","⠦","⠧","⠇","⠏"};
        private final String lead;
        private final String label;
        private volatile boolean running = true;
        private Thread thread;

        private Spinner(String lead, String label) { this.lead = lead; this.label = label; }

        public static Spinner start(String label) { return start("    ", label); }

        /** `lead` is printed verbatim before the spinner glyph (e.g. a "  │  tag " frame prefix). */
        public static Spinner start(String lead, String label) {
            Spinner s = new Spinner(lead, label);
            if (!COLOR) { System.out.println(lead + label); return s; }
            s.thread = new Thread(() -> {
                int i = 0;
                while (s.running) {
                    System.out.print("\r" + lead + CYN + FRAMES[i++ % FRAMES.length] + R + " " + label + "   ");
                    System.out.flush();
                    try { Thread.sleep(80); } catch (InterruptedException e) { break; }
                }
            }, "spinner");
            s.thread.setDaemon(true);
            s.thread.start();
            return s;
        }

        public void succeed(String msg) { stop(); ok(msg); }
        public void fail(String msg)    { stop(); error(msg); }

        /** Stop animating and clear the spinner line so the next print starts clean. */
        public void stop() {
            if (!running) return;
            running = false;
            if (thread != null) { try { thread.join(200); } catch (InterruptedException ignored) {} }
            if (COLOR) { System.out.print("\r\033[K"); System.out.flush(); }
        }
    }

    public static void banner(String title, String subtitle) {
        System.out.println();
        System.out.println("  " + BLD + title + R + DIM + "  " + subtitle + R);
        System.out.println();
    }
    public static void info(String s)  { System.out.println("    " + DIM + s + R); }
    public static void ok(String s)    { System.out.println("    " + GRN + "✓ " + R + s); }
    public static void error(String s) { System.out.println("    " + RED + "✗ " + R + s); }
}
