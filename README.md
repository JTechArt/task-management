# Task Management

This repository contains the `taskmanager` application, a Kotlin/JVM 21 desktop app built with Compose Desktop.

Project layout:

- `taskmanager/core`: domain logic, config loading, persistence, Git/workspace services, tests
- `taskmanager/desktop-app`: desktop UI, app bootstrap, view models, log configuration
- `taskmanager/scripts`: local run, debug, setup, and packaging scripts

## Requirements

- Java 21
- Docker Desktop or another Docker runtime

The app scripts in `taskmanager/scripts` resolve Java 21 automatically.

## First-time setup

```bash
cd taskmanager
cp .env.example .env.local
docker compose up -d postgres
./gradlew :desktop-app:build
```

## Recommended ways to run

For normal local development:

```bash
./taskmanager/scripts/run-local.sh
```

What it does:

- resolves Java 21
- loads `.env.local` if present
- starts Postgres with `docker compose` if needed
- runs `:desktop-app:runDesktopApp`
- writes console output to `taskmanager/logs/taskmanager-console.log`

For debugger attach on port `5005`:

```bash
./taskmanager/scripts/debug-local.sh
```

To verify Java 21 and run a clean build:

```bash
./taskmanager/scripts/setup-java21.sh
```

## Cursor / VS Code launch configs

The workspace launch configs are set up for the common cases:

- `TaskManager: Run Local App`
- `TaskManager: Debug Local App`
- `TaskManager: Run App Without DB Bootstrap`

Use `Run Local App` for most work. It is the safest default.

## Manual Gradle commands

Use these when you want explicit control or a quick validation pass:

```bash
./gradlew :desktop-app:build
./gradlew :desktop-app:runDesktopApp
./gradlew :desktop-app:runDesktopAppNoDb
./gradlew :core:test
./gradlew test
```

You usually do not need a separate manual build before `run-local.sh` because Gradle will compile what is needed before launch.

## Environment variables

| Name | Default | Purpose |
| --- | --- | --- |
| `APP_NAME` | `TaskManager` | Window title and app id |
| `DB_HOST` | `localhost` | Database host |
| `DB_PORT` | `5433` in local scripts | Local Docker port |
| `DB_NAME` | `taskmanager` | Database name |
| `DB_USER` | `taskmanager` | Database user |
| `DB_PASSWORD` | `taskmanager_local` | Database password |
| `DB_MAX_POOL_SIZE` | `10` | Hikari maximum pool size |
| `DB_MIN_IDLE` | `2` | Hikari minimum idle connections |
| `DB_CONNECTION_TIMEOUT` | `30000` | Connection timeout in ms |
| `DB_IDLE_TIMEOUT` | `600000` | Idle timeout in ms |
| `DB_MAX_LIFETIME` | `1800000` | Max lifetime in ms |
| `BOOTSTRAP_DATABASE` | `true` | Run Flyway and DB bootstrap on startup |

## Database

Start only Postgres:

```bash
docker compose up -d postgres
```

Stop it:

```bash
docker compose down
```

The local Docker mapping is `5433 -> 5432`.

## Logs

Local run and debug scripts append console output to:

```bash
taskmanager/logs/taskmanager-console.log
```

## Packaging

The application is packaged into installable distributions for Windows (MSI), macOS (DMG), and Linux (.deb, .rpm) using Compose Desktop and jlink. Package version comes from the project version (`gradle.properties` / root `version`).

### Build commands

Build the desktop artifact:

```bash
cd taskmanager
./gradlew :desktop-app:build
```

Build the native package for the current OS:

```bash
./taskmanager/scripts/build-jpackage.sh
```

Or directly via Gradle:

```bash
./gradlew :desktop-app:packageDistributionForCurrentOS
```

Outputs are written to `taskmanager/desktop-app/build/compose/binaries/` (e.g. `main/dmg`, `main/msi`, `main/deb`, `main/rpm`).

### Runtime dependencies (included or required)

| Dependency | Included in package? | Notes |
| --- | --- | --- |
| Java Runtime | Yes | Bundled via jlink; no separate JDK required |
| PostgreSQL | No | User must run PostgreSQL (local or Docker). See [Database](#database). |
| Git | No | Required for workspace generation; must be installed on the system |

Configuration (DB connection, OAuth, etc.) is read from environment variables at runtime. No secrets or credentials are bundled in the packaged output.

### Validation

After building, run the packaged app to confirm startup:

```bash
# From the built image (path varies by format)
taskmanager/desktop-app/build/compose/binaries/main/dmg/TaskManager.app/Contents/MacOS/TaskManager  # macOS
# Or install the .dmg/.msi/.deb/.rpm and launch from the OS.
```
