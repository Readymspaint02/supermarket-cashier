package com.zmj.gbs_commerce_system.service.impl;

/**
 * ============================================================
 * 【缓存-01】MemberServiceImpl - 会员缓存策略（Cache-Aside模式）
 * ============================================================
 * 
 * 文件作用：
 * 会员业务实现，重点：Redis缓存策略，减少数据库查询压力。
 * 
 * 技术原理：
 * - Cache-Aside模式：读时先查缓存，未命中查数据库并写入缓存
 * - TTL（Time To Live）：缓存过期时间，防止脏数据长期存在
 * - 缓存更新：更新数据库后，主动刷新/删除缓存
 * 
 * 业务流程（查询）：
 * 1. 构建缓存key（如：member:id:123）
 * 2. 从Redis查询缓存
 * 3. 缓存命中 → 直接返回
 * 4. 缓存未命中 → 查数据库 → 写入缓存 → 返回
 * 
 * 业务流程（更新）：
 * 1. 更新数据库
 * 2. 更新成功 → 删除旧缓存 → 写入新缓存
 * 3. 返回结果
 * 
 * 面试考点：
 * - Q1：什么是Cache-Aside模式？
 *   A1：读时先查缓存，未命中查数据库并写入缓存；
 *       写时先更新数据库，再删除/更新缓存。
 *       这是最常用的缓存策略，简单可靠。
 * 
 * - Q2：为什么设置TTL为30分钟？
 *   A2：会员信息变化频率低，30分钟足够；
 *       如果会员被禁用或信息修改，30分钟后自动更新；
 *       可根据业务调整，一般10分钟-1小时。
 * 
 * - Q3：缓存穿透/击穿/雪崩是什么？如何解决？
 *   A3：- 穿透：查询不存在的数据，缓存和数据库都没有
 *           解决：缓存null值、布隆过滤器
 *       - 击穿：热点key过期，大量请求打到数据库
 *           解决：设置逻辑过期、互斥锁
 *       - 雪崩：大量key同时过期
 *           解决：TTL随机化、多级缓存
 * 
 * - Q4：为什么更新时刷新缓存而不是删除？
 *   A4：刷新缓存避免缓存穿透（删除后下次查询会打到数据库）；
 *       如果更新频率低，可以直接删除缓存，下次查询时重建。
 * 
 * - Q5：如何保证缓存和数据库的一致性？
 *   A5：采用"先更新数据库，再更新缓存"策略；
 *       虽然极端情况下可能不一致（如更新数据库成功但缓存失败），
 *       但通过TTL机制，最多30分钟后数据会自动修正；
 *       如需强一致性，可用分布式事务或消息队列保证。
 * 
 * 关联文件：
 * - mapper/MemberMapper.java（会员表操作）
 * - config/RedisConfig.java（Redis配置）
 * 
 * 参考文档：
 * - 梳理项目.md 3.3 Redis缓存模块
 * - 项目难点讲解.txt 难点四：Redis缓存一致性
 * ============================================================
 */

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Member;
import com.zmj.gbs_commerce_system.entity.RechargeRecord;
import com.zmj.gbs_commerce_system.mapper.MemberMapper;
import com.zmj.gbs_commerce_system.mapper.RechargeRecordMapper;
import com.zmj.gbs_commerce_system.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MemberServiceImpl implements MemberService {

    // 【缓存-01-依赖注入】数据库Mapper和Redis模板
    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private RechargeRecordMapper rechargeRecordMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 【缓存-01-常量定义】缓存key前缀和TTL
    private static final String CACHE_PREFIX = "member:";  // 缓存key前缀
    private static final long CACHE_TTL = 30;              // 缓存过期时间（分钟）

    @Override
    public List<Member> findAll() {
        return memberMapper.selectList(new QueryWrapper<Member>().orderByDesc("create_time"));
    }

    @Override
    public IPage<Member> findMembersWithPagination(Page<Member> page, Map<String, Object> queryParams) {
        QueryWrapper<Member> wrapper = new QueryWrapper<>();
        if (queryParams != null) {
            Object memberId = queryParams.get("memberId");
            if (memberId != null && !"".equals(memberId)) {
                wrapper.like("member_id", memberId);
            }
            Object name = queryParams.get("name");
            if (name != null && !"".equals(name)) {
                wrapper.like("name", name);
            }
            Object phone = queryParams.get("phone");
            if (phone != null && !"".equals(phone)) {
                wrapper.like("phone", phone);
            }
            Object level = queryParams.get("level");
            if (level != null && !"".equals(level)) {
                wrapper.eq("level", level);
            }
            Object status = queryParams.get("status");
            if (status != null && !"".equals(status)) {
                wrapper.eq("status", status);
            }
        }
        wrapper.orderByDesc("create_time");
        return memberMapper.selectPage(page, wrapper);
    }

    // 【缓存-01-查询方法】根据ID查询会员（带缓存）
    @Override
    public Member findById(Long id) {
        // 1. 构建缓存key：member:id:123
        String cacheKey = CACHE_PREFIX + "id:" + id;
        
        // 2. 从Redis查询缓存
        Member member = (Member) redisTemplate.opsForValue().get(cacheKey);
        
        // 3. 缓存命中 → 直接返回
        if (member != null) {
            log.info("命中缓存: {}", cacheKey);
            return member;
        }
        
        // 4. 缓存未命中 → 查数据库
        member = memberMapper.selectById(id);
        
        // 5. 写入缓存（30分钟TTL）
        if (member != null) {
            redisTemplate.opsForValue().set(cacheKey, member, CACHE_TTL, TimeUnit.MINUTES);
            log.info("写入缓存: {}", cacheKey);
        }
        
        return member;
    }

    // 【缓存-01-查询方法】根据会员编号查询（带缓存）
    @Override
    public Member findByMemberId(String memberId) {
        if (memberId == null || memberId.isEmpty()) {
            return null;
        }
        
        // 1. 构建缓存key：member:M001
        String cacheKey = CACHE_PREFIX + memberId;
        
        // 2. 从Redis查询缓存
        Member member = (Member) redisTemplate.opsForValue().get(cacheKey);
        
        // 3. 缓存命中 → 直接返回
        if (member != null) {
            log.info("命中缓存: {}", cacheKey);
            return member;
        }
        
        // 4. 缓存未命中 → 查数据库
        member = memberMapper.selectByMemberId(memberId);
        
        // 5. 写入缓存（30分钟TTL）
        if (member != null) {
            redisTemplate.opsForValue().set(cacheKey, member, CACHE_TTL, TimeUnit.MINUTES);
            log.info("写入缓存: {}", cacheKey);
        }
        
        return member;
    }

    @Override
    public Member findByPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }
        return memberMapper.selectByPhone(phone);
    }

    // 【缓存-01-创建方法】创建会员（写入缓存）
    @Override
    @Transactional
    public boolean createMember(Member member) {
        // 1. 插入数据库
        int rows = memberMapper.insert(member);
        
        // 2. 插入成功 → 写入缓存
        if (rows > 0) {
            String cacheKey = CACHE_PREFIX + member.getMemberId();
            redisTemplate.opsForValue().set(cacheKey, member, CACHE_TTL, TimeUnit.MINUTES);
            log.info("新建会员写入缓存: {}", cacheKey);
        }
        
        return rows > 0;
    }

    // 【缓存-01-更新方法】更新会员（刷新缓存）
    // 面试考点：为什么刷新缓存而不是删除？
    // 答：刷新缓存避免缓存穿透，删除后下次查询会打到数据库
    @Override
    @Transactional
    public boolean updateMember(Member member) {
        // 1. 更新数据库
        int rows = memberMapper.updateById(member);
        
        // 2. 更新成功 → 刷新缓存
        if (rows > 0) {
            // 刷新：member:M001 缓存
            String cacheKey = CACHE_PREFIX + member.getMemberId();
            redisTemplate.opsForValue().set(cacheKey, member, CACHE_TTL, TimeUnit.MINUTES);
            
            // 刷新：member:id:123 缓存
            String idCacheKey = CACHE_PREFIX + "id:" + member.getId();
            redisTemplate.opsForValue().set(idCacheKey, member, CACHE_TTL, TimeUnit.MINUTES);
            
            log.info("更新会员刷新缓存: {}", cacheKey);
        }
        
        return rows > 0;
    }

    // 【缓存-01-删除方法】删除会员（清除缓存）
    // 面试考点：删除会员时为什么要清除所有相关缓存？
    // 答：因为缓存有两个key（member:M001 和 member:id:123），都要清除
    @Override
    @Transactional
    public boolean deleteMember(Long id) {
        // 1. 查询会员信息（用于获取memberId）
        Member member = memberMapper.selectById(id);
        
        // 2. 删除数据库
        int rows = memberMapper.deleteById(id);
        
        // 3. 删除成功 → 清除缓存
        if (rows > 0 && member != null) {
            // 清除：member:M001 缓存
            String cacheKey = CACHE_PREFIX + member.getMemberId();
            redisTemplate.delete(cacheKey);
            
            // 清除：member:id:123 缓存
            String idCacheKey = CACHE_PREFIX + "id:" + id;
            redisTemplate.delete(idCacheKey);
            
            log.info("删除会员清除缓存: {}", cacheKey);
        }
        
        return rows > 0;
    }

    @Override
    @Transactional
    public boolean deductBalance(String memberId, BigDecimal amount) {
        if (memberId == null || memberId.isEmpty()) {
            throw new RuntimeException("会员编号不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("扣减金额必须大于0");
        }

        Member member = findByMemberId(memberId);
        if (member == null) {
            throw new RuntimeException("会员不存在: " + memberId);
        }

        BigDecimal currentBalance = member.getBalance() != null ? member.getBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(amount) < 0) {
            throw new RuntimeException("会员余额不足，当前余额: " + currentBalance);
        }

        member.setBalance(currentBalance.subtract(amount));
        int rows = memberMapper.updateById(member);

        if (rows > 0) {
            String cacheKey = CACHE_PREFIX + member.getMemberId();
            redisTemplate.opsForValue().set(cacheKey, member, CACHE_TTL, TimeUnit.MINUTES);
            log.info("会员余额扣减成功: memberId={}, 扣减金额={}, 剩余余额={}", memberId, amount, member.getBalance());
        }

        return rows > 0;
    }

    @Override
    @Transactional
    public boolean addBalance(String memberId, BigDecimal amount) {
        if (memberId == null || memberId.isEmpty()) {
            throw new RuntimeException("会员编号不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }

        Member member = findByMemberId(memberId);
        if (member == null) {
            throw new RuntimeException("会员不存在: " + memberId);
        }

        BigDecimal currentBalance = member.getBalance() != null ? member.getBalance() : BigDecimal.ZERO;
        member.setBalance(currentBalance.add(amount));
        int rows = memberMapper.updateById(member);

        if (rows > 0) {
            String cacheKey = CACHE_PREFIX + member.getMemberId();
            redisTemplate.opsForValue().set(cacheKey, member, CACHE_TTL, TimeUnit.MINUTES);
            log.info("会员余额充值成功: memberId={}, 充值金额={}, 当前余额={}", memberId, amount, member.getBalance());
        }

        return rows > 0;
    }

    @Override
    @Transactional
    public RechargeRecord recharge(String memberId, BigDecimal rechargeAmount, BigDecimal giftAmount, Integer pointsToAdd, String operator, String remark) {
        if (memberId == null || memberId.isEmpty()) {
            throw new RuntimeException("会员编号不能为空");
        }
        if (rechargeAmount == null || rechargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }

        Member member = findByMemberId(memberId);
        if (member == null) {
            throw new RuntimeException("会员不存在: " + memberId);
        }

        BigDecimal balanceBefore = member.getBalance() != null ? member.getBalance() : BigDecimal.ZERO;
        BigDecimal gift = giftAmount != null ? giftAmount : BigDecimal.ZERO;
        BigDecimal totalAmount = rechargeAmount.add(gift);
        BigDecimal balanceAfter = balanceBefore.add(totalAmount);

        member.setBalance(balanceAfter);
        
        int autoPoints = 0;
        if (pointsToAdd != null && pointsToAdd > 0) {
            autoPoints = pointsToAdd;
        } else {
            autoPoints = totalAmount.divide(new BigDecimal("10"), 0, java.math.RoundingMode.DOWN).intValue();
        }
        if (autoPoints > 0) {
            int currentPoints = member.getPoints() != null ? member.getPoints() : 0;
            member.setPoints(currentPoints + autoPoints);
        }
        
        int rows = memberMapper.updateById(member);

        if (rows > 0) {
            String cacheKey = CACHE_PREFIX + member.getMemberId();
            redisTemplate.opsForValue().set(cacheKey, member, CACHE_TTL, TimeUnit.MINUTES);
            log.info("会员充值成功: memberId={}, 充值金额={}, 赠送金额={}, 增加积分={}, 充值后余额={}", 
                    memberId, rechargeAmount, gift, pointsToAdd, balanceAfter);
        }

        RechargeRecord record = new RechargeRecord();
        record.setMemberId(memberId);
        record.setMemberName(member.getName());
        record.setRechargeAmount(rechargeAmount);
        record.setGiftAmount(gift);
        record.setTotalAmount(totalAmount);
        record.setPointsAdded(autoPoints);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setOperator(operator);
        record.setRemark(remark);
        record.setCreateTime(new Date());
        rechargeRecordMapper.insert(record);
        
        log.info("充值记录已保存: recordId={}, memberId={}", record.getId(), memberId);
        
        return record;
    }
}
