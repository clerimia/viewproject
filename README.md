# 云隐山智能导览数字人 - A5 景区导览服务 AI 数字人

锐捷网络（苏州）有限公司 | 项目编号：1093135039

## 项目简介

本项目利用 AI 数字人技术，构建智能、互动、个性化的景区导览服务系统。游客可通过语音或文字与数字人交互，获得景区历史、文化、景点、美食等方面的智能导览服务。

## 技术栈

### 后端
- Spring Boot 3.3.5
- Spring Security + JWT
- Spring Data JPA (MySQL 8.0)
- Redis 7
- WebFlux SSE 流式响应

### 前端
- Vue 3 + Vite 6 + TypeScript
- Tailwind CSS 4
- Pinia 状态管理
- Vue Router

### AI 服务
- RAG 检索增强（外部服务）
- Ollama LLM (qwen2.5:7b)
- Edge TTS 语音合成
- Faster-Whisper ASR 语音识别

## 快速启动

详细启动说明见：[docs/startup-guide.md](docs/startup-guide.md)。

### 推荐：本地混合开发模式

```text
Docker：MySQL + Redis
本地：Spring Boot 后端 + Vue 前端 + Ollama + Whisper ASR + edge-tts
```

### 前置要求

- JDK 21
- Node.js 20+
- Docker + Docker Compose
- Python 3.10+
- Ollama
- Git Bash（Windows 下运行 `scripts/*.sh`）

### 1. 准备环境变量

```bash
cp .env.example .env
```

按本机情况修改 `.env`，尤其是：

```env
MYSQL_ROOT_PASSWORD=root123
MYSQL_PORT=3307
REDIS_PORT=6380
EDGE_TTS_BIN=E:\Project\viewproject\.venv\Scripts\edge-tts.exe
RAG_PASSWORD=your_rag_password
```

### 2. 准备 Python 虚拟环境（首次）

```bash
python -m venv .venv
source .venv/Scripts/activate
python -m pip install --upgrade pip
pip install faster-whisper edge-tts
```

### 3. 按顺序启动开发服务

```bash
# MySQL + Redis
bash scripts/dev-infra.sh

# Whisper ASR 服务（TTS 不需要单独服务，后端会调用 edge-tts）
bash scripts/dev-tts.sh

# Spring Boot 后端
bash scripts/dev-backend.sh

# Vue/Vite 前端
bash scripts/dev-frontend.sh
```

前端访问：`http://localhost:5173`  
后端 API：`http://localhost:8888`

### 4. 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 登录页选择“管理端” |
| 游客 | 自行注册 | 自行设置 | 注册入口只创建游客账号 |

如数据库中已有旧 `admin` 密码，可临时设置 `ADMIN_RESET_PASSWORD=true` 后重启后端重置密码。

## 功能模块

### 游客端

- [x] 多模态交互（文字 + 语音输入）
- [x] AI 智能问答与讲解
- [x] 个性化推荐（历史/自然/美食模式）
- [x] 数字人语音回答（语音合成 + 口型同步）
- [x] 数字人表情变化（待机/聆听/思考/讲解）
- [x] 满意度评价

### 管理后台

- [x] 知识库管理（上传文档 / 文本导入 / 删除）
- [x] 数字人形象配置（外观/服装/声音/语速等）
- [x] 游客情感报告（满意度趋��/关注点分析）
- [x] 数据大屏

## API 接口文档

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 登录 |
| POST | /api/auth/register | 注册（游客） |
| GET | /api/auth/captcha | 获取验证码 |

### 聊天

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/chat | 流式对话（SSE） |

### 语音

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/speech/synthesize | TTS 语音合成 |
| POST | /api/speech/recognize | ASR 语音识别 |

### 管理接口（需 ADMIN 权限）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | /api/admin/digital-human | 数字人配置 CRUD |
| POST | /api/admin/digital-human/{id}/activate | 激活数字人配置 |
| GET | /api/admin/dashboard | 数据大屏 |
| GET | /api/admin/report/sentiment | 情感报告 |

## 项目结构

```
├── src/main/java/com/aicust/
│   ├── config/          # 配置类
│   ├── controller/      # 控制器
│   ├── dto/             # DTO
│   ├── model/           # 实体类
│   ├── repository/      # 数据访问
│   ├── security/        # 安全认证
│   └── service/         # 业务逻辑
├─��� frontend/
│   ├── src/
│   │   ├── api/         # API 调用
│   │   ├── components/  # 组件
│   │   ├── composables/ # 组合式函数
│   │   ├── router/      # 路由
│   │   ├── stores/      # 状态管理
│   │   ├── styles/      # 样式
│   │   ├── types/       # 类型定义
│   │   └── views/       # 页面视图
│   └── vite.config.ts   # Vite 配置
├── docker-compose.yml   # Docker 编排
├── Dockerfile           # 后端镜像
└── README.md            # 本文件
```

## 部署

### 生产构建

```bash
# 后端 Docker 构建
docker compose build ai-cust

# 前端构建（已集成到 Docker）
# 构建产物会自动复制到后端 static 目录
```

### 环境变量

本地开发优先修改 `.env`，不要直接改 `application.yml`。常用变量：

```env
SERVER_PORT=8888
MYSQL_HOST=localhost
MYSQL_PORT=3307
REDIS_HOST=localhost
REDIS_PORT=6380
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:3b
RAG_BASE_URL=http://1.117.74.151:8081
EDGE_TTS_BIN=E:\Project\viewproject\.venv\Scripts\edge-tts.exe
```

## 常见问题

### 1. 前端无法访问后端 API

开发模式下，确保后端已启动在 `8888`：

```bash
curl -i http://localhost:8888/api/digital-human/active
```

如果 Vite 输出 `http proxy error: ECONNREFUSED`，通常是前端启动早于后端。等后端启动完成后刷新页面即可。

### 2. 语音功能无法使用

确保已创建虚拟环境并安装依赖：

```bash
source .venv/Scripts/activate
pip install faster-whisper edge-tts
```

启动 ASR：

```bash
bash scripts/dev-tts.sh
```

TTS 不需要单独启动服务，但后端需要配置 `EDGE_TTS_BIN` 指向 `.venv` 中的 `edge-tts.exe`。

### 3. 注册失败显示 500 错误

检查数据库是否有重复用户名：
```bash
docker exec cust-mysql mysql -uroot -proot123 ai_cust -e "SELECT username, COUNT(*) FROM users GROUP BY username HAVING COUNT(*) > 1;"
```

## 许可证

本项目仅供学习和演示使用。