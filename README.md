# 智慧校园食堂点餐系统

《Java 高级开发技术》期末大作业 —— 前后端分离完整项目

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | VUE 3 + Vite + Element Plus + Axios + ECharts + Pinia |
| 后端 | Spring Boot 2.7.18 + MyBatis + Spring Security + JWT |
| 缓存 | Redis + Redisson（分布式锁防超卖 / ZSet 热销榜） |
| 日志 | slf4j + Logback |
| 数据库 | MySQL 8 |

## 目录结构

```
canteen-system/
├── backend/    后端工程（Spring Boot）
├── frontend/   前端工程（VUE3 + Vite）
├── sql/        数据库脚本（schema.sql 建表 + init_data.sql 初始数据）
└── docs/       需求分析 / 系统设计 / 操作手册
```

## 本地启动步骤

### 1. 环境要求
- JDK 8 或 11（⚠️ 不要用 JDK 20+ 跑 Spring Boot 2.7）
- Maven 3.6+
- Node.js 16+
- MySQL 8（本地 root 密码默认 123456，在 application.yml 修改）
- Docker Desktop（Redis 统一用 Docker 跑：`docker compose up -d`；未开 Docker 时后端自动降级运行，主流程不受影响）

### 2. 启动 Redis（Docker 一键）
```bash
docker compose up -d
docker exec canteen-redis redis-cli ping   # 返回 PONG 即成功
```
> 本机装过原生 Redis 的先停用（管理员执行 `net stop Redis`），避免 6379 端口冲突。
> 详见 `docs/环境安装_Redis.md`。

### 3. 初始化数据库
用 Navicat 或命令行执行 `sql/schema.sql`（已含建库建表）。

### 4. 启动后端
```bash
cd backend
mvn spring-boot:run
```
启动成功后：
- 健康检查：http://localhost:8080/api/health
- 接口文档：http://localhost:8080/doc.html

### 5. 启动前端
```bash
cd frontend
npm install
npm run dev
```
访问 http://localhost:5173

## 团队分组与分支约定

| 成员 | 分支 | 职责 |
|---|---|---|
| 组长 A | feature/backend | 后端业务接口 |
| 组员 B | feature/frontend | 前端全部页面 |
| 组员 C | feature/infra | Redis/权限/日志/数据库/文档 |

规矩：禁止直接 push main，一律 Pull Request；main 随时可运行。
