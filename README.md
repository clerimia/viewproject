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

### 前置要求

- Docker + Docker Compose
- Node.js 20+
- pnpm 或 npm

### 1. 启动后端（Docker）

```bash
# 进入项目目录
cd /path/to/project

# 启动所有���务（MySQL + Redis + Spring Boot）
docker compose up -d

# 查看日志
docker compose logs -f ai-cust
```

后端启动成功后：
- API 地址：`http://localhost:8888`
- MySQL：`localhost:3306`（root/root123）
- Redis：`localhost:6381`

### 2. 启动前端（开发模式）

```bash
# 进入前端目录
cd frontend

# 安装依赖（首次）
npm install

# 启动开发服务器
npm run dev
```

前端启动成功后访问：`http://localhost:5173`

### 3. 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 可进入管理后台 |
| 游客 | (自行注册) | 自行注册 | 仅限游客端 |

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

如需修改配置，编辑 `src/main/resources/application.yml`：

```yaml
# RAG 服务地址
rag:
  base-url: http://1.117.74.151:8081
  username: admin
  password: admin123

# Ollama
ollama:
  base-url: http://localhost:11434
```

## 常见问题

### 1. 前端无法访问后端 API

确保 Docker 后端已启动：
```bash
docker compose ps
```

### 2. 语音功能无法使用

确保本地安装了：
- `edge-tts`：`pip install edge-tts`
- `faster-whisper` 并启动 HTTP 服务（默认 9876 端口）

### 3. 注册失败显示 500 错误

检查数据库是否有重复用户名：
```bash
docker exec cust-mysql mysql -uroot -proot123 ai_cust -e "SELECT username, COUNT(*) FROM users GROUP BY username HAVING COUNT(*) > 1;"
```

## 许可证

本项目仅供学习和演示使用。