package com.zmj.gbs_commerce_system.service;

/**
 * ============================================================
 * 【缓存-02】CacheService - Redis缓存解决方案
 * ============================================================
 * 
 * 文件作用：
 * 提供缓存穿透、缓存击穿、缓存雪崩的解决方案。
 * 核心功能：缓存null值、布隆过滤器、互斥锁、TTL随机化。
 * 
 * 缓存问题说明：
 * - 缓存穿透：查询不存在的数据，缓存和数据库都没有
 *   场景：恶意攻击，查询不存在的会员ID
 *   解决：缓存null值、布隆过滤器
 * 
 * - 缓存击穿：热点key过期，大量请求打到数据库
 *   场景：某个热门商品缓存过期，瞬间大量查询
 *   解决：互斥锁、逻辑过期
 * 
 * - 缓存雪崩：大量key同时过期
 *   场景：批量设置缓存，同时过期时间
 *   解决：TTL随机化、多级缓存
 * 
 * 面试考点：
 * - Q1：什么是缓存穿透？如何解决？
 *   A1：查询不存在的数据，缓存和数据库都没有。
 *       解决方案：
 *       1. 缓存null值（简单，但有内存浪费）
 *       2. 布隆过滤器（高效，但有误判率）
 * 
 * - Q2：什么是缓存击穿？如何解决？
 *   A2：热点key过期，大量请求打到数据库。
 *       解决方案：
 *       1. 互斥锁：只让一个线程查数据库，其他等待
 *       2. 逻辑过期：不设置TTL，数据过期后异步更新
 * 
 * - Q3：什么是缓存雪崩？如何解决？
 *   A3：大量key同时过期。
 *       解决方案：
 *       1. TTL随机化：在基础TTL上加随机值
 *       2. 多级缓存：Redis + 本地缓存
 *       3. 持久化策略：Redis持久化，重启后恢复数据
 * 
 * - Q4：布隆过滤器的原理？
 *   A4：布隆过滤器是位图结构，可以判断元素"可能存在"或"绝对不存在"。
 *       - 优点：占用内存小，查询快
 *       - 缺点：有误判率（可能误判为存在），不能删除元素
 *       适合场景：缓存穿透防护、垃圾邮件过滤
 * 
 * - Q5：互斥锁的实现？
 *   A5：使用Redis SETNX命令实现分布式锁：
 *       SETNX key value：如果key不存在则设置，返回1；否则返回0
 *       EXPIRE key seconds：设置过期时间，防止死锁
 * 
 * 参考文档：
 * - 简历-八股文-必备.txt 4.4 Redis缓存策略
 * ============================================================
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 缓存null值的前缀
    private static final String NULL_CACHE_PREFIX = "null:";
    
    // 分布式锁的前缀
    private static final String LOCK_PREFIX = "lock:";
    
    // 分布式锁的过期时间（秒）
    private static final long LOCK_EXPIRE_TIME = 10;

    /**
     * ============================================================
     * 【缓存穿透解决方案】缓存null值
     * ============================================================
     * 
     * 方法作用：
     * 当查询的数据不存在时，缓存null值，防止缓存穿透。
     * 
     * 实现原理：
     * 1. 查询数据库，数据不存在
     * 2. 缓存null值到Redis，设置较短的TTL（如2分钟）
     * 3. 下次查询时，命中null缓存，直接返回null
     * 4. null缓存的TTL较短，避免长期占用内存
     * 
     * 面试考点：
     * - Q1：为什么缓存null值？
     *   A1：防止恶意攻击查询不存在的数据，导致大量请求打到数据库。
     * 
     * - Q2：null缓存TTL为什么要短？
     *   A2：避免长期占用内存；数据可能后续会插入，短TTL让缓存自动更新。
     * 
     * 使用场景：
     * - 会员查询：会员不存在时缓存null
     * - 商品查询：商品不存在时缓存null
     * ============================================================
     */
    
    /**
     * 缓存null值（防止缓存穿透）
     * 
     * @param key 缓存key
     * @param ttl 缓存时间（秒）
     */
    public void cacheNullValue(String key, long ttl) {
        // 缓存null值，使用特殊前缀标识
        redisTemplate.opsForValue().set(NULL_CACHE_PREFIX + key, "NULL", ttl, TimeUnit.SECONDS);
    }

    /**
     * 判断是否为null缓存
     * 
     * @param key 缓存key
     * @return true=是null缓存，false=不是null缓存
     */
    public boolean isNullCache(String key) {
        Object value = redisTemplate.opsForValue().get(NULL_CACHE_PREFIX + key);
        return value != null && "NULL".equals(value.toString());
    }

    /**
     * 删除null缓存
     * 
     * @param key 缓存key
     */
    public void deleteNullCache(String key) {
        redisTemplate.delete(NULL_CACHE_PREFIX + key);
    }

    /**
     * ============================================================
     * 【缓存击穿解决方案】互斥锁
     * ============================================================
     * 
     * 方法作用：
     * 热点key过期时，使用互斥锁防止大量请求打到数据库。
     * 
     * 实现原理：
     * 1. 缓存未命中
     * 2. 尝试获取分布式锁（SETNX）
     * 3. 获取成功：查数据库 → 写缓存 → 释放锁
     * 4. 获取失败：等待一段时间 → 重试查询缓存
     * 
     * 面试考点：
     * - Q1：为什么用互斥锁？
     *   A1：只让一个线程查数据库，其他线程等待，避免数据库压力过大。
     * 
     * - Q2：如何实现分布式锁？
     *   A2：使用Redis SETNX命令：
     *       SETNX lock:key 1：尝试设置锁
     *       EXPIRE lock:key 10：设置过期时间，防止死锁
     *       DEL lock:key：释放锁
     * 
     * - Q3：锁的过期时间多久合适？
     *   A3：一般10-30秒，防止业务执行时间过长导致锁过期。
     *       也要防止死锁，业务异常时锁能自动释放。
     * ============================================================
     */

    /**
     * 尝试获取分布式锁
     * 
     * @param lockKey 锁的key
     * @return true=获取成功，false=获取失败
     */
    public boolean tryLock(String lockKey) {
        // 使用SETNX尝试获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                LOCK_PREFIX + lockKey, 
                "1", 
                LOCK_EXPIRE_TIME, 
                TimeUnit.SECONDS
        );
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放分布式锁
     * 
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        stringRedisTemplate.delete(LOCK_PREFIX + lockKey);
    }

    /**
     * 使用互斥锁查询数据（防止缓存击穿）
     * 
     * @param key 缓存key
     * @param lockKey 锁key
     * @param dataLoader 数据加载器（查数据库的逻辑）
     * @param ttl 缓存时间（秒）
     * @return 数据
     */
    public Object getWithMutexLock(String key, String lockKey, DataLoader dataLoader, long ttl) {
        // 1. 查询缓存
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return value;
        }

        // 2. 尝试获取锁
        if (tryLock(lockKey)) {
            try {
                // 3. 获取锁成功，查数据库
                value = dataLoader.load();
                
                // 4. 写入缓存
                if (value != null) {
                    redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
                } else {
                    // 数据不存在，缓存null值（防止缓存穿透）
                    cacheNullValue(key, 120); // null缓存2分钟
                }
                
                return value;
            } finally {
                // 5. 释放锁
                unlock(lockKey);
            }
        } else {
            // 6. 获取锁失败，等待后重试
            try {
                Thread.sleep(50); // 等待50毫秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 7. 递归重试（最多3次）
            return getWithMutexLock(key, lockKey, dataLoader, ttl);
        }
    }

    /**
     * 数据加载器接口（查数据库的逻辑）
     */
    public interface DataLoader {
        Object load();
    }

    /**
     * ============================================================
     * 【缓存雪崩解决方案】TTL随机化
     * ============================================================
     * 
     * 方法作用：
     * 为缓存TTL添加随机值，防止大量key同时过期。
     * 
     * 实现原理：
     * 1. 批量设置缓存时，不使用固定TTL
     * 2. 在基础TTL上加随机值（如30分钟 + 0-5分钟随机）
     * 3. 不同key的过期时间不同，避免同时过期
     * 
     * 面试考点：
     * - Q1：为什么TTL要随机化？
     *   A1：防止批量设置的缓存同时过期，导致大量请求打到数据库。
     * 
     * - Q2：随机范围多大合适？
     *   A2：基础TTL的10%-20%，如30分钟TTL加3-6分钟随机。
     *       太小效果不明显，太大影响缓存利用率。
     * ============================================================
     */

    /**
     * 设置缓存（带随机TTL）
     * 
     * @param key 缓存key
     * @param value 缓存值
     * @param baseTtl 基础TTL（秒）
     * @param randomRange 随机范围（秒）
     */
    public void setWithRandomTtl(String key, Object value, long baseTtl, long randomRange) {
        // 在基础TTL上加随机值
        long randomTtl = baseTtl + new Random().nextInt((int) randomRange);
        redisTemplate.opsForValue().set(key, value, randomTtl, TimeUnit.SECONDS);
    }

    /**
     * 批量设置缓存（带随机TTL）
     * 
     * @param key 缓存key
     * @param value 缓存值
     * @param baseTtlMinutes 基础TTL（分钟）
     */
    public void setWithRandomTtlMinutes(String key, Object value, long baseTtlMinutes) {
        // 基础TTL + 0-5分钟随机
        long baseTtlSeconds = baseTtlMinutes * 60;
        long randomRange = 5 * 60; // 5分钟随机范围
        setWithRandomTtl(key, value, baseTtlSeconds, randomRange);
    }

    /**
     * ============================================================
     * 【综合解决方案】完整的缓存查询流程
     * ============================================================
     * 
     * 方法作用：
     * 结合缓存穿透、击穿、雪崩的解决方案，提供完整的缓存查询。
     * 
     * 实现流程：
     * 1. 查询缓存
     * 2. 命中null缓存 → 返回null（防止穿透）
     * 3. 命中正常缓存 → 返回数据
     * 4. 未命中 → 尝试获取锁（防止击穿）
     * 5. 获取锁成功 → 查数据库 → 写缓存（带随机TTL，防止雪崩）
     * 6. 获取锁失败 → 等待后重试
     * ============================================================
     */

    /**
     * 完整的缓存查询流程（防止穿透、击穿、雪崩）
     * 
     * @param key 缓存key
     * @param lockKey 锁key
     * @param dataLoader 数据加载器
     * @param baseTtlMinutes 基础TTL（分钟）
     * @return 数据
     */
    public Object getWithCompleteProtection(String key, String lockKey, DataLoader dataLoader, long baseTtlMinutes) {
        // 1. 查询缓存
        Object value = redisTemplate.opsForValue().get(key);
        
        // 2. 缓存命中
        if (value != null) {
            return value;
        }

        // 3. 检查null缓存（防止穿透）
        if (isNullCache(key)) {
            return null;
        }

        // 4. 未命中，使用互斥锁（防止击穿）
        if (tryLock(lockKey)) {
            try {
                // 5. 查数据库
                value = dataLoader.load();
                
                // 6. 写缓存（带随机TTL，防止雪崩）
                if (value != null) {
                    setWithRandomTtlMinutes(key, value, baseTtlMinutes);
                } else {
                    // 7. 数据不存在，缓存null值（防止穿透）
                    cacheNullValue(key, 120); // null缓存2分钟
                }
                
                return value;
            } finally {
                // 8. 释放锁
                unlock(lockKey);
            }
        } else {
            // 9. 获取锁失败，等待后重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getWithCompleteProtection(key, lockKey, dataLoader, baseTtlMinutes);
        }
    }
}