<div align="center">

# ☕ Ristretto

### One shot. Three espressos.

[![JDK](https://img.shields.io/badge/JDK-17+-007396?style=for-the-badge&logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](./LICENSE)

</div>

`ristretto` bundles **[jdp](https://github.com/Nandobez/jdp)** (deps)
\+ **[Xpresso](https://github.com/Nandobez/Xpresso)** (backend) +
**[Macchiato](https://github.com/Nandobez/Macchiato)** (frontend) into
a single CLI and adds **fullstack recipes** that touch all three at
once.

Aliases: `ristretto` · `rist` · `r`.

## Install

```bash
curl -fsSL https://raw.githubusercontent.com/Nandobez/Ristretto/main/install.sh | bash
```

The installer pulls **jdp**, **xpresso**, **macc**, then builds and
installs **ristretto** itself.

Use `--solo` to skip the trio (`curl … | bash -s -- --solo`) when you
already have jdp/xpresso/macc.

## 60-second fullstack

```bash
r new cafe-shop --group io.acme               # backend (xpresso) + frontend (macc) together
cd cafe-shop
r resource Order item:string price:decimal paid:bool   # CRUD across both layers
r serve                                       # backend :8080 + vite :5173 with proxy
```

`r resource` generates **eight files** in one go:
- `domain/Order.java` (JPA entity)
- `repository/OrderRepository.java`
- `dto/OrderDto.java`
- `service/OrderService.java`
- `web/OrderController.java` (REST CRUD)
- `db/migration/V…__create_orders.sql` (Flyway)
- `ui/OrderModel.java` (TS-shared record)
- `ui/OrdersPage.java` (Macc @Page)

Then `r serve` runs xpresso (backend) + vite (frontend) in parallel —
edits to either are hot-reloaded.

## Commands

### Fullstack (ristretto own)

```
new <name>                       scaffold backend + frontend together
resource <Name> <fields...>      CRUD across backend AND frontend
serve, s                         backend + frontend in parallel
doctor [--fix]                   CVE + outdated checks (delegates to jdp)
version, v                       show all 4 tools + JDK + Node versions
update                           update jdp + xpresso + macc to latest
```

### Pass-through

Any unknown verb is forwarded to the right tool:

```
r list                           → jdp list
r add starter-data-jpa           → jdp add
r search jjwt                    → jdp search
r weight                         → jdp weight
r why tomcat                     → jdp why
r g model User name:string       → xpresso g model
r server                         → xpresso s
r build                          → xpresso build
r db migrate                     → xpresso db migrate
r routes                         → xpresso routes
r console                        → xpresso console
r codegen                        → macc codegen
r dev                            → macc dev
```

## How it composes

```
                  ┌──────────────────┐
                  │   r <command>    │
                  └────────┬─────────┘
                           │
            ┌──────────────┼──────────────┐
            ▼              ▼              ▼
       ┌─────────┐    ┌──────────┐   ┌────────┐
       │   jdp   │    │  xpresso │   │  macc  │
       │  deps   │    │ backend  │   │ front  │
       └─────────┘    └──────────┘   └────────┘
                           │
                           ▼
               ┌─────────────────────────┐
               │  Spring Boot @ :8080    │
               │  + bundled React (vite) │
               └─────────────────────────┘
```

## Recipes

### Add a backend dep + verify

```bash
r add starter-actuator              # jdp add (with verify chain)
r doctor                            # jdp doctor — CVE + outdated + score
```

### Hot-reload while editing pages

```bash
r serve                             # backend + vite together
# edit ui/OrdersPage.java
# macc codegen runs automatically on file change
# vite hot-reloads the page
```

### Ship to production

```bash
r doctor --fix                      # auto-bump CVE deps
r install                           # mvn clean install + macc install
java -jar target/cafe-shop-*.jar    # single jar serves backend + frontend
```

## Environment

The tools are located by:

1. `$JDP_HOME` / `$XPRESSO_HOME` / `$MACC_HOME` (if set)
2. `~/.local/share/<tool>/<tool>.jar`
3. `/usr/local/share/<tool>/<tool>.jar`
4. `/tmp/<tool>/target/<tool>.jar` (dev builds)

`r version` shows which paths are picked.

## License

MIT — Fernando Bezerra · 2026
