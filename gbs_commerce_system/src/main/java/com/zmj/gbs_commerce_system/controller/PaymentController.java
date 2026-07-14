package com.zmj.gbs_commerce_system.controller;

import com.zmj.gbs_commerce_system.dto.PaymentRequest;
import com.zmj.gbs_commerce_system.dto.FacePayRequest;
import com.zmj.gbs_commerce_system.entity.Orders;
import com.zmj.gbs_commerce_system.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
@Tag(name = "支付中心接口")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create")
    @Operation(summary = "创建支付订单")
    public Map<String, Object> createPayment(@RequestBody PaymentRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Orders order = paymentService.createPaymentOrder(request);
            result.put("code", 200);
            result.put("msg", "支付订单创建成功");
            result.put("data", order);
        } catch (Exception e) {
            log.error("创建支付订单失败", e);
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @PostMapping("/confirm")
    @Operation(summary = "确认支付")
    public Map<String, Object> confirmPayment(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long orderId = Long.valueOf(params.get("orderId").toString());
            Integer paymentMethod = Integer.valueOf(params.get("paymentMethod").toString());
            Orders order = paymentService.confirmPayment(orderId, paymentMethod);
            result.put("code", 200);
            result.put("msg", "支付成功");
            result.put("data", order);
        } catch (Exception e) {
            log.error("支付确认失败", e);
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @PostMapping("/facepay")
    @Operation(summary = "刷脸支付（1:1验证，需提供会员ID）")
    public Map<String, Object> facePay(@RequestBody FacePayRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Orders order = paymentService.facePay(request);
            result.put("code", 200);
            result.put("msg", "刷脸支付成功");
            result.put("data", order);
        } catch (Exception e) {
            log.error("刷脸支付失败", e);
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @PostMapping("/facepay/search")
    @Operation(summary = "刷脸支付（1:N搜索，无需会员ID）")
    public Map<String, Object> facePaySearch(@RequestBody FacePayRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Orders order = paymentService.facePayBySearch(request);
            result.put("code", 200);
            result.put("msg", "刷脸支付成功");
            result.put("data", order);
        } catch (Exception e) {
            log.error("刷脸支付失败", e);
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    @GetMapping("/query/{orderId}")
    @Operation(summary = "查询支付状态")
    public Map<String, Object> queryPayment(@PathVariable Long orderId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Orders order = paymentService.queryPayment(orderId);
            result.put("code", 200);
            result.put("data", order);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }
}