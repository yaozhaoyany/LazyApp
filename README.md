# 🦥 LazyApp - 做了么

AI 驱动的智能待办事项管理应用。把大化小，让 AI 当你的秘书。

## 核心功能

- **自然语言输入** — 用模糊文字描述任务（如"今年想学日语"）
- **AI 任务拆解** — 自动将大目标拆分为可执行的子任务
- **AI 追问澄清** — 信息不足时 AI 主动提问细化需求
- **AI 每日排程** — 自动生成优先列表和日程表
- **🐟 今天摸鱼** — 一键跳过今天，AI 自动重排计划

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 + Spring Boot 3 |
| 前端 | React 18 + TailwindCSS |
| 数据库 | PostgreSQL |
| AI | Google Gemini 2.0 Flash（可切换其他模型） |

## 快速开始（Docker 部署）

### 前置条件

- Docker + Docker Compose
- Google Gemini API Key（[Google AI Studio](https://aistudio.google.com/) 免费获取）

### 1. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填入你的 API Key 和数据库密码
```

### 2. 一键启动

```bash
docker compose up -d --build
```

启动后访问 http://你的服务器IP 即可使用。

### 3. 查看日志

```bash
docker compose logs -f backend   # 后端日志
docker compose logs -f frontend  # 前端日志
```

### 4. 停止服务

```bash
docker compose down
```

### 本地开发（可选）

如需本地开发，需要 Java 17+、Node.js 18+、PostgreSQL 15+：

```bash
# 后端
cd backend && mvn spring-boot:run

# 前端
cd frontend && npm install && npm run dev
```

## API 文档

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/tasks | 创建任务 |
| GET | /api/tasks | 获取任务列表 |
| GET | /api/tasks/:id | 获取任务详情 |
| PUT | /api/tasks/:id | 更新任务 |
| PATCH | /api/tasks/:id/status | 更新任务状态 |
| DELETE | /api/tasks/:id | 删除任务 |
| POST | /api/ai/tasks/:id/decompose | AI 拆解任务 |
| POST | /api/ai/tasks/:id/clarify | AI 追问澄清 |
| POST | /api/ai/tasks/:id/respond | 回复 AI 追问 |
| POST | /api/ai/plan/daily | 生成每日计划 |
| POST | /api/ai/plan/skip-day | 今天摸鱼 🐟 |
| POST | /api/ai/subtasks/:id/clarify | AI 子任务执行指导 |
| PATCH | /api/ai/subtasks/:id/status | 更新子任务状态 |
| PATCH | /api/ai/tasks/:id/deadline | 更新任务截止日期 |

## 项目结构

```
LazyApp/
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/com/lazyapp/
│   │   ├── controller/       # REST API 控制器
│   │   ├── service/          # 业务逻辑 + AI 服务
│   │   ├── model/            # JPA 实体类
│   │   ├── repository/       # 数据访问层
│   │   ├── dto/              # 数据传输对象
│   │   └── config/           # 配置类
│   └── pom.xml
├── frontend/                 # React 前端
│   ├── src/
│   │   ├── pages/            # 页面组件
│   │   ├── services/         # API 调用
│   │   └── main.jsx          # 入口
│   └── package.json
├── docker-compose.yml        # Docker 编排
├── .env.example              # 环境变量模板
└── README.md
```
