# 智慧校园食堂点餐系统

《Java 高级开发技术》期末大作业 —— 前后端分离完整项目（四角色：学生 / 商家 / 校园配送员 / 管理员）

> 文档状态：已按当前 `main` 分支（提交 `f53f9d1`，2026-11）代码核对更新。
> 交付文档：`docs/软件需求规格说明书.md`、`docs/系统设计文档.md`、`docs/系统操作手册.md`。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite 5 + Element Plus + Axios + ECharts + Pinia + qrcode |
| 后端 | Spring Boot 2.7.18（Java 11）+ MyBatis + PageHelper + Spring Security + JWT |
| 缓存 / 分布式锁 | Redis 6 + Redisson 3.23.5（预减库存、ZSet 热销榜、抢单锁） |
| 日志 | slf4j + Logback + AOP（`@OpLog` 落库留痕） |
| 数据库 | MySQL 8 |
| AI | 前端 DeepSeek `deepseek-chat` + Function Calling（需自配 `VITE_DEEPSEEK_API_KEY`，无 key 时走本地关键词兜底） |

## 目录结构

```
canteen-system/
├── backend/       Spring Boot 后端（controller / service / mapper / entity / dto / vo / security / aspect / task）
├── frontend/      Vue 3 前端（layouts / views 四角色 / components / api / stores / router）
├── sql/           schema.sql（16 表）+ init_data.sql（演示数据）+ 3 个增量补丁脚本
├── docs/          需求规格说明书 / 系统设计 / 操作手册 / 环境安装 / 数据库连接
├── scripts/       菜品图片抓取脚本与生成的图片 UPDATE 语句
├── docker-compose.yml   Redis + MySQL 一键环境（含建库与初始数据自动初始化）
└── README.md
```

## 已有功能

- **学生端**：食堂/档口/分类浏览、限量菜余量与热销榜、购物车（跨档口拆单）、下单（自取/校内配送）、取餐码与二维码、钱包充值与流水、优惠券、评价、投诉、站内消息、AI 助手「胆子肥嘟嘟」。
- **校园配送（勤工助学）**：学生下单时填写**校内送达地点**（宿舍楼/图书馆/教学楼，可输入具体楼栋），支付 2 元跑腿费；配送员在**跑腿大厅**自主抢单（Redisson 锁 + 条件更新保证只一人抢到），送达后跑腿费结算到其校园钱包（`wallet_record.type=4`），优惠券不抵扣跑腿费；管理员可做兜底指派。自取单走商家取餐码核销，配送单走配送状态机，两条线互不干扰。
- **订单一致性**：跨档口下单由后端一个事务原子拆单，任一失败全部回滚；取消/投诉成立退款会**全量回滚**余额、优惠券（精确到领券记录）、库存、销量与 Redis 余量/榜单。
- **投诉闭环**：学生投诉 → 商家回应 → 管理员裁定（可勾选退款）；商家 24 小时未回应自动超时升级（定时任务 + 手动触发双通道）。
- **核销与播报**：学生端二维码（5 分钟有效）→ 商家扫码或手工输入码核销；商家出餐看板可开启新订单语音播报。
- **审计与消息**：AOP 操作日志落库，管理端可按操作人/模块/结果查询最近 300 条并清空；四角色站内消息中心（订单/入驻/投诉/配送通知）。
- **AI 助手**：DeepSeek + Function Calling，可实时查余额、订单、购物车、热销榜并直接加购；无 API Key 时自动降级为本地关键词问答。
- **优雅降级**：Redis 不可用时后端仍正常启动，登录/浏览/下单主流程不受影响，仅分布式锁、预减库存、热销榜等增强能力退化为数据库实现。

> 接口限流（早期规划亮点 H3）当前**未实现**，已从已交付功能中移除，仅保留扩展点。

## 本地启动步骤

### 1. 环境要求

- JDK 11（⚠️ 不要用 JDK 20+ 跑 Spring Boot 2.7；本机高版本 JDK 需配置 Lombok 注解处理器）
- Maven 3.6+
- Node.js 16+
- Docker Desktop（Redis 与 MySQL 统一用 Docker：`docker compose up -d`）
- 本机若装过原生 Redis / MySQL，先停用，避免 6379 / 3306 端口冲突（`net stop Redis`、`net stop mysqlzt`）

### 2. 一键起 Redis + MySQL

