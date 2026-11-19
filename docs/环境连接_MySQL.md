# 数据库连接指南（MySQL @ Docker）

> 数据库已经 Docker 化：**不需要安装本地 MySQL**，一条命令起库，然后用工具连上去看。
> Redis 环境见《环境安装_Redis.md》。本文写给第一次连库的同学。

## 1. 前提：先把库跑起来（10 秒）

在仓库根目录执行（Docker Desktop 需已开启）：

```powershell
docker compose up -d
```

- 首次运行会自动建库 `canteen` + 执行基础脚本（`schema.sql` 16 张表 + `init_data.sql` 演示数据）
- 连接参数**统一口径**：地址 `127.0.0.1:3306`，账号 `root`，密码 `123456`（与 application.yml 一致，不用改任何配置）
- ⚠️ 基础脚本**不含**配送/投诉/消息/日志四张表。首次建库后需按顺序执行三个补丁（详见第 5 节），否则校园配送、投诉、消息中心、操作日志功能会因缺表报错

## 2. 方式一：Navicat 连接（推荐答辩演示用）

1. 连接 → MySQL：
   | 栏目 | 值 |
   |---|---|
   | 连接名 | `canteen-dev`（随意） |
   | 主机 | `127.0.0.1` |
   | 端口 | `3306` |
   | 用户名 | `root` |
   | 密码 | `123456` |
2. 左下角"测试连接"提示成功后再保存（首次可能提示下载驱动，照提示点下一步）
3. 双击连接 → 双击 `canteen` 库 → 表 → 双击任意表看网格数据
4. 若中文显示异常：连接属性 → 高级 → 编码改为 **UTF-8**（库内数据已保证 utf8mb4 正确存储）

**重点看哪几张表**（答辩/联调都用得上）：

| 表 | 看什么 |
|---|---|
| `user` | 6 个演示账号（admin/merchant/merchant2/student/rider1/rider2，后两个由补丁插入），password 列是 BCrypt 密文 |
| `role` / `user_role` | 四角色（含补丁插入的 RIDER）+ 用户-角色绑定（RBAC 第一环） |
| `menu` / `role_menu` | 权限点与角色-权限映射（RBAC 第二环） |
| `dish` | 20 道演示菜；`is_limited=1` 且 `daily_stock>0` = 限量菜（防超卖演示），`sale_count` 有梯度（热销榜演示） |
| `stall` | 3 个档口，1 条 `status=2` 待审核（管理端审核流程演示用） |
| `coupon` / `user_coupon` | 优惠券 + student 已领 1 张未使用 |
| `order_info` | 看 `delivery_type`/`rider_id`/`delivery_status`/`delivery_location`/`delivery_fee`/`pickup_code`/`batch_no`（补丁加列） |
| `rider`（补丁） | 2 名校园配送员：`rider1` 在岗、`rider2` 停用 |
| `complaint`（补丁） | 投诉工单，`status` 0待处理/1已回应/2已裁定，`escalated` 超时升级 |
| `sys_message`（补丁） | 站内消息，type 0系统/1订单/2入驻/3投诉/4配送 |
| `sys_operation_log`（补丁） | AOP 操作日志（操作人/模块/参数/耗时/成败） |

## 3. 方式二：IDEA 自带 Database 面板（日常开发最方便）

1. 右侧边栏 **Database** → `+` → Data Source → **MySQL**
2. 参数同上（127.0.0.1 / 3306 / root / 123456），首次会提示"Download Driver"，点下载
3. 点 **Test Connection** → 绿勾后 Apply
4. 之后可直接浏览表、双击编辑、Ctrl+Enter 执行选中 SQL

## 4. 方式三：命令行（零安装，验证用）

```powershell
# 看全部表
docker exec canteen-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 -e "SHOW TABLES FROM canteen"

# 随便查点什么
docker exec canteen-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 -e "SELECT id, username, nickname, balance FROM canteen.\`user\`"
```

## 5. 首次建库必做：应用三个增量补丁

`docker-compose.yml` 只挂载了 `01-schema.sql`、`02-init_data.sql`，补丁脚本必须手动执行一次（**顺序不能乱**）：

```powershell
docker exec -i canteen-mysql mysql -uroot -p123456 canteen < sql/patch_message_complaint.sql
docker exec -i canteen-mysql mysql -uroot -p123456 canteen < sql/patch_rider.sql
docker exec -i canteen-mysql mysql -uroot -p123456 canteen < sql/patch_enhancements.sql
```

| 补丁 | 新增内容 | 可重复执行 |
|---|---|---|
| `patch_message_complaint.sql` | `sys_message`、`complaint` 两表 | ✅（`CREATE TABLE IF NOT EXISTS`） |
| `patch_rider.sql` | `rider` 表；`order_info` 四列（`delivery_type`/`rider_id`/`delivery_status`/`delivery_location`）；RIDER 角色与权限；2 名配送员 + 2 笔待分配配送单 | ❌ 一次性，重复执行报 `Duplicate column` |
| `patch_enhancements.sql` | `sys_operation_log` 表；`complaint.escalated`；`user_coupon_id`/`batch_no`/`limited`/`is_limited`/`delivery_fee` 等兼容列 | ✅（幂等判断） |

完成后共 **20 张表**（16 基础 + 4 补丁）。`docker compose down -v` 重置数据卷后需重新执行三者。

菜品图片可选：执行 `scripts/dish_images.sql` 补 19 张本地图片路径。

## 6. 常见报错对照

| 报错 | 原因 → 解法 |
|---|---|
| `Can't connect... 10061` 拒绝连接 | 容器没起 → `docker compose up -d` |
| `1045 Access denied` | 密码不对 → 是 `123456`，不是你旧本地库的密码 |
| 中文乱码 | 工具编码改 UTF-8（Navicat 见上）；库内数据无问题 |
| 连接时整个变慢/超时 | Docker Desktop 刚启动还在初始化，等半分钟再连 |
| 端口 3306 被占用起不来容器 | 本机装过别的 MySQL（如 phpStudy）在跑 → `net stop mysqlzt`，或停用该服务 |

## 7. ⚠️ 约定与红线

1. **不要启动本机旧 MySQL 服务**（phpStudy 的 `mysqlzt` 等）：会抢占 3306，先到的赢，导致 Docker MySQL 失联，排查极费时
2. **不要在 Navicat 里手动改业务表数据**后直接提交代码：演示数据一律以 `sql/init_data.sql` 为准——要改数据请改脚本再重置（`docker compose down -v && docker compose up -d`）
3. **表结构改动必须同步改 `sql/schema.sql` 或对应补丁脚本**，群里说一声；不要在库里手动加列
4. 重置会清空所有业务表数据（包括你注册接口新建的用户），重置后请重新注册并重新执行三个补丁
5. 配送、投诉、消息、日志相关表由补丁创建，不是 `schema.sql` 遗漏；遇到"Table doesn't exist"先补执行第 5 节脚本
