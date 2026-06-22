# 景区导览 AI 数字人 — 启动方法

## 前置依赖

| 依赖 | 用途 |
|------|------|
| Docker Desktop | 运行全部服务（应用 + MySQL + Redis + Whisper + TTS） |
| Ollama | 本地 LLM 推理（本机运行，容器通过 host.docker.internal 连接） |
| ollama model | `qwen2.5:7b`（或 `qwen2.5:3b`） |

不需要单独安装 Java、Python、MySQL、Redis——全在 Docker 里。

---

## 1. 一键启动

```bash
cd "C:/Users/fan/OneDrive/Desktop/新建文件夹 (2)"

# 构建并启动全部服务
docker compose up -d
```

首次运行会：

1. 构建应用镜像（Maven 编译 + Python3 + ffmpeg + edge-tts + Whisper 模型下载）
2. 启动 MySQL（自动建库 `ai_cust`）
3. 启动 Redis
4. 启动应用（Spring Boot + Whisper HTTP 服务）

第一次构建约 5-10 分钟（后续构建有缓存，10 秒内完成）。

构建过程中关键步骤：

- `pip install edge-tts faster-whisper`
- `python3 -c "...WhisperModel('small'...)"` → 下载 Whisper small 模型约 1GB
- `mvn package -DskipTests`

构建完成后的镜像大小约 **1.5 GB**（含 JRE + Python + Whisper 模型）。

---

## 2. 验证启动

```bash
# 容器状态
docker ps --filter "name=cust-"

# 期望输出：cust-app、cust-mysql、cust-redis 都是 Up / Healthy
```

### 访问地址

| 页面 | 地址 | 说明 |
|------|------|------|
| 游客端 | `http://localhost:8888/` | 景区 AI 导览聊天页 |
| 管理后台 | `http://localhost:8888/admin.html` | 知识库管理 + 数字人配置 |
| 数据大屏 | `http://localhost:8888/dashboard.html` | 服务数据看板 |

### 命令行验证

```bash
# 首页
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8888/
# → 200

# 数字人配置接口
curl -s http://localhost:8888/api/digital-human/active
# → {"success":false,"message":"暂无激活的数字人配置"}

# Whisper ASR 状态
docker exec cust-app curl -s -X POST http://127.0.0.1:9876 \
  -H "Content-Type: application/json" \
  -d '{"file":"/tmp/test.wav","language":"zh"}'
# → 模型已加载，返回识别结果

# TTS 状态
docker exec cust-app edge-tts --version
# → edge-tts 7.2.8
```

---

## 3. 语音服务（全在容器内）

### 架构

```
游客端浏览器
  ├── 文字输入     → POST /api/chat          → Ollama (LLM)
  ├── 点击朗读按钮 → POST /api/speech/synthesize → edge-tts (容器内) → 返回 mp3
  └── 按住麦克风   → POST /api/speech/recognize  → Whisper :9876 (容器内) → 返回文本
```

| 服务 | 方式 | 状态 |
|------|------|------|
| LLM 对话 | Ollama 本机 `host.docker.internal:11434` | 需本机运行 Ollama |
| TTS 朗读 | 容器内 `edge-tts` CLI，调用微软免费 TTS | 镜像已内置 |
| ASR 识别 | 容器内 `whisper-server.py` 常驻 :9876 | 镜像已内置 Whisper small 模型 |

**不需要单独启动任何语音服务**——应用容器在启动时自动拉起了 Whisper 服务器。

---

## 4. 管理员账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| `admin` | `admin123` | ADMIN | 可访问管理后台和数据大屏 |
| `testuser` | `test123` | USER | 只能访问游客端聊天 |

新注册用户默认为 USER，如需管理员：

```sql
docker exec cust-mysql mysql -uroot -proot123 -e "UPDATE ai_cust.users SET role = 'ADMIN' WHERE username = '用户名';"
```

---

## 5. RAG 知识库

如果远程 RAG 服务已就绪且有数据，应用会自动使用。

如果要重新导入云隐山知识文档：

```bash
export RAG_BASE_URL=http://1.117.74.151:8081
export RAG_USERNAME=admin
export RAG_PASSWORD=rag2024
./scripts/import-knowledge.sh
```

验证 RAG 检索：

