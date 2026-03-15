#!/usr/bin/env bash
# Run TaskManager desktop app with local PostgreSQL on port 5433.
# Uses docker-compose for Postgres when not already running.

set -e

cd "$(dirname "$0")"

# Always use port 5433 (Docker maps container 5432 -> host 5433)
export DB_PORT=5433

# Start Docker Postgres if 5433 is not reachable
if ! nc -z localhost 5433 2>/dev/null; then
  echo "PostgreSQL not on 5433. Starting via Docker..."
  docker compose up -d postgres
  echo "Waiting for Postgres to be ready..."
  sleep 5
fi

./gradlew :desktop-app:run "$@"
