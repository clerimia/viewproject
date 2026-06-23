# 景区导览 AI 数字人 — 本地开发启动指南

本项目推荐使用 **混合开发模式**：

```text
Docker：MySQL + Redis
本地：Spring Boot 后端 + Vue 前端 + Ollama + Whisper ASR + edge-tts
```

这样既保留数据库/缓存环境的一致性，又方便后端断点调试、前端热更新和语音服务排错。

---

## 1. 前置依赖

| 依赖 | 用途 |
|------|------|
| JDK 21 | 本地运行 Spring Boot |
| Node.js 20+ | 本地运行 Vue/Vite 前端 |
| Docker Desktop | 启动 MySQL、Redis，也可用于最终 Docker 验证 |
| Ollama | 本地 LLM 推理 |
| Python 3.10+ | 本地运行 Whisper ASR 与 edge-tts |
| Git Bash | Windows 下执行 `scripts/*.sh` 脚本 |

推荐模型：

```bash
ollama pull qwen2.5:3b
```

---

## 2. 环境变量配置

复制示例文件：

```bash
cp .env.example .env
```

本地开发推荐 `.env`：

```env
# Spring Boot
SERVER_PORT=8888

# Docker 基础设施
MYSQL_ROOT_PASSWORD=root123
MYSQL_PORT=3307
REDIS_PORT=6380

# Spring Boot 连接 MySQL / Redis
MYSQL_HOST=localhost
MYSQL_USER=root
MYSQL_PASSWORD=root123
REDIS_HOST=localhost

# Ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:3b

# RAG
RAG_BASE_URL=http://1.117.74.151:8081
RAG_USERNAME=admin
RAG_PASSWORD=替换为真实密码

# 语音服务
HF_ENDPOINT=https://hf-mirror.com
WHISPER_MODEL=tiny
EDGE_TTS_BIN=E:\Project\viewproject\.venv\Scripts\edge-tts.exe

# 默认管理员
ADMIN_INIT_ENABLED=true
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
ADMIN_BALANCE=100000
ADMIN_RESET_PASSWORD=false
```

说明：

| 变量 | 说明 |
|------|------|
| `SERVER_PORT` | 本地后端端口。前端 Vite 默认代理到 `8888` |
| `MYSQL_ROOT_PASSWORD` | Docker MySQL root 密码 |
| `MYSQL_PORT` | MySQL 映射到宿主机的端口，默认建议 `3307` |
| `REDIS_PORT` | Redis 映射到宿主机的端口，默认建议 `6380` |
| `OLLAMA_BASE_URL` | 本地 Ollama 地址 |
| `RAG_BASE_URL` | 外部 RAG 服务地址 |
| `HF_ENDPOINT` | Whisper 模型下载镜像，国内推荐 `https://hf-mirror.com` |
| `WHISPER_MODEL` | 开发推荐 `tiny`，演示可换 `small` |
| `EDGE_TTS_BIN` | `edge-tts.exe` 绝对路径，IDEA 启动后端时尤其推荐配置 |
| `ADMIN_RESET_PASSWORD` | 设为 `true` 时，每次后端启动都会重置默认管理员密码 |

---

## 3. 首次准备 Python 虚拟环境

```bash
cd /e/Project/viewproject
python -m venv .venv
source .venv/Scripts/activate
python -m pip install --upgrade pip
pip install faster-whisper edge-tts
```

验证：

```bash
edge-tts --version
python -c "from faster_whisper import WhisperModel; print('faster-whisper ok')"
```

Whisper 首次启动会下载模型。开发阶段建议使用：

```env
WHISPER_MODEL=tiny
HF_ENDPOINT=https://hf-mirror.com
```

---

## 4. 推荐启动顺序

### 4.1 启动 MySQL / Redis

```bash
bash scripts/dev-infra.sh
```

等价于：

```bash
docker compose up -d mysql redis
```

默认端口：

| 服务 | 宿主机端口 | 容器内端口 |
|------|------------|------------|
| MySQL | `3307` | `3306` |
| Redis | `6380` | `6379` |

### 4.2 启动 Whisper ASR 服务

```bash
bash scripts/dev-tts.sh
```

> 说明：脚本名保留为 `dev-tts.sh`，实际启动的是 Whisper ASR HTTP 服务。TTS 不需要单独服务，后端会调用 `edge-tts` 命令生成音频。

成功标志：

```text
Loading whisper model: tiny ...
Model loaded. Listening on :9876
```

### 4.3 启动 Spring Boot 后端

```bash
bash scripts/dev-backend.sh
```

脚本会自动：

1. 进入项目根目录；
2. 激活 `.venv`；
3. 读取 `.env`；
4. 执行 `./mvnw spring-boot:run`。

成功后 API 地址：

```text
http://localhost:8888
```

