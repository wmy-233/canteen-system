package com.canteen.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 统一入口（软依赖封装，组员 C 维护）
 *
 * 设计目标：Redis 从"硬依赖"变"软依赖"——
 *   Redis 可用   -> 防超卖分布式锁 / ZSet 热销榜 / 接口限流等亮点功能全部启用
 *   Redis 不可用 -> 后端照常启动，主流程不受影响，业务走数据库兜底（优雅降级）
 *
 * 业务侧约定（第 2-3 周防超卖/热销榜开发时遵守）：
 *   1. 一律通过本类访问 Redis，先 isAvailable() 判断，false 时走数据库兜底逻辑
 *   2. 需要分布式锁的场景直接调用 withLock()，降级时自动退化为无锁直跑
 *   3. 降级防超卖由业务层数据库条件更新兜底（UPDATE ... SET stock=stock-? WHERE stock>=?），
 *      依然不会超卖，只是并发性能低于 Redis 预扣方案
 */
@Slf4j
@Component
public class RedisSupport {

    /** Redisson 客户端：null 表示 Redis 不可用，处于降级模式 */
    private final RedissonClient client;

    public RedisSupport(RedisProperties redisProperties) {
        RedissonClient created = null;
        try {
            Config config = new Config();
            SingleServerConfig single = config.useSingleServer()
                    .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                    .setDatabase(redisProperties.getDatabase())
                    .setTimeout((int) redisProperties.getTimeout().toMillis())
                    .setConnectTimeout((int) redisProperties.getTimeout().toMillis())
                    .setRetryAttempts(1)        // 降级模式关键：只重试 1 次，避免启动干等太久
                    .setRetryInterval(1000);
            if (StringUtils.hasText(redisProperties.getPassword())) {
                single.setPassword(redisProperties.getPassword());
            }
            created = Redisson.create(config);
            log.info("Redis 连接成功（{}:{} db={}）：防超卖锁/热销榜/限流等亮点功能已启用",
                    redisProperties.getHost(), redisProperties.getPort(), redisProperties.getDatabase());
        } catch (Exception e) {
            log.warn("Redis 连接失败，进入降级模式：登录/浏览/下单等主流程不受影响，"
                    + "防超卖锁/热销榜/限流等亮点功能不可用。原因: {}", e.getMessage());
        }
        this.client = created;
    }

    /** Redis 是否可用；false = 降级模式，业务必须走数据库兜底逻辑 */
    public boolean isAvailable() {
        return client != null;
    }

    /** 原生 Redisson 客户端（降级模式下为 null），供 ZSet 热销榜等高级用法使用 */
    public RedissonClient getClient() {
        return client;
    }

    /**
     * 带分布式锁执行业务（Redisson RLock 可重入锁）
     *
     * @param lockKey     锁键，如 lock:dish:3:stock
     * @param waitSeconds 最多等待秒数，超时视为操作冲突
     * @param action      业务体
     * @return 业务体返回值
     * @throws IllegalStateException 等锁超时（提示用户稍后重试）
     */
    public <T> T withLock(String lockKey, long waitSeconds, Supplier<T> action) {
        if (client == null) {
            // 降级：Redis 不可用时退化为无锁直跑，防超卖由业务层数据库条件更新兜底
            log.warn("[降级] Redis 不可用，未加分布式锁直接执行: key={}", lockKey);
            return action.get();
        }
        RLock lock = client.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitSeconds, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("当前操作人数过多，请稍后重试");
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("获取锁被中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
