#!/usr/bin/env bash
# Run TaskManager desktop app with local PostgreSQL on port 5433.
# Uses docker-compose for Postgres when not already running.

set -euo pipefail

cd "$(dirname "$0")/.."

export JAVA_HOME="$(./scripts/resolve-java21.sh)"
export PATH="${JAVA_HOME}/bin:${PATH}"
export TASKMANAGER_LOG_DIR="${PWD}/logs"

if [ -f ".env.local" ]; then
  set -a
  # shellcheck disable=SC1091
  source ".env.local"
  set +a
fi

# Default to the Docker-mapped Postgres port unless overridden locally.
export DB_PORT="${DB_PORT:-5433}"

# Start Docker Postgres if 5433 is not reachable
if ! nc -z "${DB_HOST:-localhost}" "${DB_PORT}" 2>/dev/null; then
  echo "PostgreSQL not on ${DB_HOST:-localhost}:${DB_PORT}. Starting via Docker..."
  docker compose up -d postgres
  echo "Waiting for Postgres to be ready..."
  sleep 5
fi

mkdir -p "${TASKMANAGER_LOG_DIR}"
LOG_FILE="${TASKMANAGER_LOG_DIR}/taskmanager-console.log"

./gradlew --console=plain :desktop-app:runDesktopApp "$@" 2>&1 | tee -a "${LOG_FILE}"
exit "${PIPESTATUS[0]}"
