package com.zmj.gbs_commerce_system.mq.consumer;

import com.zmj.gbs_commerce_system.config.RabbitMQConfig;
import com.zmj.gbs_commerce_system.dto.OrderPaidMessage;
import com.zmj.gbs_commerce_system.entity.Member;
import com.zmj.gbs_commerce_system.mapper.MemberMapper;
import com.zmj.gbs_commerce_system.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MemberPointsConsumer {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "member:";
    private static final long CACHE_TTL = 30;
    private static final BigDecimal POINTS_RATE = new BigDecimal("10");

    @RabbitListener(queues = RabbitMQConfig.ORDER_PAID_QUEUE)
    public void handleOrderPaid(OrderPaidMessage message) {
        log.info("收到订单支付消息: orderId={}, orderNo={}, memberId={}, paidAmount={}, paymentMethod={}",
                message.getOrderId(), message.getOrderNo(),
                message.getMemberId(), message.getPaidAmount(), message.getPaymentMethod());

        if (message.getMemberId() == null || message.getMemberId().isEmpty()) {
            log.info("非会员订单，跳过积分处理");
            return;
        }

        try {
            Member member = memberMapper.selectByMemberId(message.getMemberId());
            if (member == null) {
                log.warn("会员不存在: {}", message.getMemberId());
                return;
            }

            if (message.getPaymentMethod() != null && message.getPaymentMethod() == 6) {
                try {
                    memberService.deductBalance(message.getMemberId(), message.getPaidAmount());
                    log.info("余额支付扣减成功: memberId={}, 扣减金额={}", message.getMemberId(), message.getPaidAmount());
                } catch (Exception e) {
                    log.error("余额扣减失败: memberId={}, error={}", message.getMemberId(), e.getMessage());
                    throw e;
                }
            }

            int pointsToAdd = calculatePoints(message.getPaidAmount());
            if (pointsToAdd <= 0) {
                log.info("无需增加积分: paidAmount={}", message.getPaidAmount());
                return;
            }

            int currentPoints = member.getPoints() != null ? member.getPoints() : 0;
            int newPoints = currentPoints + pointsToAdd;
            member.setPoints(newPoints);

            int rows = memberMapper.updateById(member);
            if (rows > 0) {
                String cacheKey = CACHE_PREFIX + member.getMemberId();
                redisTemplate.opsForValue().set(cacheKey, member, CACHE_TTL, TimeUnit.MINUTES);
                log.info("会员积分更新成功: memberId={}, 原积分={}, 新增={}, 当前积分={}",
                        member.getMemberId(), currentPoints, pointsToAdd, newPoints);
            } else {
                log.error("会员积分更新失败: memberId={}", member.getMemberId());
            }

        } catch (Exception e) {
            log.error("处理订单积分异常: orderId={}, error={}", message.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }

    private int calculatePoints(BigDecimal paidAmount) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return paidAmount.divide(POINTS_RATE, 0, RoundingMode.DOWN).intValue();
    }
}