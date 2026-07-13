package dev.nandobez.ristretto.cmd;

import static dev.nandobez.ristretto.cmd.Tui.*;

/** Shared Vite-style app banner (tree of URLs) used by both `up` and `status`. */
final class AppBanner {

    static void render(String name, String headline, String meta, int port) {
        System.out.println();
        System.out.println("  " + BLD + name + R + "  " + headline + DIM + "  ·  " + meta + R);
        System.out.println("  " + DIM + "│" + R);

        var rows = new java.util.ArrayList<String[]>();
        rows.add(new String[]{"Local", "http://localhost:" + port});
        String ip = lanIp();
        if (ip != null) rows.add(new String[]{"Network", "http://" + ip + ":" + port});
        rows.add(new String[]{"Docs", "http://localhost:" + port + "/swagger-ui.html"});
        rows.add(new String[]{"Health", "http://localhost:" + port + "/actuator/health"});
        for (int i = 0; i < rows.size(); i++) {
            String branch = i == rows.size() - 1 ? "└" : "├";
            System.out.println("  " + DIM + branch + "➔ " + R + pad(rows.get(i)[0], 8) + " " + CYN + rows.get(i)[1] + R);
        }
        System.out.println();
        System.out.println("  " + DIM + "logs: ristretto logs -f  ·  stop: ristretto down" + R);
    }

    /** Same shape as render(), for the not-running state (no URLs, a start hint). */
    static void stopped(String name) {
        System.out.println();
        System.out.println("  " + BLD + name + R + "  " + DIM + "stopped" + R);
        System.out.println("  " + DIM + "│" + R);
        System.out.println("  " + DIM + "└➔ " + R + pad("start", 8) + " " + CYN + "ristretto up" + R);
    }

    static String projectName() {
        java.nio.file.Path fn = java.nio.file.Path.of("").toAbsolutePath().getFileName();
        return fn == null ? "app" : fn.toString();
    }

    /** First site-local IPv4 of an up, non-loopback interface — the LAN address. */
    static String lanIp() {
        try {
            for (var ni : java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (var addr : java.util.Collections.list(ni.getInetAddresses()))
                    if (addr instanceof java.net.Inet4Address && addr.isSiteLocalAddress())
                        return addr.getHostAddress();
            }
        } catch (Exception ignored) {}
        return null;
    }

    static String pad(String s, int w) { return s.length() >= w ? s : s + " ".repeat(w - s.length()); }
}
