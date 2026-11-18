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
└── docs/       软件需求规格说明书 / 系统设计 / 操作手册
```

## 功能说明

- **三种就餐方式**：到店自取（凭取餐码）、堂食、配送到寝室。
- **校园配送（勤工助学）**：学生选择校内配送时填写送达地点（宿舍楼 / 图书馆 / 教学楼等，可输入具体楼栋）并支付 2 元跑腿费；固定配送员与勤工助学同学均可在「跑腿大厅」按送达地点接单（Redisson 分布式锁 + 数据库条件更新保证并发安全）。跑腿费采用**担保交易**：下单时冻结在平台，配送员确认送达后才结算到其校园钱包（`wallet_record.type=4`），优惠券不抵扣跑腿费。
- **订单一致性**：跨档口下单由后端一个事务原子拆单，任一失败全部回滚；取消/投诉退款会回滚余额、优惠券、库存、销量与 Redis 余量。
- **投诉闭环**：学生投诉 → 商家回应 → 管理员裁定，商家 24 小时未回应自动超时升级。
- **AI 助手**：DeepSeek + Function Calling，可实时查余额、订单、购物车并加购。

> 在已有数据库上升级时，需执行 `sql/patch_enhancements.sql`（幂等补丁：精确退券、跑腿费、超时升级、操作日志表等）。

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
> 连接与查看方法见 `docs/环境连接_MySQL.md`（数据库已 Docker 化并自动初始化，通常无需手动执行）。

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