### 4.4 启动 Vue 前端

```bash
bash scripts/dev-frontend.sh
```

成功后访问：

```text
http://localhost:5173
```

> 如果前端先于后端启动，Vite 控制台可能出现 `http proxy error: ECONNREFUSED`。这表示后端当时还没启动完成，等后端启动后刷新页面即可。

---

## 5. IDEA 启动配置

推荐在 IDEA 中配置 4 个 Run Configuration。日常开发可以先启动 `infra`，等 MySQL / Redis 就绪后，再启动 `asr-whisper`、`backend` 和 `frontend`。

| 配置名 | 类型 | 作用 |
|--------|------|------|
| `infra` | Shell Script | 启动 MySQL + Redis |
| `asr-whisper` | Shell Script 或 Python | 启动 Whisper ASR HTTP 服务 |
| `backend` | Spring Boot | 启动后端，方便 Java 断点调试 |
| `frontend` | npm | 启动 Vue/Vite 前端 |

### 5.1 `infra`：Shell Script

```text
Run → Edit Configurations... → + → Shell Script
```

| 配置项 | 值 |
|--------|----|
| Name | `infra` |
| Script path | `E:\Project\viewproject\scripts\dev-infra.sh` |
| Working directory | `E:\Project\viewproject` |
| Interpreter path | `C:\Program Files\Git\bin\bash.exe` |

如果 Git Bash 不在上述路径，可尝试：

```text
C:\Program Files\Git\usr\bin\bash.exe
```

### 5.2 `asr-whisper`：推荐 Shell Script

```text
Run → Edit Configurations... → + → Shell Script
```

| 配置项 | 值 |
|--------|----|
| Name | `asr-whisper` |
| Script path | `E:\Project\viewproject\scripts\dev-tts.sh` |
| Working directory | `E:\Project\viewproject` |
| Interpreter path | `C:\Program Files\Git\bin\bash.exe` |

该脚本会自动激活 `.venv` 并读取 `.env`。成功标志：

```text
Model loaded. Listening on :9876
```

也可以使用 Python 配置：

| 配置项 | 值 |
|--------|----|
| Script path | `E:\Project\viewproject\scripts\whisper-server.py` |
| Working directory | `E:\Project\viewproject` |
| Python interpreter | `E:\Project\viewproject\.venv\Scripts\python.exe` |
| Environment variables | `HF_ENDPOINT=https://hf-mirror.com;WHISPER_MODEL=tiny` |

如果 IDEA 不能选择 `.venv\Scripts\python.exe`，直接使用 Shell Script 配置即可。

### 5.3 `backend`：Spring Boot

```text
Run → Edit Configurations... → + → Spring Boot
```

| 配置项 | 值 |
|--------|----|
| Name | `backend` |
| Main class | `com.aicust.AiCustApplication` |
| Working directory | `E:\Project\viewproject` |
| JRE | Java 21 |

IDEA 直接运行 Spring Boot 时不会自动读取 `.env`。推荐安装 EnvFile 插件并加载：

```text
E:\Project\viewproject\.env
```

如果不使用 EnvFile 插件，请在 Environment variables 中手动配置：

```text
SERVER_PORT=8888
MYSQL_HOST=localhost
MYSQL_PORT=3307
MYSQL_USER=root
MYSQL_PASSWORD=root123
REDIS_HOST=localhost
REDIS_PORT=6380
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:3b
RAG_BASE_URL=http://1.117.74.151:8081
RAG_USERNAME=admin
RAG_PASSWORD=替换为真实密码
EDGE_TTS_BIN=E:\Project\viewproject\.venv\Scripts\edge-tts.exe
ADMIN_INIT_ENABLED=true
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
ADMIN_RESET_PASSWORD=false
```

如果 IDEA 是单行输入框，使用分号分隔：

```text
SERVER_PORT=8888;MYSQL_HOST=localhost;MYSQL_PORT=3307;MYSQL_USER=root;MYSQL_PASSWORD=root123;REDIS_HOST=localhost;REDIS_PORT=6380;OLLAMA_BASE_URL=http://localhost:11434;OLLAMA_MODEL=qwen2.5:3b;EDGE_TTS_BIN=E:\Project\viewproject\.venv\Scripts\edge-tts.exe;ADMIN_INIT_ENABLED=true;ADMIN_USERNAME=admin;ADMIN_PASSWORD=admin123;ADMIN_RESET_PASSWORD=false
```

不要在 IDEA 环境变量中写：

```text
PATH=E:\Project\viewproject\.venv\Scripts;%PATH%
```

部分 IDEA 配置不会展开 `%PATH%`，可能导致：

```text
找不到文件: %
```

本项目已经支持 `EDGE_TTS_BIN`，所以无需改 PATH。

### 5.4 `frontend`：npm

