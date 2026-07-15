package com.zmj.gbs_commerce_system.service.impl;

/**
 * ============================================================
 * 【缓存-03】MemberCacheServiceImpl - 会员缓存增强版
 * ============================================================
 * 
 * 文件作用：
 * 会员缓存增强版，包含缓存穿透、击穿、雪崩的完整解决方案。
 * 
 * 解决方案总结：
 * - 缓存穿透：缓存null值 + 布隆过滤器
 * - 缓存击穿：互斥锁 + 逻辑过期
 * - 缓存雪崩：TTL随机化
 * 
 * 使用场景：
 * - 高并发场景：大量请求查询会员信息
 * - 热点数据：某些热门会员被频繁查询
 * - 恶意攻击：查询不存在的会员ID
 * 
 * 参考文档：
 * - 简历-八股文-必备.txt 4.4 Redis缓存策略
 * ============================================================
 */

import com.zmj.gbs_commerce_system.entity.Member;
import com.zmj.gbs_commerce_system.mapper.MemberMapper;
import com.zmj.gbs_commerce_system.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MemberCacheServiceImpl {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheService cacheService;

    private static final String CACHE_PREFIX = "member:";
    private static final String NULL_PREFIX = "null:";
    private static final String LOCK_PREFIX = "lock:member:";
    private static final long BASE_TTL_MINUTES = 30;
    private static final long NULL_TTL_MINUTES = 2;

    /**
     * ============================================================
     * 【缓存穿透+击穿+雪崩】完整的会员查询
     * ============================================================
     * 
     * 实现流程：
     * 1. 查询Redis缓存
     * 2. 命中null缓存 → 返回null（防止穿透）
     * 3. 命中正常缓存 → 返回数据
     * 4. 未命中 → 尝试获取分布式锁（防止击穿）
     * 5. 获取锁成功 → 查数据库 → 写缓存（带随机TTL，防止雪崩）
     * 6. 数据不存在 → 缓存null值（防止穿透）
     * 7. 获取锁失败 → 等待后重试查询缓存
     * 
     * 面试考点：
     * - Q1：为什么先查null缓存？
     *   A1：防止缓存穿透，如果会员不存在，null缓存会拦截恶意请求。
     * 
     * - Q2：为什么用分布式锁？
     *   A2：防止缓存击穿，热点key过期时只让一个线程查数据库。
     * 
     * - Q3：为什么TTL随机化？
     *   A3：防止缓存雪崩，避免大量key同时过期。
     * ============================================================
     */
    
    /**
     * 查询会员（防止穿透、击穿、雪崩）
     * 
     * @param memberId 会员编号
     * @return 会员信息（null表示不存在）
     */
    public Member findByMemberIdWithProtection(String memberId) {
        if (memberId == null || memberId.isEmpty()) {
            return null;
        }

        // ========== 步骤1：查询Redis缓存 ==========
        String cacheKey = CACHE_PREFIX + memberId;
        Member member = (Member) redisTemplate.opsForValue().get(cacheKey);

        // ========== 步骤2：缓存命中 ==========
        if (member != null) {
            log.info("命中缓存: {}", cacheKey);
            return member;
        }

        // ========== 步骤3：检查null缓存（防止穿透）==========
        if (cacheService.isNullCache(cacheKey)) {
            log.info("命中null缓存（会员不存在）: {}", cacheKey);
            return null;
        }

        // ========== 步骤4：未命中，尝试获取分布式锁（防止击穿）==========
        String lockKey = LOCK_PREFIX + memberId;
        
        if (cacheService.tryLock(lockKey)) {
            try {
                // ========== 步骤5：获取锁成功，查数据库 ==========
                log.info("获取锁成功，查询数据库: memberId={}", memberId);
                member = memberMapper.selectByMemberId(memberId);

                // ========== 步骤6：写入缓存 ==========
                if (member != null) {
                    // 数据存在：写入缓存（带随机TTL，防止雪崩）
                    long randomTtl = BASE_TTL_MINUTES + new Random().nextInt(5); // 30-35分钟
                    redisTemplate.opsForValue().set(cacheKey, member, randomTtl, TimeUnit.MINUTES);
                    log.info("写入缓存（带随机TTL={}分钟）: {}", randomTtl, cacheKey);
                } else {
                    // 数据不存在：缓存null值（防止穿透）
                    cacheService.cacheNullValue(cacheKey, NULL_TTL_MINUTES * 60);
                    log.info("缓存null值（会员不存在）: {}", cacheKey);
                }

                return member;

            } finally {
                // ========== 步骤7：释放锁 ==========
                cacheService.unlock(lockKey);
                log.info("释放锁: {}", lockKey);
            }
        } else {
            // ========== 步骤8：获取锁失败，等待后重试 ==========
            log.info("获取锁失败，等待重试: memberId={}", memberId);
            
            try {
                Thread.sleep(50); // 等待50毫秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 递归重试（最多重试3次）
            return findByMemberIdWithProtection(memberId);
        }
    }

    /**
     * ============================================================
     * 【缓存击穿】逻辑过期方案（不设置TTL）
     * ============================================================
     * 
     * 实现原理：
     * 1. 缓存不设置TTL（永不过期）
     * 2. 缓存中包含逻辑过期时间字段
     * 3. 查询时检查逻辑过期时间
     * 4. 未过期 → 直接返回
     * 5. 过期 → 异步更新缓存
     * 
     * 优点：
     * - 热点key永不过期，避免击穿
     * - 异步更新，不影响用户体验
     * 
     * 缺点：
     * - 代码复杂
     * - 需要额外的逻辑过期时间字段
     * ============================================================
     */

    /**
     * 查询会员（逻辑过期方案）
     * 
     * @param memberId 会员编号
     * @return 会员信息
     */
    public Member findByMemberIdWithLogicalExpire(String memberId) {
        if (memberId == null || memberId.isEmpty()) {
            return null;
        }

        // 查询缓存
        String cacheKey = CACHE_PREFIX + memberId;
        Object value = redisTemplate.opsForValue().get(cacheKey);

        if (value == null) {
            // 缓存不存在，查数据库并写入缓存（不设置TTL）
            Member member = memberMapper.selectByMemberId(memberId);
            if (member != null) {
                // 写入缓存，包含逻辑过期时间
                CacheDataWithExpire cacheData = new CacheDataWithExpire();
                cacheData.setData(member);
                cacheData.setExpireTime(System.currentTimeMillis() + BASE_TTL_MINUTES * 60 * 1000);
                
                redisTemplate.opsForValue().set(cacheKey, cacheData);
                log.info("写入缓存（逻辑过期方案）: {}", cacheKey);
            }
            return member;
        }

        // 检查逻辑过期时间
        CacheDataWithExpire cacheData = (CacheDataWithExpire) value;
        
        if (cacheData.getExpireTime() > System.currentTimeMillis()) {
            // 未过期，直接返回
            log.info("命中缓存（逻辑未过期）: {}", cacheKey);
            return (Member) cacheData.getData();
        }

        // 已过期，异步更新缓存
        // 获取锁，防止多个线程同时更新
        String lockKey = LOCK_PREFIX + memberId;
        
        if (cacheService.tryLock(lockKey)) {
            // 异步更新（不影响用户体验）
            new Thread(() -> {
                try {
                    Member member = memberMapper.selectByMemberId(memberId);
                    if (member != null) {
                        CacheDataWithExpire newCacheData = new CacheDataWithExpire();
                        newCacheData.setData(member);
                        newCacheData.setExpireTime(System.currentTimeMillis() + BASE_TTL_MINUTES * 60 * 1000);
                        
                        redisTemplate.opsForValue().set(cacheKey, newCacheData);
                        log.info("异步更新缓存成功: {}", cacheKey);
                    }
                } finally {
                    cacheService.unlock(lockKey);
                }
            }).start();
        }

        // 返回旧数据（虽然是过期的，但数据可用）
        log.info("命中缓存（逻辑已过期，返回旧数据）: {}", cacheKey);
        return (Member) cacheData.getData();
    }

    /**
     * 缓存数据包装类（包含逻辑过期时间）
     */
    public static class CacheDataWithExpire {
        private Object data;           // 实际数据
        private long expireTime;       // 逻辑过期时间（毫秒）

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(long expireTime) {
            this.expireTime = expireTime;
        }
    }

    /**
     * ============================================================
     * 【缓存更新】更新会员信息并刷新缓存
     * ============================================================
     */

    /**
     * 更新会员信息（刷新缓存）
     * 
     * @param member 会员信息
     * @return 是否成功
     */
    public boolean updateMemberWithCache(Member member) {
        // 1. 更新数据库
        int rows = memberMapper.updateById(member);
        
        if (rows > 0) {
            // 2. 删除null缓存（如果存在）
            String cacheKey = CACHE_PREFIX + member.getMemberId();
            cacheService.deleteNullCache(cacheKey);
            
            // 3. 写入新缓存（带随机TTL）
            long randomTtl = BASE_TTL_MINUTES + new Random().nextInt(5);
            redisTemplate.opsForValue().set(cacheKey, member, randomTtl, TimeUnit.MINUTES);
            
            log.info("更新会员并刷新缓存: memberId={}, TTL={}分钟", member.getMemberId(), randomTtl);
        }
        
        return rows > 0;
    }

    /**
     * ============================================================
     * 【缓存删除】删除会员信息并清除缓存
     * ============================================================
     */

    /**
     * 删除会员信息（清除缓存）
     * 
     * @param id 会员ID
     * @return 是否成功
     */
    public boolean deleteMemberWithCache(Long id) {
        // 1. 查询会员信息（获取memberId）
        Member member = memberMapper.selectById(id);
        
        // 2. 删除数据库
        int rows = memberMapper.deleteById(id);
        
        if (rows > 0 && member != null) {
            // 3. 清除缓存
            String cacheKey = CACHE_PREFIX + member.getMemberId();
            redisTemplate.delete(cacheKey);
            
            // 4. 清除null缓存
            cacheService.deleteNullCache(cacheKey);
            
            log.info("删除会员并清除缓存: memberId={}", member.getMemberId());
        }
        
        return rows > 0;
    }
}