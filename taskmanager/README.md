# TaskManager Scaffold

Kotlin 2 / JVM 21 Compose Desktop scaffold with env-driven config, database wiring, Docker, and jpackage skeleton.

## Modules
- `core`: Configuration loader, database factory (Hikari + Flyway + Exposed types), logging helpers.
- `desktop-app`: Compose Desktop entry point, logback config, startup smoke hook.

## Quick start
```bash
cd taskmanager
./gradlew :desktop-app:build
java -jar desktop-app/build/libs/desktop-app-0.1.0-SNAPSHOT.jar
```

## Environment variables
| Name | Default | Purpose |
| --- | --- | --- |
| `APP_NAME` | TaskManager | Window title and app id |
| `DB_HOST` | localhost | Database host |
| `DB_PORT` | 5432 | Database port (use 5433 for Docker: `docker compose` maps 5433→5432) |
| `DB_NAME` | taskmanager | Database name |
| `DB_USER` | taskmanager | Database user |
| `DB_PASSWORD` | taskmanager_local | Database password (not logged) |
| `DB_MAX_POOL_SIZE` | 10 | Hikari maximumPoolSize |
| `DB_MIN_IDLE` | 2 | Hikari minimumIdle |
| `DB_CONNECTION_TIMEOUT` | 30000 | Hikari connectionTimeout (ms) |
| `DB_IDLE_TIMEOUT` | 600000 | Hikari idleTimeout (ms) |
| `DB_MAX_LIFETIME` | 1800000 | Hikari maxLifetime (ms) |
| `BOOTSTRAP_DATABASE` | true | Run Flyway + connect on startup (set `false` to disable) |

## Run with database (local development)
```bash
# Option 1: Use run script (starts Postgres via Docker if needed)
./run-local.sh

# Option 2: Manual - start Postgres first, then run app
docker compose up -d postgres
# Wait for Postgres, then run with port 5433 (Docker mapping):
DB_PORT=5433 ./gradlew :desktop-app:run
```

## Docker
```bash
docker-compose build
docker-compose up
```

## Jpackage skeleton
```bash
./script/build-jpackage.sh
```

## Smoke test
```bash
./gradlew :desktop-app:test
```

