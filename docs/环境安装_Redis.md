# Redis 环境安装（Docker 一键版）

> 本组 Redis 统一用 Docker 跑，不再使用 Windows 原生版。
> 口径：无密码（与 `application.yml` 对齐）、端口 6379、容器名 `canteen-redis`。

## 1. 启动（人人必做，1 分钟）

拉取最新代码后，在仓库根目录执行：

```bash
docker compose up -d
docker exec canteen-redis redis-cli ping   # 期望输出 PONG
```

Docker Desktop 建议设置为开机自启，容器配置了 `restart: unless-stopped`，Desktop 开着就会自动拉起。

## 2. 装过原生 Redis 的先停用（迁移口径）

原生 Redis 服务占着 6379，必须先让位，否则容器会因端口冲突起不来。
用**管理员** PowerShell / CMD 执行：

```powershell
net stop Redis
sc config Redis start= disabled
```

然后再 `docker compose up -d`。

## 3. 常见问题

| 现象 | 原因与处理 |
|---|---|
| `ping` 返回 `NOAUTH Authentication required` | 你的 Redis 设了密码（口径是无密码）：停用原生版，改用本仓库 Docker 版 |
| 后端启动报 Redisson 连接失败 / 6379 被占用 | 原生 Redis 还在跑，见第 2 节 |
| Docker Desktop 没开 | **后端仍能正常启动（降级模式）**：登录、浏览、下单等主流程不受影响；热销榜、防超卖分布式锁、限流等亮点功能不可用。开发/演示前记得先把 Docker Desktop 点开 |
| 想重置 Redis 数据 | `docker compose down -v && docker compose up -d` |
| 拉取镜像慢 | 配置 Docker Desktop 镜像加速，或手机热点重试一次 |
