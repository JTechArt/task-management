# TaskManager

TaskManager is a Kotlin/JVM 21 desktop app built with Compose Desktop. The project is split into:

- `core`: domain logic, config loading, persistence, Git/workspace services, tests
- `desktop-app`: desktop UI, app bootstrap, view models, log configuration

## Requirements

- Java 21
- Docker Desktop or another Docker runtime

The repo includes `scripts/resolve-java21.sh`, and the run/debug scripts use it automatically.

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
./run-local.sh
```

What it does:

- resolves Java 21
- loads `.env.local` if present
- starts Postgres with `docker compose` if needed
- runs `:desktop-app:runDesktopApp`
- writes console output to `logs/taskmanager-console.log`

For debugger attach on port `5005`:

```bash
./scripts/debug-local.sh
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
logs/taskmanager-console.log
```

## Packaging

Build the desktop artifact:

```bash
./gradlew :desktop-app:build
```
