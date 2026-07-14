package com.zmj.gbs_commerce_system.service.impl;

import com.zmj.gbs_commerce_system.config.BaiduAiProperties;
import com.zmj.gbs_commerce_system.dto.FacePayRequest;
import com.zmj.gbs_commerce_system.dto.PaymentRequest;
import com.zmj.gbs_commerce_system.entity.Member;
import com.zmj.gbs_commerce_system.entity.Orders;
import com.zmj.gbs_commerce_system.mapper.MemberMapper;
import com.zmj.gbs_commerce_system.mq.producer.OrderPaidProducer;
import com.zmj.gbs_commerce_system.service.FaceAuthService;
import com.zmj.gbs_commerce_system.service.OrderService;
import com.zmj.gbs_commerce_system.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OrderPaidProducer orderPaidProducer;

    @Autowired
    private BaiduAiProperties baiduAiProperties;

    @Autowired
    private FaceAuthService faceAuthService;

    private static final String CACHE_PREFIX = "member:";
    private static final String FACE_TOKEN_PREFIX = "face_token:";

    @Override
    @Transactional
    public Orders createPaymentOrder(PaymentRequest request) {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("cartItems", request.getCartItems());
        orderData.put("paidAmount", request.getPaidAmount());
        orderData.put("discountAmount", request.getDiscountAmount());
        orderData.put("paymentMethod", 1);
        orderData.put("remark", request.getRemark());
        
        Orders order = orderService.createOrder(orderData);
        log.info("支付订单创建成功: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        
        String cacheKey = "order:pending:" + order.getId();
        redisTemplate.opsForValue().set(cacheKey, order, 30, TimeUnit.MINUTES);
        
        return order;
    }

    @Override
    @Transactional
    public Orders confirmPayment(Long orderId, Integer paymentMethod) {
        Orders order = orderService.getOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        order.setPaymentMethod(paymentMethod);
        order.setOrderStatus(1);
        
        redisTemplate.delete("order:pending:" + orderId);
        
        if (order.getMemberId() != null && !order.getMemberId().isEmpty()) {
            orderPaidProducer.sendOrderPaidMessage(order.getId(), order.getOrderNo(), order.getPaidAmount(), order.getCashierId());
            log.info("发送积分处理消息: orderId={}", orderId);
        }
        
        log.info("支付确认成功: orderId={}, paymentMethod={}", orderId, paymentMethod);
        return order;
    }

    @Override
    @Transactional
    public Orders facePay(FacePayRequest request) {
        if (request.getOrderId() != null) {
            Orders existingOrder = orderService.getOrderById(request.getOrderId());
            if (existingOrder != null) {
                boolean faceVerified = verifyFace(request.getFaceImage(), request.getMemberId());
                if (!faceVerified) {
                    throw new RuntimeException("人脸验证失败，请重新尝试");
                }
                
                existingOrder.setPaymentMethod(5);
                existingOrder.setOrderStatus(1);
                
                if (existingOrder.getMemberId() != null) {
                    orderPaidProducer.sendOrderPaidMessage(existingOrder.getId(), existingOrder.getOrderNo(), existingOrder.getPaidAmount(), existingOrder.getCashierId());
                }
                
                log.info("刷脸支付成功: orderId={}, memberId={}", existingOrder.getId(), request.getMemberId());
                return existingOrder;
            }
        }
        
        boolean faceVerified = verifyFace(request.getFaceImage(), request.getMemberId());
        if (!faceVerified) {
            throw new RuntimeException("人脸验证失败，请重新尝试");
        }
        
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("cartItems", request.getCartItems());
        orderData.put("paidAmount", request.getPaidAmount());
        orderData.put("discountAmount", request.getDiscountAmount());
        orderData.put("paymentMethod", 5);
        orderData.put("remark", request.getRemark());
        
        Orders order = orderService.createOrder(orderData);
        
        Member member = memberMapper.selectByMemberId(request.getMemberId());
        if (member != null) {
            order.setMemberId(request.getMemberId());
        }
        
        order.setOrderStatus(1);
        
        orderPaidProducer.sendOrderPaidMessage(order.getId(), order.getOrderNo(), order.getPaidAmount(), order.getCashierId());
        
        log.info("刷脸支付创建订单成功: orderId={}, memberId={}", order.getId(), request.getMemberId());
        return order;
    }

    @Override
    @Transactional
    public Orders facePayBySearch(FacePayRequest request) {
        log.info("开始1:N人脸搜索支付");
        
        if (baiduAiProperties.getApiKey() == null || baiduAiProperties.getApiKey().isEmpty()) {
            log.warn("百度人脸识别未配置，跳过验证");
            String defaultMemberId = "M001";
            return createOrderWithMember(request, defaultMemberId);
        }
        
        if (request.getFaceImage() == null || request.getFaceImage().isEmpty()) {
            throw new RuntimeException("人脸图像不能为空");
        }
        
        Optional<String> matchedMemberId = faceAuthService.matchAndGetUsername(request.getFaceImage());
        if (matchedMemberId.isEmpty()) {
            throw new RuntimeException("人脸识别失败，未找到匹配的会员，请确保已注册人脸");
        }
        
        String memberId = matchedMemberId.get();
        log.info("人脸搜索匹配成功: memberId={}", memberId);
        
        Member member = memberMapper.selectByMemberId(memberId);
        if (member == null) {
            throw new RuntimeException("匹配的会员不存在: " + memberId);
        }
        
        return createOrderWithMember(request, memberId);
    }
    
    private Orders createOrderWithMember(FacePayRequest request, String memberId) {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("cartItems", request.getCartItems());
        orderData.put("paidAmount", request.getPaidAmount());
        orderData.put("discountAmount", request.getDiscountAmount());
        orderData.put("paymentMethod", 5);
        orderData.put("remark", request.getRemark());
        
        Orders order = orderService.createOrder(orderData);
        order.setMemberId(memberId);
        order.setOrderStatus(1);
        
        orderPaidProducer.sendOrderPaidMessage(order.getId(), order.getOrderNo(), order.getPaidAmount(), order.getCashierId());
        
        log.info("刷脸支付订单创建成功: orderId={}, memberId={}", order.getId(), memberId);
        return order;
    }

    @Override
    public Orders queryPayment(Long orderId) {
        String cacheKey = "order:pending:" + orderId;
        Orders order = (Orders) redisTemplate.opsForValue().get(cacheKey);
        if (order != null) {
            log.info("命中待支付订单缓存: {}", cacheKey);
            return order;
        }
        return orderService.getOrderById(orderId);
    }

    private boolean verifyFace(String faceImage, String memberId) {
        log.info("开始人脸验证: memberId={}", memberId);
        
        if (baiduAiProperties.getApiKey() == null || baiduAiProperties.getApiKey().isEmpty()) {
            log.warn("百度人脸识别未配置，跳过验证");
            return true;
        }
        
        if (faceImage == null || faceImage.isEmpty()) {
            log.warn("人脸图像为空");
            return false;
        }
        
        boolean verified = faceAuthService.verifyFaceForMember(faceImage, memberId);
        
        if (verified) {
            redisTemplate.opsForValue().set(FACE_TOKEN_PREFIX + memberId, "verified", 5, TimeUnit.MINUTES);
            log.info("人脸验证通过: memberId={}", memberId);
        } else {
            log.warn("人脸验证失败: memberId={}", memberId);
        }
        
        return verified;
    }
}