#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .venv/Scripts/activate ]; then
  # Windows Git Bash
  source .venv/Scripts/activate
elif [ -f .venv/bin/activate ]; then
  # Linux/macOS/WSL
  source .venv/bin/activate
fi

if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

./mvnw spring-boot:run
