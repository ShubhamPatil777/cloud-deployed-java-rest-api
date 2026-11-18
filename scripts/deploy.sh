#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed or not on PATH." >&2
  exit 1
fi

if git rev-parse --abbrev-ref --symbolic-full-name '@{u}' >/dev/null 2>&1; then
  git pull --ff-only
else
  echo "No git upstream is configured. Building the files already in ${ROOT}."
fi

docker compose up --build -d
docker compose ps

HOST_PORT="${SERVER_PORT:-8080}"
echo "Waiting for http://127.0.0.1:${HOST_PORT}/actuator/health"
for _ in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:${HOST_PORT}/actuator/health" >/dev/null; then
    echo "Application is healthy."
    curl -fsS "http://127.0.0.1:${HOST_PORT}/api/info"
    echo
    exit 0
  fi
  sleep 2
done

echo "The application did not become healthy. Recent logs:" >&2
docker compose logs --tail=100
exit 1
