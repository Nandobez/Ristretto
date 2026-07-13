<div align="center">

<p align="center"><img src="rist.png" alt="Ristretto" width="520"></p>

### One shot. Three espressos.

[![JDK](https://img.shields.io/badge/JDK-17+-007396?style=for-the-badge&logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](./LICENSE)

</div>

`rist` bundles **[jdp](https://github.com/Nandobez/jdp)** (deps)
\+ **[Xpresso](https://github.com/Nandobez/Xpresso)** (backend)
\+ **[Macchiato](https://github.com/Nandobez/Macchiato)** (frontend) into a
single CLI, and adds a **fullstack lifecycle** on top: scaffold, run, inspect,
and hit your API — all through one command.

Download **one** tool, drive **all three**.

Default alias: `rist` (also: `r`, `ristretto`).

**▶ Demo video**

## Install

```bash
curl -fsSL https://raw.githubusercontent.com/Nandobez/Ristretto/main/install.sh | bash
```

The installer pulls **jdp**, **xpresso**, **macc**, then builds and installs
**rist** itself. Already have the trio? Run `rist install-tools` to
build them from local source, or `rist update` to fetch the latest.

## 60-second backend

```bash
rist new shop --group io.acme --no-frontend
cd shop
rist resource Order item:string qty:int paid:bool   # full CRUD, tests, error handler
rist g seed Order item:string qty:int paid:bool      # fake data on dev startup
rist up                                              # build + run detached
rist api GET orders                                  # hit the live API
rist down
```

`rist resource` generates a complete, compiling CRUD slice:

- `domain/Order.java` — JPA entity
- `repository/OrderRepository.java`
- `dto/OrderRequest.java` (validated, no id) + `dto/OrderResponse.java`
- `service/OrderService.java` — `@Transactional`
- `web/OrderController.java` — DTO-based, paged
- `exception/GlobalExceptionHandler.java` — 400 / 404 / 500
- `resources/db/migration/V…__create_orders.sql` — Flyway
- `…ServiceTest` + `…ControllerTest`

## Run & manage

| Command | What it does |
|---------|--------------|
| `rist up [--profile dev] [--port N] [--build]` | package (if needed) + run **detached**; shows a ready banner with Local/Network/Swagger/Health URLs |
| `rist status` | running (pid · uptime) with the same banner, or `stopped` |
| `rist logs [-f] [-n N] [--raw]` | app logs reformatted `time · level · message` |
| `rist down` | stop the app started by `up` |
| `rist serve` | dev mode: backend **and** frontend in parallel |

State lives under `.ristretto/` (git-ignored automatically).

## Talk to your API

```bash
rist api GET orders             # aligned table + pagination
rist api GET orders/1           # single record
rist api POST -m orders         # -m fills a valid mock body from OpenAPI
rist api POST -m orders item=Cafe qty=3   # key=value overrides
rist api PUT -m orders/1
rist api DELETE orders/1
rist api GET orders --raw       # raw (colored) JSON
```

`api` reads the running app's `/v3/api-docs`, so `-m` bodies always satisfy the
real request schema.

## Everything through `rist`

Any subcommand of the trio is reachable directly:

### jdp — dependency management

```bash
rist add starter-web              # add + verify chain + picker + role conflict
rist list                         # declared deps as a table
rist search jpa                   # Maven Central with ★ canonical highlight
rist rm log4j-core                # remove with fuzzy suggestions
rist why tomcat                   # transitive chain (dependency:tree, prettified)
rist weight -n 20                 # top jars by size
rist unused --clean               # declared deps without imports
rist doctor                       # CVE (OSV.dev) + outdated + score
rist doctor --fix                 # bump CVE deps to patched versions
rist diff org.springframework.boot spring-boot-starter-parent 3.3.4 3.4.0
rist migrate                      # maven → gradle (Kotlin DSL)
rist init my-api -t rest-api      # scaffold: rest-api | batch | lib
rist repl                         # interactive shell with artifact tab-complete
```

### xpresso — backend (Spring Boot)

```bash
rist new shop --group io.acme                 # scaffold project
rist g resource Product name:string price:decimal   # CRUD: model + service + controller + tests
rist g model Order user:belongs_to status:enum(NEW,PAID,SHIPPED)  # entity + DTOs + migration
rist g seed Product name:string price:decimal --count 30          # Faker factory + dev seeder
rist g controller User                        # DTO-based REST controller
rist g service Order                          # @Transactional @Service
rist g migration "add index"                  # blank Flyway migration
rist g auth                                   # AppUser + bcrypt + SecurityConfig
rist g endpoint users POST /{id}/archive      # add method to existing controller
rist g job / event / exception / config / component / test

rist server, s                                # spring-boot:run (auto-builds frontend)
rist console, c                               # Spring Shell / jshell fallback
rist build                                    # clean + package (skips tests)
rist test                                     # test suite
rist compile                                  # mvn compile (+ macc codegen if frontend)
rist clean --deep                             # mvn clean + node_modules
rist watch                                    # re-compile on every .java change
rist install                                  # clean + install + macc install

rist db migrate                               # Flyway migrate
rist db status                                # Flyway state
rist db rollback                              # Flyway undo
rist db clean                                 # Flyway clean (destructive)
rist db repair                                # Flyway repair

rist routes                                   # list endpoints (colour-coded by verb)
rist beans                                    # list @Service / @Repository / @Controller
rist config                                   # @ConfigurationProperties + application.yml
rist health                                   # curl /actuator/health, pretty-print
rist api GET products                         # hit the running app
rist api POST -m products                     # -m mocks valid body from OpenAPI
rist api GET products --raw                   # raw (colored) JSON

rist deps                                     # → jdp list
rist doctor --fix                             # → jdp doctor
rist profile add dev                          # create application-dev.yml
```

### macc — frontend (React)

```bash
rist codegen                  # scan @Page/@Model → emit .tsx + types + routes
rist dev                      # vite hot reload (frontend only)
```

### explicit tool prefix

For verbs that exist in more than one tool, target it explicitly:

```bash
rist xpresso g resource Post title:string   # force xpresso
rist macc add Button                        # force macc (not jdp's add)
rist jdp --help
```

## How it composes

```
        ┌──────────────────┐
        │  rist <command>  │
        └────────┬─────────┘
                 │
   ┌─────────────┼─────────────┐
   ▼             ▼             ▼
┌───────┐   ┌──────────┐   ┌────────┐
│  jdp  │   │ xpresso  │   │  macc  │
│ deps  │   │ backend  │   │ front  │
└───────┘   └──────────┘   └────────┘
                 │
                 ▼
      ┌─────────────────────────┐
      │   Spring Boot @ :8080   │
      │  + bundled React (vite) │
      └─────────────────────────┘
```

## Meta

```bash
rist version         # ristretto + jdp + xpresso + macc + JDK + Node
rist update          # update the trio to latest
rist install-tools   # build the trio from local source (offline)
```

Colors honor `NO_COLOR` and non-tty output. An opt-in GraalVM binary can be
built with `mvn -Pnative package` (requires `native-image`).

## Environment

Tools are located by:

1. `$JDP_HOME` / `$XPRESSO_HOME` / `$MACC_HOME` (if set)
2. `~/.local/share/<tool>/<tool>.jar`
3. `/usr/local/share/<tool>/<tool>.jar`
