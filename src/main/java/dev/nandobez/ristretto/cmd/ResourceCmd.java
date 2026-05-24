package dev.nandobez.ristretto.cmd;

import dev.nandobez.ristretto.core.ToolRegistry;
import dev.nandobez.ristretto.core.ToolRegistry.Tool;
import picocli.CommandLine.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.Callable;

import static dev.nandobez.ristretto.cmd.Tui.*;

@Command(name = "resource",
    description = "Full-stack CRUD: xpresso g resource (backend) + Macc @Model + @Page (frontend).")
public class ResourceCmd implements Callable<Integer> {

    @Parameters(index = "0", description = "Entity name (e.g. Post).")
    String name;

    @Parameters(index = "1..*", arity = "1..*", description = "Fields: title:string body:text done:bool")
    List<String> fields;

    @Option(names = "--no-frontend", description = "Skip the Macc page generation.")
    boolean noFrontend;

    public Integer call() throws Exception {
        banner("ristretto resource " + name, String.join(" ", fields));

        // 1. xpresso g resource
        var args = new java.util.ArrayList<String>();
        args.add("g"); args.add("resource"); args.add(name);
        args.addAll(fields);
        int rc = ToolRegistry.invoke(Tool.XPRESSO, args.toArray(String[]::new));
        if (rc != 0) return rc;

        if (noFrontend || !ToolRegistry.has(Tool.MACC)) {
            if (noFrontend) info("frontend skipped (--no-frontend)");
            else            info("macc not installed — skipping frontend");
            return 0;
        }

        // 2. Locate base package + write @Model record + @Page stub
        Path root = Path.of(".").toAbsolutePath();
        String basePackage = detectBasePackage(root);
        if (basePackage == null) {
            error("could not detect base package — generate the frontend manually with `macc g page " + name + "`");
            return 0;
        }

        Path uiDir = root.resolve("src/main/java/" + basePackage.replace('.', '/') + "/ui");
        Files.createDirectories(uiDir);

        String modelSrc = modelTemplate(basePackage + ".ui", name, fields);
        String pageSrc  = pageTemplate(basePackage + ".ui", name, fields);
        Path modelPath = uiDir.resolve(name + "Model.java");
        Path pagePath  = uiDir.resolve(name + "sPage.java");
        Files.writeString(modelPath, modelSrc);
        Files.writeString(pagePath,  pageSrc);
        System.out.println("    " + GRN + "create" + R + "  " + modelPath);
        System.out.println("    " + GRN + "create" + R + "  " + pagePath);

        System.out.println();
        ok("backend + frontend ready. run " + BLD + "ristretto serve" + R);
        return 0;
    }

    private static String detectBasePackage(Path root) throws Exception {
        Path java = root.resolve("src/main/java");
        if (!Files.exists(java)) return null;
        try (var s = Files.walk(java)) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.toString().endsWith("Application.java"))::iterator) {
                Path rel = java.relativize(p.getParent());
                return rel.toString().replace('/', '.');
            }
        }
        return null;
    }

    private static String modelTemplate(String pkg, String name, List<String> fields) {
        var sb = new StringBuilder("package ").append(pkg).append(";\n\n");
        boolean hasBigDecimal = fields.stream().anyMatch(f -> f.endsWith(":decimal") || f.endsWith(":money"));
        if (hasBigDecimal) sb.append("import java.math.BigDecimal;\n");
        sb.append("import dev.nandobez.macc.dsl.annotations.Model;\n\n");
        sb.append("@Model\npublic record ").append(name).append("Model(Long id");
        for (String f : fields) {
            String[] p = f.split(":");
            String fname = p[0];
            String ftype = switch (p.length > 1 ? p[1] : "string") {
                case "int","integer" -> "Integer";
                case "long","bigint" -> "Long";
                case "bool","boolean" -> "Boolean";
                case "decimal","money" -> "BigDecimal";
                case "double" -> "Double";
                case "float" -> "Float";
                default -> "String";
            };
            sb.append(", ").append(ftype).append(" ").append(fname);
        }
        sb.append(") {}\n");
        return sb.toString();
    }

    private static String pageTemplate(String pkg, String name, List<String> fields) {
        String plural = name + "s"; // very simple
        String pluralLower = plural.toLowerCase();
        String firstField = fields.get(0).split(":")[0];
        return """
            package %s;

            import dev.nandobez.macc.dsl.*;
            import dev.nandobez.macc.dsl.annotations.*;
            import static dev.nandobez.macc.dsl.Tags.*;
            import static dev.nandobez.macc.dsl.Helpers.*;

            import java.util.List;

            @Page("/%s")
            public class %sPage extends Component {

                @Fetch(url = "/api/%s", empty = "No %s yet")
                Var<List<%sModel>> items;

                @Action(method = "DELETE", url = "/api/%s/{id}")
                void remove(Long id) {}

                @Override
                public Element render() {
                    return div().className("max-w-3xl mx-auto p-8").children(
                        h1("%s").className("text-2xl font-bold mb-6"),
                        ul(each("items", "x",
                            li()
                                .key($("x.id"))
                                .className("flex items-center gap-3 py-2 border-b")
                                .children(
                                    span().text($("x.%s")),
                                    button("Delete")
                                        .onClick($("() => remove(x.id)"))
                                        .className("ml-auto text-red-500 text-sm hover:underline")
                                )
                        ))
                    );
                }
            }
            """.formatted(pkg, pluralLower, plural, pluralLower, pluralLower, name, pluralLower, plural, firstField);
    }
}
