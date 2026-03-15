# Cursor Setup Guide

This project is a Kotlin/Compose desktop app in [`taskmanager`](/Users/arthurho/Projects/task-management/taskmanager). The most common startup failures are:

- Java 17 is active instead of Java 21.
- Cursor launch settings point to a machine-specific JDK path.
- Database environment values are wrong or missing.
- Gradle run/debug commands target a task that does not exist.

## 1. Verify Java 21

Run:

```bash
./taskmanager/scripts/setup-java21.sh
```

That script locates a local JDK 21, verifies it, and runs a Gradle build with it.

If Java 21 is installed in a custom location, set:

```bash
export TASKMANAGER_JAVA_HOME="/path/to/jdk-21"
```

## 2. Configure local environment

Copy the example config:

```bash
cp taskmanager/.env.example taskmanager/.env.local
```

Default values:

```dotenv
APP_NAME=TaskManager
DB_HOST=localhost
DB_PORT=5433
DB_NAME=taskmanager
DB_USER=taskmanager
DB_PASSWORD=taskmanager_local
BOOTSTRAP_DATABASE=true
```

Notes:

- Use `DB_PORT=5433` when running the included Docker PostgreSQL service.
- Set `BOOTSTRAP_DATABASE=false` if you only want to bring up the UI without DB initialization.

## 3. Run from Cursor

Open the repo root in Cursor, then use one of these:

- `Terminal -> Run Task -> TaskManager: Run Local Desktop App`
- `F5 -> TaskManager: Debug Local App`
- `Run and Debug -> TaskManager: Run App Without DB Bootstrap`

The Cursor tasks call repo-local scripts instead of hardcoded absolute JDK paths.

## 4. Manual commands

Start Postgres:

```bash
cd taskmanager
docker compose up -d postgres
```

Run app:

```bash
cd taskmanager
./scripts/run-local.sh
```

Run app without DB bootstrap:

```bash
cd taskmanager
JAVA_HOME="$(./scripts/resolve-java21.sh)" PATH="$(./scripts/resolve-java21.sh)/bin:${PATH}" ./gradlew --console=plain :desktop-app:runDesktopAppNoDb
```

Start debug JVM and wait for Cursor to attach on port `5005`:

```bash
cd taskmanager
./scripts/debug-local.sh
```

## 5. If startup still fails

Check these in order:

1. `java -version` and `./taskmanager/scripts/setup-java21.sh`
2. `taskmanager/.env.local`
3. `docker compose ps`
4. `cd taskmanager && ./gradlew :desktop-app:build -x test`

If the build fails before startup, it is a Gradle/config issue rather than a Cursor issue.

## Logs

Local runs now write logs to:

- `taskmanager/logs/taskmanager-console.log`
- `taskmanager/logs/taskmanager.log`

If a workspace clone fails, check those files first.
