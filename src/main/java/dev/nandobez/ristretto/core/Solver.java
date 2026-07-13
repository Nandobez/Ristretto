package dev.nandobez.ristretto.core;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Heuristic auto-fixes behind `up --solve`.
 * Two fronts: compile errors reported by javac (missing ';', missing '}', missing imports
 * of well-known classes) and boot-time runtime crashes (NPE, out-of-bounds, arithmetic…)
 * where the offending statement in project code is neutralized so the app can start.
 * Every rewrite leaves a `[solve]` marker so the dev can find and properly fix it later.
 */
public final class Solver {
    private Solver() {}

    /* ------------------------------------------------- compile errors */

    private static final Map<String, String> KNOWN_IMPORTS = Map.ofEntries(
        Map.entry("List", "java.util.List"),
        Map.entry("ArrayList", "java.util.ArrayList"),
        Map.entry("LinkedList", "java.util.LinkedList"),
        Map.entry("Map", "java.util.Map"),
        Map.entry("HashMap", "java.util.HashMap"),
        Map.entry("LinkedHashMap", "java.util.LinkedHashMap"),
        Map.entry("Set", "java.util.Set"),
        Map.entry("HashSet", "java.util.HashSet"),
        Map.entry("Optional", "java.util.Optional"),
        Map.entry("Collections", "java.util.Collections"),
        Map.entry("Arrays", "java.util.Arrays"),
        Map.entry("Objects", "java.util.Objects"),
        Map.entry("UUID", "java.util.UUID"),
        Map.entry("Random", "java.util.Random"),
        Map.entry("Stream", "java.util.stream.Stream"),
        Map.entry("Collectors", "java.util.stream.Collectors"),
        Map.entry("Pattern", "java.util.regex.Pattern"),
        Map.entry("Matcher", "java.util.regex.Matcher"),
        Map.entry("BigDecimal", "java.math.BigDecimal"),
        Map.entry("BigInteger", "java.math.BigInteger"),
        Map.entry("LocalDate", "java.time.LocalDate"),
        Map.entry("LocalDateTime", "java.time.LocalDateTime"),
        Map.entry("LocalTime", "java.time.LocalTime"),
        Map.entry("Instant", "java.time.Instant"),
        Map.entry("Duration", "java.time.Duration"),
        Map.entry("IOException", "java.io.IOException"),
        Map.entry("Files", "java.nio.file.Files"),
        Map.entry("Path", "java.nio.file.Path"),
        Map.entry("Paths", "java.nio.file.Paths"),
        Map.entry("PostConstruct", "jakarta.annotation.PostConstruct"),
        Map.entry("PreDestroy", "jakarta.annotation.PreDestroy"),
        Map.entry("Autowired", "org.springframework.beans.factory.annotation.Autowired")
    );

    private static final Pattern JAVAC_ERR =
        Pattern.compile("(\\S+\\.java):\\[(\\d+),(\\d+)\\]\\s+(.*)");
    private static final Pattern SYMBOL_CLASS =
        Pattern.compile("symbol:\\s+class\\s+(\\w+)");

    /** Parse captured maven output and patch sources. Returns human-readable fix lines. */
    public static List<String> fixCompileErrors(String mvnOut) {
        var fixes = new ArrayList<String>();
        var bracesDone = new HashSet<String>();
        var importsDone = new HashSet<String>();
        String[] raw = mvnOut.split("\n");
        for (int i = 0; i < raw.length; i++) {
            var m = JAVAC_ERR.matcher(strip(raw[i]));
            if (!m.find()) continue;
            Path file = Path.of(m.group(1));
            int line = Integer.parseInt(m.group(2)), col = Integer.parseInt(m.group(3));
            String msg = m.group(4);
            try {
                if (!Files.isRegularFile(file)) continue;
                if (msg.contains("';' expected")) {
                    if (insertSemicolon(file, line, col))
                        fixes.add("inserted missing ';' — " + file.getFileName() + ":" + line);
                } else if (msg.contains("reached end of file while parsing")) {
                    if (bracesDone.add(file.toString())) {
                        Files.writeString(file, Files.readString(file) + "}\n");
                        fixes.add("appended missing '}' — " + file.getFileName());
                    }
                } else if (msg.contains("cannot find symbol")) {
                    String cls = null;
                    for (int j = i + 1; j < Math.min(i + 4, raw.length); j++) {
                        var d = SYMBOL_CLASS.matcher(strip(raw[j]));
                        if (d.find()) { cls = d.group(1); break; }
                    }
                    String fqcn = cls == null ? null : KNOWN_IMPORTS.get(cls);
                    if (fqcn != null && importsDone.add(file + "#" + fqcn) && addImport(file, fqcn))
                        fixes.add("added import " + fqcn + " — " + file.getFileName());
                }
            } catch (Exception ignore) {}
        }
        return fixes;
    }

    private static String strip(String s) {
        return s.replaceAll("\\u001B\\[[0-9;]*m", "").replaceFirst("^\\[[A-Z]+\\]\\s?", "").strip();
    }

    private static boolean insertSemicolon(Path file, int line, int col) throws Exception {
        var lines = new ArrayList<>(Files.readAllLines(file));
        if (line < 1 || line > lines.size()) return false;
        String l = lines.get(line - 1);
        int pos = Math.min(Math.max(col - 1, 0), l.length());
        if (pos > 0 && pos <= l.length() && l.substring(0, pos).stripTrailing().endsWith(";")) return false;
        lines.set(line - 1, l.substring(0, pos) + ";" + l.substring(pos));
        Files.writeString(file, String.join("\n", lines) + "\n");
        return true;
    }

