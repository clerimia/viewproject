#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .venv/Scripts/activate ]; then
  # Windows Git Bash
  source .venv/Scripts/activate
elif [ -f .venv/bin/activate ]; then
  # Linux/macOS/WSL
  source .venv/bin/activate
else
  echo "未找到 Python 虚拟环境 .venv，请先执行：python -m venv .venv && source .venv/Scripts/activate && pip install faster-whisper edge-tts" >&2
  exit 1
fi

if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

: "${HF_ENDPOINT:=https://hf-mirror.com}"
: "${WHISPER_MODEL:=tiny}"

export HF_ENDPOINT WHISPER_MODEL

python scripts/whisper-server.py