```bash
docker compose up -d
docker exec canteen-redis redis-cli ping                                  # PONG
docker exec canteen-mysql mysql -uroot -p123456 -e "SELECT 1"             # 1
```

首次启动会自动建库 `canteen` 并执行 `sql/schema.sql`、`sql/init_data.sql`（16 张基础表 + 演示数据）。
详见 `docs/环境安装_Redis.md` 与 `docs/环境连接_MySQL.md`。

### 3. 应用增量补丁（必须执行一次，否则配送/投诉/日志功能无表可用）

在新库上按顺序各执行一次（**顺序不能乱**，`patch_rider.sql` 不可重复执行）：

```bash
docker exec -i canteen-mysql mysql -uroot -p123456 canteen < sql/patch_message_complaint.sql
docker exec -i canteen-mysql mysql -uroot -p123456 canteen < sql/patch_rider.sql
docker exec -i canteen-mysql mysql -uroot -p123456 canteen < sql/patch_enhancements.sql
```

补丁完成后共 **20 张表**：新增 `rider`（校园配送员）、`sys_message`（站内消息）、`complaint`（投诉工单）、`sys_operation_log`（操作日志），并给 `order_info` 增加配送四列（`delivery_type` / `rider_id` / `delivery_status` / `delivery_location`）。
`patch_enhancements.sql` 可重复执行；`patch_message_complaint.sql` 可重复执行；`patch_rider.sql` 一次性。

菜品图片（可选）：执行 `scripts/dish_images.sql` 给 19 道菜补本地图片路径。

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

- 健康检查：http://localhost:8080/api/health
- 接口文档：http://localhost:8080/doc.html

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 。需要真实 AI 问答时在项目根目录建 `frontend/.env.local` 写入 `VITE_DEEPSEEK_API_KEY=你的key`（该文件已被 git 忽略）。

## 演示账号（密码统一 `123456`）

| 角色 | 账号 | 说明 |
|---|---|---|
| 学生 | `student` | 余额 50.00，已领 1 张满 10 减 2 券 |
| 商家 | `merchant` / `merchant2` | 分别管理「川湘家常菜」「兰州拉面」；另有 1 个待审核档口可演示审核流 |
| 校园配送员 | `rider1`（在岗）/ `rider2`（停用） | 跑腿大厅抢单、送达结算 |
| 管理员 | `admin` | 用户/档口/配送/投诉/优惠券/日志/统计 |

注册接口默认创建学生；商家走「入驻申请制」生成待审核档口；管理员账号不开放注册；配送员账号由管理员在「配送调度」页创建。

## 权限与接口约定

| 路径前缀 | 允许角色 |
|---|---|
| `/api/user/login`、`/api/user/register`、`/api/user/register-merchant`、`/api/canteen/list`、`/api/health`、`/doc.html` | 放行 |
| `/api/admin/**` | ADMIN |
| `/api/merchant/**` | MERCHANT |
| `/api/rider/**` | RIDER |
| 其余 `/api/**` | 已登录即可（学生端、公共查询） |

统一返回体 `{code, message, data}`；401 未登录 / 403 无权限由后端 JSON 返回，前端拦截器统一跳登录。JWT 中角色仅作兜底，鉴权时**实时查库**，禁用用户与角色变更即时生效。

## 文档索引

| 文档 | 内容 |
|---|---|
| `docs/软件需求规格说明书.md` | 需求编号、四角色权限矩阵、用例、验收标准、需求—实现追踪 |
| `docs/系统设计文档.md` | 架构、库表设计、核心流程（防超卖/原子下单/配送担保/抢单锁/投诉升级/日志/AI） |
| `docs/系统操作手册.md` | 四角色演示步骤与常见问题 |
| `docs/环境安装_Redis.md` / `docs/环境连接_MySQL.md` | 环境搭建与连库指南 |
| `docs/阶段一_项目搭建总结与计划.md` / `docs/任务分配_收尾冲刺文档.md` | 历史过程记录（已归档，仅作过程参考） |

## 团队约定（归档口径）

开发期规矩：禁止直接 push `main`，一律 feature 分支 + Pull Request；commit 前缀 `feat:` / `fix:` / `docs:` / `sql:`；表结构变更必须同步 `sql/schema.sql` 并在补丁脚本留痕。当前功能已全部合入 `main`。