```text
Run → Edit Configurations... → + → npm
```

| 配置项 | 值 |
|--------|----|
| Name | `frontend` |
| package.json | `E:\Project\viewproject\frontend\package.json` |
| Command | `run` |
| Scripts | `dev` |
| Working directory | `E:\Project\viewproject\frontend` |

### 5.5 可选：Compound 一键启动

可以新建：

```text
Run → Edit Configurations... → + → Compound
```

建议拆成两个：

| 配置名 | 包含 |
|--------|------|
| `dev-infra` | `infra` |
| `dev-app` | `asr-whisper`、`backend`、`frontend` |

先运行 `dev-infra`，等 MySQL / Redis healthy 后，再运行 `dev-app`。如果前端启动早于后端，Vite 可能短暂输出 `ECONNREFUSED`，后端启动完成后刷新页面即可。

---

## 6. 默认账号

后端启动时会自动初始化默认管理员账号：

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | `admin` | `admin123` | 需从登录页选择“管理端”登录 |
| 游客 | 自行注册 | 自行设置 | 注册入口只创建 `USER` 账号 |

如果数据库里已经存在 `admin`，默认不会覆盖密码。忘记密码时可临时设置：

```env
ADMIN_RESET_PASSWORD=true
```

重启后端，登录成功后建议改回：

```env
ADMIN_RESET_PASSWORD=false
```

---

## 7. 数字人配置

管理端路径：

```text
登录页 → 选择“管理端” → 数字人配置
```

可配置：

- 名称、欢迎语；
- 形象风格、性别、服装颜色；
- 自定义头像 URL；
- TTS 音色、语速、音调、音量；
- 是否激活。

游客端会调用：

```text
GET /api/digital-human/active
```

加载当前激活的数字人配置。若 `avatarImageUrl` 为空，前端会使用内置 2D 数字人组件。

---

## 8. 对话、思考过程与语音朗读

模型可能返回：

```text
<think>
这里是模型中间思考过程。
</think>
这里是正式回答。
```

前端处理规则：

| 内容 | 展示 | 朗读 | 记录/分析 |
|------|------|------|-----------|
| `<think>...</think>` 思考过程 | 单独以弱化样式展示，可折叠 | 不朗读 | 不进入对话记忆和情感分析 |
| 正式回答 | 正常对话气泡展示 | 可朗读 | 写入记忆、日志和分析 |

朗读按钮支持：

- 开始朗读；
- 暂停朗读；
- 继续朗读；
- 播放时驱动数字人口型动画；
- 暂停时停止口型动画。

ASR 只负责识别用户语音，不处理模型思考内容。

---

## 9. 常用验证命令

### 后端接口

```bash
curl -i http://localhost:8888/api/digital-human/active
```

正常返回示例：

```json
{"success":true,"data":{"name":"小云"}}
```

### Whisper ASR

```bash
curl -X POST http://127.0.0.1:9876 \
  -H "Content-Type: application/json" \
  -d '{"file":"E:/Project/viewproject/test.wav","language":"zh"}'
```

### TTS 命令

```bash
source .venv/Scripts/activate
edge-tts --text "你好，我是云隐山智能导览数字人。" --voice zh-CN-XiaoxiaoNeural --write-media test.mp3
```

### 构建验证

```bash
./mvnw -q -DskipTests compile
cd frontend && npm run build
```

---

## 10. Docker 全量运行 / 交付验证

日常开发推荐混合模式。提交前或演示前，可以用 Docker 全量验证：

```bash
docker compose up -d --build
```

查看状态：

```bash
docker compose ps
docker compose logs -f ai-cust
```

停止：

```bash
docker compose down
```

重置数据库和缓存：

```bash
docker compose down -v
```

---

## 11. 常见问题

### 前端提示 `http proxy error: ECONNREFUSED`

原因：前端已启动，但后端 `localhost:8888` 还没启动完成。

处理：等待后端启动完成后刷新页面；或按推荐顺序先启动后端再启动前端。

### 管理员 `admin/admin123` 登录不上

检查：

1. 登录页是否选择“管理端”；
2. 后端是否成功启动并执行初始化器；
3. 数据库是否已有旧的 `admin` 密码。

忘记密码时设置：

```env
ADMIN_RESET_PASSWORD=true
```

重启后端后再登录。

### IDEA 启动报 `找不到文件: %`

删除 Run Configuration 里的 `PATH=...%PATH%`，改用：

```env
EDGE_TTS_BIN=E:\Project\viewproject\.venv\Scripts\edge-tts.exe
```

### Whisper 下载超时

使用 Hugging Face 镜像：

```env
HF_ENDPOINT=https://hf-mirror.com
WHISPER_MODEL=tiny
```

模型下载成功一次后，后续会走本地缓存。
