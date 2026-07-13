<div align="center">

<p align="center"><img src="rist.png" alt="Ristretto" width="320"></p>

# Ristretto

### One shot. Three espressos.

[![JDK](https://img.shields.io/badge/JDK-17+-007396?style=for-the-badge&logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](./LICENSE)

</div>

`ristretto` bundles **[jdp](https://github.com/Nandobez/jdp)** (deps)
\+ **[Xpresso](https://github.com/Nandobez/Xpresso)** (backend)
\+ **[Macchiato](https://github.com/Nandobez/Macchiato)** (frontend) into a
single CLI, and adds a **fullstack lifecycle** on top: scaffold, run, inspect,
and hit your API — all through one command.

Download **one** tool, drive **all three**.

Aliases: `ristretto` · `rist` · `r`.

<p align="center"><video src="rist_min.mp4" controls width="640"></video></p>

## Install

```bash
curl -fsSL https://raw.githubusercontent.com/Nandobez/Ristretto/main/install.sh | bash
```

The installer pulls **jdp**, **xpresso**, **macc**, then builds and installs
**ristretto** itself. Already have the trio? Run `ristretto install-tools` to
build them from local source, or `ristretto update` to fetch the latest.

## 60-second backend

```bash
r new shop --group io.acme --no-frontend
cd shop
r resource Order item:string qty:int paid:bool   # full CRUD, tests, error handler
r g seed Order item:string qty:int paid:bool      # fake data on dev startup
r up                                              # build + run detached
r api GET orders                                  # hit the live API
r down
```

`r resource` generates a complete, compiling CRUD slice:

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
| `r up [--profile dev] [--port N] [--build]` | package (if needed) + run **detached**; shows a ready banner with Local/Network/Swagger/Health URLs |
| `r status` | running (pid · uptime) with the same banner, or `stopped` |
| `r logs [-f] [-n N] [--raw]` | app logs reformatted `time · level · message` |
| `r down` | stop the app started by `up` |
| `r serve` | dev mode: backend **and** frontend in parallel |

State lives under `.ristretto/` (git-ignored automatically).

## Talk to your API

```bash
r api GET orders             # aligned table + pagination
r api GET orders/1           # single record
r api POST -m orders         # -m fills a valid mock body from OpenAPI
r api POST -m orders item=Cafe qty=3   # key=value overrides
r api PUT -m orders/1
r api DELETE orders/1
r api GET orders --raw       # raw (colored) JSON
```

`api` reads the running app's `/v3/api-docs`, so `-m` bodies always satisfy the
real request schema.

## Everything through `r`

Any subcommand of the trio is reachable directly:

```bash
r add starter-actuator       # → jdp add
r list · r why · r doctor    # → jdp
r g model User name:string   # → xpresso g model
r routes · r beans · r db info · r test · r compile   # → xpresso
r codegen · r dev            # → macc
```

For verbs that exist in more than one tool, target it explicitly:

```bash
r xpresso g resource Post title:string   # force xpresso
r macc add Button                        # force macc (not jdp's add)
r jdp --help
```

## How it composes

```
        ┌──────────────────┐
        │   r <command>    │
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
r version         # ristretto + jdp + xpresso + macc + JDK + Node
r update          # update the trio to latest
r install-tools   # build the trio from local source (offline)
```

Colors honor `NO_COLOR` and non-tty output. An opt-in GraalVM binary can be
built with `mvn -Pnative package` (requires `native-image`).

## Environment

Tools are located by:

1. `$JDP_HOME` / `$XPRESSO_HOME` / `$MACC_HOME` (if set)
2. `~/.local/share/<tool>/<tool>.jar`
3. `/usr/local/share/<tool>/<tool>.jar`