    private static boolean addImport(Path file, String fqcn) throws Exception {
        var lines = new ArrayList<>(Files.readAllLines(file));
        String stmt = "import " + fqcn + ";";
        if (lines.stream().anyMatch(l -> l.strip().equals(stmt))) return false;
        int at = -1;
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).strip();
            if (t.startsWith("package ")) { if (at < 0) at = i + 1; }
            else if (t.startsWith("import ")) at = i + 1;
        }
        if (at < 0) at = 0;
        lines.add(at, stmt);
        Files.writeString(file, String.join("\n", lines) + "\n");
        return true;
    }

    /* ------------------------------------------------- runtime crashes */

    private static final Set<String> FIXABLE_EX = Set.of(
        "NullPointerException", "IndexOutOfBoundsException", "ArrayIndexOutOfBoundsException",
        "StringIndexOutOfBoundsException", "ArithmeticException", "ClassCastException",
        "NumberFormatException", "NoSuchElementException", "NegativeArraySizeException",
        "UnsupportedOperationException");

    private static final Pattern EX_LINE = Pattern.compile(
        "^(?:Caused by:\\s*)?((?:[a-z]\\w*\\.)+[A-Z]\\w*(?:Exception|Error))(?::\\s*(.*))?$");
    private static final Pattern FRAME = Pattern.compile(
        "^at\\s+([\\w.$]+)\\.[\\w$<>]+\\((\\w+)\\.java:(\\d+)\\)");
    private static final Pattern DECL = Pattern.compile(
        "^(?:final\\s+)?([A-Za-z_][\\w.]*(?:<[^=;]*>)?(?:\\[\\])*)\\s+(\\w+)\\s*=\\s*(.+);$");

    /** Find the root-cause exception's first frame inside project code and neutralize it. */
    public static List<String> fixRuntimeCrash(Path log) {
        try {
            String exSimple = null; Path file = null; int line = -1;
            String curEx = null; boolean awaitingFrame = false;
            for (String rawLine : Files.readAllLines(log)) {
                String s = rawLine.strip();
                var em = EX_LINE.matcher(s);
                if (em.matches()) {
                    String simple = em.group(1).substring(em.group(1).lastIndexOf('.') + 1);
                    curEx = FIXABLE_EX.contains(simple) ? simple : null;
                    awaitingFrame = curEx != null;
                    continue;
                }
                if (!awaitingFrame) continue;
                var fm = FRAME.matcher(s);
                if (!fm.find()) continue;
                Path candidate = sourceOf(fm.group(1), fm.group(2));
                if (candidate != null) {                 // last root-cause chain wins
                    exSimple = curEx; file = candidate; line = Integer.parseInt(fm.group(3));
                    awaitingFrame = false;
                }
            }
            if (file == null) return List.of();
            return neutralize(file, line, exSimple);
        } catch (Exception e) { return List.of(); }
    }

    /** fqcn + simple file name → existing source file under src/main/java, or null. */
    private static Path sourceOf(String fqcn, String simpleFile) {
        int cut = fqcn.lastIndexOf('.');
        if (cut < 0) return null;
        String pkg = fqcn.substring(0, cut);
        Path p = Path.of("src/main/java", pkg.replace('.', '/'), simpleFile + ".java");
        return Files.isRegularFile(p) ? p : null;
    }

    /** Declaration → safe default; plain statement → commented out. Both keep a [solve] marker. */
    private static List<String> neutralize(Path file, int lineNo, String exSimple) throws Exception {
        var lines = new ArrayList<>(Files.readAllLines(file));
        if (lineNo < 1 || lineNo > lines.size()) return List.of();
        String l = lines.get(lineNo - 1);
        String t = l.strip();
        if (t.startsWith("//") || t.startsWith("return") || t.startsWith("}")) return List.of();
        String indent = l.substring(0, l.length() - l.stripLeading().length());
        String where = file.getFileName() + ":" + lineNo;
        var d = DECL.matcher(t);
        String fix;
        if (d.matches() && !d.group(1).equals("var") && !d.group(1).equals("return")) {
            lines.set(lineNo - 1, indent + t.substring(0, t.indexOf('=') + 1) + " "
                + defaultFor(d.group(1)) + "; // [solve] " + exSimple + ", was: " + d.group(3));
            fix = "defaulted '" + d.group(2) + "' (" + exSimple + ") — " + where;
        } else if (t.endsWith(";")) {
            lines.set(lineNo - 1, indent + "// [solve] removed (" + exSimple + "): " + t);
            fix = "removed crashing statement (" + exSimple + ") — " + where;
        } else return List.of();
        Files.writeString(file, String.join("\n", lines) + "\n");
        return List.of(fix);
    }

    private static String defaultFor(String type) {
        String base = type.replaceAll("<.*>", "").replaceAll("\\[\\]", "").strip();
        return switch (base) {
            case "int", "long", "short", "byte" -> "0";
            case "double", "float" -> "0.0";
            case "boolean" -> "false";
            case "char" -> "' '";
            default -> type.endsWith("[]") ? "null" : "null";
        };
    }
}
