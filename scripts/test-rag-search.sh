#!/usr/bin/env bash
set -euo pipefail

RAG_BASE_URL="${RAG_BASE_URL:-http://1.117.74.151:8081}"
AUTH_ARGS=()

if [[ -n "${RAG_USERNAME:-}" ]]; then
  AUTH_ARGS=(-u "${RAG_USERNAME}:${RAG_PASSWORD:-}")
fi

post_search() {
  local title="$1"
  local query="$2"
  local category="${3:-}"
  local payload

  if [[ -n "$category" ]]; then
    payload=$(cat <<JSON
{"query":"$query","topK":5,"filter":{"conditions":[{"field":"category","op":"eq","value":"$category"}],"logic":"AND"}}
JSON
)
  else
    payload=$(cat <<JSON
{"query":"$query","topK":5}
JSON
)
  fi

  echo
  echo "===== $title ====="
  echo "query: $query"
  [[ -n "$category" ]] && echo "category: $category"

  curl -sS "${AUTH_ARGS[@]}" \
    -H 'Content-Type: application/json' \
    -X POST "${RAG_BASE_URL}/rag/search" \
    -d "$payload" | pretty_json
}

pretty_json() {
  if command -v jq >/dev/null 2>&1; then
    jq '{success, hitCount, hits: (.hits // [] | map({title, category, fusedScore, text: (.text[0:120] + "...")}))}'
  else
    python -m json.tool
  fi
}

echo "RAG_BASE_URL=${RAG_BASE_URL}"
post_search "综合召回" "云隐山景区介绍"
post_search "历史文化分类召回" "云隐山有什么历史故事" "历史文化"
post_search "自然风光分类召回" "云隐山春天有什么自然风光" "自然风光"
post_search "美食特产分类召回" "云隐山有什么美食和伴手礼" "美食特产"
