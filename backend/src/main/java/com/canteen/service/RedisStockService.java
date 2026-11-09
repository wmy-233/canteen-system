package com.canteen.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Redis 库存预减/回滚（A5 防超卖第一重，软依赖）
 *
 * 口径：
 *   1. 仅限量菜（daily_stock &gt; 0）参与预减，key 全局唯一 dish:stock:{id}
 *   2. key 不存在时用数据库当日余量初始化（trySet 仅在缺失时生效，天然幂等）
 *   3. 预减后 &lt; 0 视为已抢完并立即回滚本次预减
 *   4. 到期时间设到次日凌晨 0 点，跨天自动重置，与“每日限量”口径一致
 *   5. Redis 不可用时调用方必须走数据库条件更新兜底（依旧零超卖）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStockService {

    public static final String KEY_PREFIX = "dish:stock:";

    /** Redis 统一入口（软依赖）：不可用时所有方法直接返回走降级 */
    private final com.canteen.service.RedisSupport redisSupport;
    private final com.canteen.mapper.DishMapper dishMapper;

    /**
     * 预减库存；返回 false = 已抢完
     */
    public boolean tryDecrease(Long dishId, Integer quantity) {
        if (!redisSupport.isAvailable()) {
            return true;    // 降级模式跳过预减，由数据库条件更新兜底
        }
        try {
            RAtomicLong stock = atomicLong(KEY_PREFIX + dishId);
            // key 缺失时以数据库当日余量初始化（微弱的 check-then-set 竞态仅影响预减灵敏度，不产生超卖——
            // 分了三层防线的 DB 条件更新始终是最终闸门）
            if (!stock.isExists()) {
                Integer dbStock = dishMapper.selectDailyStock(dishId);
                stock.set(dbStock == null ? 0 : dbStock.longValue());
                expireAtNextMidnight(stock);
            }
            long left = stock.decrementAndGet();
            if (left < 0) {
                stock.incrementAndGet();    // 本单未抢到，恢复预减
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis 预减库存异常，退化为数据库条件更新兜底: dishId={}, 原因: {}", dishId, e.getMessage());
            return true;
        }
    }

    /** 下单失败（除“抢完”外）事务回滚时的 Redis 余量补偿 */
    public void restore(Long dishId, Integer quantity) {
        if (!redisSupport.isAvailable()) {
            return;
        }
        try {
            atomicLong(KEY_PREFIX + dishId).incrementAndGet();
        } catch (Exception e) {
            log.warn("Redis 库存补偿失败: dishId={}, 原因: {}", dishId, e.getMessage());
        }
    }

    /** 商家修改每日限量后清 key：下一次下单会重新从数据库初始化 */
    public void evict(Long dishId) {
        if (!redisSupport.isAvailable()) {
            return;
        }
        try {
            atomicLong(KEY_PREFIX + dishId).delete();
        } catch (Exception ignored) {
        }
    }

    private RAtomicLong atomicLong(String key) {
        RedissonClient client = redisSupport.getClient();
        return client.getAtomicLong(key);
    }

    private void expireAtNextMidnight(RAtomicLong stock) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIN);
        stock.expire(Duration.between(now, nextMidnight));
    }
}
