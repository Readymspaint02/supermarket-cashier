package com.zmj.gbs_commerce_system.controller;

import com.zmj.gbs_commerce_system.entity.OrderItem;
import com.zmj.gbs_commerce_system.entity.Orders;
import com.zmj.gbs_commerce_system.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public")
@Tag(name = "公开接口")
public class PublicController {
    
    @Autowired
    private OrderService orderService;
    
    @GetMapping("/order/{orderNo}")
    @Operation(summary = "根据订单号查询订单详情（公开接口，无需登录）")
    public Map<String, Object> getOrderByOrderNoPublic(
            @Parameter(description = "订单号") @PathVariable String orderNo) {
        Map<String, Object> result = new HashMap<>();
        try {
            Orders order = orderService.getOrderByOrderNo(orderNo);
            if (order == null) {
                result.put("code", 404);
                result.put("msg", "订单不存在");
                return result;
            }
            
            List<OrderItem> items = orderService.getOrderItemsByOrderId(order.getId());
            order.setItems(items);
            
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", order);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
        }
        return result;
    }
}
