package com.zmj.gbs_commerce_system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.OrderItem;
import com.zmj.gbs_commerce_system.entity.Orders;
import com.zmj.gbs_commerce_system.metrics.BusinessMetrics;
import com.zmj.gbs_commerce_system.service.OrderService;
import com.zmj.gbs_commerce_system.utils.PageParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单管理（收银管理）Controller
 * 
 * 提供收银结账、订单查询、订单退款接口
 * 
 * 接口路径：/api/order/...
 * 
 * 这是整个系统最核心的Controller！
 */
@RestController
@RequestMapping("/order")
@Tag(name = "订单管理接口")
public class OrderController {
    
    @Autowired
    private OrderService orderService;

    @Autowired
    private BusinessMetrics businessMetrics;
    
    /**
     * 分页查询订单列表
     * 
     * 请求参数示例：
     * {
     *   "pageNum": 1,
     *   "pageSize": 10,
     *   "queryParams": {
     *     "orderNo": "20251027",      // 可选：按订单号模糊查询
     *     "cashierId": 1,             // 可选：按收银员ID查询
     *     "orderStatus": 1,           // 可选：按订单状态查询（1-已完成，2-已退款）
     *     "startTime": "2025-10-01",  // 可选：开始时间
     *     "endTime": "2025-10-31"     // 可选：结束时间
     *   }
     * }
     */
    @RequiresPermissions("cashier:order:list")
    @PostMapping("/page")
    @Operation(summary = "分页查询订单列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getOrdersWithPagination(
            @RequestBody PageParams pageParams) {
        Integer pageNum = pageParams.getPageNum() != null ? pageParams.getPageNum() : 1;
        Integer pageSize = pageParams.getPageSize() != null ? pageParams.getPageSize() : 10;
        Page<Orders> page = new Page<>(pageNum, pageSize);
        IPage<Orders> orderPage = orderService.getOrdersWithPagination(page, pageParams.getQueryParams());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", orderPage);
        return result;
    }
    
    /**
     * 根据ID查询订单
     */
    @RequiresPermissions("cashier:order:detail")
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询订单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "订单不存在")
    })
    public Map<String, Object> getOrderById(
            @Parameter(description = "订单ID") @PathVariable Long id) {
        Orders order = orderService.getOrderById(id);
        Map<String, Object> result = new HashMap<>();
        if (order != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", order);
        } else {
            result.put("code", 500);
            result.put("msg", "订单不存在");
        }
        return result;
    }
    
    /**
     * 根据订单号查询订单
     * 
     * 接口路径：GET /api/order/no/{orderNo}
     * 例如：GET /api/order/no/20251027153045123456
     */
    @RequiresPermissions("cashier:order:detail")
    @GetMapping("/no/{orderNo}")
    @Operation(summary = "根据订单号查询订单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "订单不存在")
    })
    public Map<String, Object> getOrderByOrderNo(
            @Parameter(description = "订单号") @PathVariable String orderNo) {
        Orders order = orderService.getOrderByOrderNo(orderNo);
        Map<String, Object> result = new HashMap<>();
        if (order != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", order);
        } else {
            result.put("code", 500);
            result.put("msg", "订单不存在");
        }
        return result;
    }
    
    /**
     * 根据订单ID查询订单明细
     * 
     * 接口路径：GET /api/order/{orderId}/items
     */
    @RequiresPermissions("cashier:order:detail")
    @GetMapping("/{orderId}/items")
    @Operation(summary = "根据订单ID查询订单明细")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getOrderItems(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        List<OrderItem> orderItems = orderService.getOrderItemsByOrderId(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", orderItems);
        return result;
    }
    
    /**
     * 创建订单（收银结账）
     * 
     * 权限要求：cashier:checkout
     * 
     * 这是整个系统最核心的接口！
     * 
     * 请求体示例：
     * {
     *   "cartItems": [
     *     {
     *       "productId": 1,
     *       "quantity": 2
     *     },
     *     {
     *       "productId": 2,
     *       "quantity": 1
     *     }
     *   ],
     *   "paidAmount": 8.00,        // 实付金额
     *   "discountAmount": 0.00,    // 优惠金额（可选）
     *   "paymentMethod": 1,        // 支付方式（1-现金，2-微信，3-支付宝，4-银行卡）
     *   "remark": "备注"           // 备注（可选）
     * }
     * 
     * 业务流程：
     * 1. 验证购物车商品
     * 2. 检查库存是否充足
     * 3. 计算订单金额
     * 4. 生成订单号
     * 5. 创建订单主表和明细
     * 6. 扣减库存（使用乐观锁）
     * 7. 记录库存变动日志
     * 8. 返回订单信息
     * 
     * 注意：
     * - 使用事务保证数据一致性
     * - 任何步骤失败都会自动回滚
     * - 使用乐观锁防止超卖
     */
    @RequiresPermissions("cashier:checkout")
    @PostMapping("/checkout")
    @Operation(summary = "创建订单（收银结账）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "结账成功"),
            @ApiResponse(responseCode = "500", description = "结账失败")
    })
    public Map<String, Object> checkout(@RequestBody Map<String, Object> orderData) {
        Map<String, Object> result = new HashMap<>();
        try {
            businessMetrics.incrementOrderCreate();
            Orders order = orderService.createOrder(orderData);
            businessMetrics.incrementOrderSuccess();
            businessMetrics.addOrderAmount(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0);
            result.put("code", 200);
            result.put("msg", "结账成功");
            result.put("data", order);
        } catch (RuntimeException e) {
            businessMetrics.incrementOrderFail();
            result.put("code", 500);
            result.put("msg", "结账失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 订单退款
     * 
     * 权限要求：cashier:order:refund
     * 
     * 业务流程：
     * 1. 查询订单和订单明细
     * 2. 更新订单状态为"已退款"
     * 3. 退回库存
     * 4. 记录库存变动日志
     * 
     * 接口路径：POST /api/order/{orderId}/refund
     */
    @RequiresPermissions("cashier:order:refund")
    @PostMapping("/{orderId}/refund")
    @Operation(summary = "订单退款")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "退款成功"),
            @ApiResponse(responseCode = "500", description = "退款失败")
    })
    public Map<String, Object> refundOrder(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = orderService.refundOrder(orderId);
            if (success) {
                result.put("code", 200);
                result.put("msg", "退款成功");
            } else {
                result.put("code", 500);
                result.put("msg", "退款失败");
            }
        } catch (RuntimeException e) {
            result.put("code", 500);
            result.put("msg", "退款失败：" + e.getMessage());
        }
        return result;
    }
}

