# 04 知识库真实导入与 RAG 联调验证

## 目标

确认景区知识文档已经真实导入 RAG/Qdrant，并且 `/rag/search` 能稳定召回文档，支持分类过滤。

## 当前情况

仓库已有知识文档和导入脚本，但需要确认是否已经在运行环境中成功导入。

## 涉及文件

- `knowledge-docs/`
- `scripts/import-knowledge.sh`
- 可新增：`scripts/test-rag-search.sh`
- 配置文件：`src/main/resources/application*.properties` 或 `application*.yml`

## 实施步骤

### 步骤 1：确认 RAG 服务配置

检查配置中是否有：

```properties
rag.base-url=http://1.117.74.151:8081
rag.username=
rag.password=
```

确认 Ai-cust 后端和 RAG 服务机器网络互通。

### 步骤 2：执行知识文档导入

使用已有脚本导入 `knowledge-docs/` 下的 Markdown 文档。

如果脚本缺少分类映射，需要确保导入时传入：

- 综合信息
- 历史文化
- 自然风光
- 美食特产

### 步骤 3：新增 RAG 搜索测试脚本

新增文件：

```text
scripts/test-rag-search.sh
```

建议包含以下测试：

```bash
#!/usr/bin/env bash
set -euo pipefail

RAG_BASE_URL="${RAG_BASE_URL:-http://1.117.74.151:8081}"

search() {
  local query="$1"
  local category="${2:-}"

  if [ -n "$category" ]; then
    curl -s "$RAG_BASE_URL/rag/search" \
      -H 'Content-Type: application/json' \
      -d "{\"query\":\"$query\",\"topK\":5,\"filter\":{\"conditions\":[{\"field\":\"category\",\"op\":\"eq\",\"value\":\"$category\"}],\"logic\":\"AND\"}}" | jq .
  else
    curl -s "$RAG_BASE_URL/rag/search" \
      -H 'Content-Type: application/json' \
      -d "{\"query\":\"$query\",\"topK\":5}" | jq .
  fi
}

search "云隐山景区介绍"
search "云隐山有什么历史故事" "历史文化"
search "云隐山春天有什么自然风光" "自然风光"
search "云隐山有什么美食" "美食特产"
```

### 步骤 4：记录联调结果

可新增：

```text
docs/rag-verification.md
```

记录：

- 导入时间
- 导入文档数量
- RAG 服务地址
- Qdrant collection 名称
- 搜索样例
- 每类搜索命中结果

## 验收标准

1. 执行导入脚本成功。
2. `/rag/search` 返回 `success = true`。
3. `hits` 数量大于 0。
4. 分类过滤能返回对应分类文档。
5. Ai-cust 对话中能引用对应知识内容。

## 风险点

- 远程 RAG 服务可能不可达。
- Qdrant collection 可能为空。
- RAG API 的 filter 格式可能与当前 `RagSearchService` 拼接格式不完全一致，需要联调确认。
- Windows 环境运行 `.sh` 需要 Git Bash 或 WSL。