```bash
export RAG_BASE_URL=http://1.117.74.151:8081
export RAG_USERNAME=admin
export RAG_PASSWORD=rag2024
./scripts/test-rag-search.sh
```

---

## 6. 数字人配置

管理员登录 → 管理后台 → 数字人配置 → 新建配置 → 启用。

启用后游客端会自动加载数字人名称、欢迎语、头像、音色和语速。

---

## 7. 常用命令

```bash
# 查看日志
docker logs cust-app -f

# 重启应用
docker compose restart ai-cust

# 停止全部服务
docker compose down

# 停止并删除数据卷（重置数据库）
docker compose down -v

# 重新构建镜像（代码改动后）
docker compose build ai-cust && docker compose up -d

# 进入容器调试
docker exec -it cust-app bash
```

---

## 8. `.env` 配置

```text
MYSQL_ROOT_PASSWORD=root123
MYSQL_PORT=3306
REDIS_PORT=6381
APP_PORT=8888
OLLAMA_HOST=host.docker.internal
OLLAMA_MODEL=qwen2.5:7b
RAG_BASE_URL=http://1.117.74.151:8081
RAG_USERNAME=admin
RAG_PASSWORD=rag2024
HF_ENDPOINT=https://huggingface.co
```

| 变量 | 说明 |
|------|------|
| `APP_PORT` | 应用对外端口，默认 8888 |
| `OLLAMA_HOST` | Ollama 地址，`host.docker.internal` = 本机 |
| `OLLAMA_MODEL` | 使用的 LLM 模型 |
| `RAG_BASE_URL` | 外部 RAG 服务地址 |
| `HF_ENDPOINT` | Whisper 模型下载地址（`https://huggingface.co` 或 `https://hf-mirror.com`） |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `REDIS_PORT` | Redis 宿主机端口（容器内固定 6379） |

---

## 9. 端口说明

| 端口 | 用途 |
|------|------|
| 8888 | 应用对外访问 |
| 3306 | MySQL（宿主机） |
| 6381 | Redis（宿主机，容器内为 6379） |
| 9876 | Whisper HTTP（容器内） |
| 11434 | Ollama（本机） |

---

## 10. 故障排查

### 端口冲突

```bash
netstat -ano | grep ":8888"
taskkill //PID 进程ID //F
```

或修改 `.env` 中的 `APP_PORT`。

### 容器无法连接 Ollama

确保本机 Ollama 在运行：

```bash
ollama list
ollama run qwen2.5:7b
```

如果 Ollama 在其他机器，修改 `.env` 中的 `OLLAMA_HOST`。

### 重新构建镜像

代码改动后：

```bash
docker compose build ai-cust && docker compose up -d
```

只改了前端 HTML，不需要重新构建——静态资源在容器外也能通过 volume 挂载，但目前 compose 没有挂载 volume，所以改代码后需要重新 build。

如果频繁改前端，可以临时加 volume：

```yaml
# docker-compose.yml ai-cust 服务下加：
volumes:
  - ./src/main/resources/static:/app/BOOT-INF/classes/static
```

### HuggingFace 下载失败

如果 `hf-mirror.com` 和 `huggingface.co` 都不能用：

```bash
# 方法1：改 .env 尝试其他镜像
HF_ENDPOINT=https://hf.itate.chat

# 方法2：设代理
# 在 Docker Desktop Settings → Resources → Proxies 中配置 HTTP 代理
```

---

## 11. 本次实际配置（2026-06-22）

**访问地址**：`http://localhost:8888`

**启动命令**：

```bash
cd "C:/Users/fan/OneDrive/Desktop/新建文件夹 (2)"
docker compose up -d
```

**运行状态**：

| 容器 | 状态 | 端口 |
|------|------|------|
| cust-app | Up | 8888 (应用) + 9876 (Whisper 容器内) |
| cust-mysql | Healthy | 3306 |
| cust-redis | Healthy | 6381 |
| Ollama (本机) | 运行中 | 11434 |

**已包含功能**：

- 游客端景区导览聊天（含 RAG 参考资料展示）
- 兴趣模式选择（历史/自然/美食）
- 数字人状态动画
- TTS 语音朗读（edge-tts）
- ASR 语音识别（Whisper small）
- 管理后台（知识库管理 + 数字人配置）
- 数据大屏
- 游客满意度评价
- 知识库本地索引管理
