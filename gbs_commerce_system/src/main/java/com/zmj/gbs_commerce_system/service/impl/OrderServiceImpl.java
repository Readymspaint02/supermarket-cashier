package com.zmj.gbs_commerce_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.*;
import com.zmj.gbs_commerce_system.mapper.InventoryLogMapper;
import com.zmj.gbs_commerce_system.mapper.InventoryMapper;
import com.zmj.gbs_commerce_system.mapper.OrderItemMapper;
import com.zmj.gbs_commerce_system.mapper.OrdersMapper;
import com.zmj.gbs_commerce_system.mapper.ProductMapper;
import com.zmj.gbs_commerce_system.service.OrderService;
import com.zmj.gbs_commerce_system.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 订单管理 Service 实现类
 * 
 * 实现订单（收银）管理的业务逻辑
 * 
 * 核心功能：收银结账
 * 这是整个系统最复杂的业务逻辑！
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    @Override
    public IPage<Orders> getOrdersWithPagination(Page<Orders> page, Map<String, Object> queryParams) {
        QueryWrapper<Orders> queryWrapper = new QueryWrapper<>();

        // 按订单号查询
        if (queryParams != null && queryParams.containsKey("orderNo") && queryParams.get("orderNo") != null) {
            queryWrapper.like("order_no", queryParams.get("orderNo"));
        }

        // 按收银员ID查询
        if (queryParams != null && queryParams.containsKey("cashierId") && queryParams.get("cashierId") != null) {
            queryWrapper.eq("cashier_id", queryParams.get("cashierId"));
        }

        // 按订单状态查询
        if (queryParams != null && queryParams.containsKey("orderStatus") && queryParams.get("orderStatus") != null) {
            queryWrapper.eq("order_status", queryParams.get("orderStatus"));
        }

        // 按时间范围查询
        if (queryParams != null && queryParams.containsKey("startTime") && queryParams.get("startTime") != null) {
            String startTime = queryParams.get("startTime").toString().trim();
            if (!startTime.isEmpty()) {
                queryWrapper.ge("create_time", startTime);
            }
        }
        if (queryParams != null && queryParams.containsKey("endTime") && queryParams.get("endTime") != null) {
            String endTime = queryParams.get("endTime").toString().trim();
            if (!endTime.isEmpty()) {
                queryWrapper.le("create_time", endTime);
            }
        }

        // 按创建时间倒序
        queryWrapper.orderByDesc("create_time");

        return ordersMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Orders getOrderById(Long id) {
        return ordersMapper.selectById(id);
    }

    @Override
    public Orders getOrderByOrderNo(String orderNo) {
        return ordersMapper.selectByOrderNo(orderNo);
    }

    @Override
    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        return orderItemMapper.selectByOrderId(orderId);
    }

    /**
     * 创建订单（收银结账）
     * 
     * 这是整个系统最核心的业务逻辑！
     * 
     * 业务流程：
     * 1. 验证购物车商品
     * 2. 计算订单金额
     * 3. 生成订单号
     * 4. 创建订单主表
     * 5. 创建订单明细
     * 6. 扣减库存（使用乐观锁）
     * 7. 记录库存变动日志
     * 
     * 注意事项：
     * - 使用 @Transactional 保证事务一致性
     * - 使用乐观锁防止超卖
     * - 任何步骤失败都会自动回滚
     * 
     * @param orderData 订单数据
     *                  结构：{
     *                  "cartItems": [ // 购物车商品列表
     *                  {
     *                  "productId": 1,
     *                  "quantity": 2
     *                  }
     *                  ],
     *                  "paidAmount": 100.00, // 实付金额
     *                  "discountAmount": 0.00, // 优惠金额
     *                  "paymentMethod": 1, // 支付方式
     *                  "remark": "备注"
     *                  }
     * @return 订单信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders createOrder(Map<String, Object> orderData) throws RuntimeException {
        // ========== 第1步：获取购物车商品列表 ==========
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cartItems = (List<Map<String, Object>>) orderData.get("cartItems");

        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("购物车为空");
        }

        // ========== 第2步：验证商品和库存，计算总金额 ==========
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map<String, Object> item : cartItems) {
            Long productId = Long.valueOf(item.get("productId").toString());
            Integer quantity = Integer.valueOf(item.get("quantity").toString());

            // 验证商品
            Product product = productMapper.selectById(productId);
            if (product == null || product.getStatus() != 0) {
                throw new RuntimeException("商品【ID:" + productId + "】已下架或不存在");
            }

            // 验证库存
            Inventory inventory = inventoryMapper.selectByProductId(productId);
            if (inventory == null || inventory.getStockQuantity() < quantity) {
                throw new RuntimeException("商品【" + product.getProductName() + "】库存不足！" +
                        "当前库存：" + (inventory != null ? inventory.getStockQuantity() : 0) +
                        "，需要：" + quantity);
            }

            // 计算金额
            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(quantity));
            totalAmount = totalAmount.add(itemTotal);

            // 将商品信息暂存到 item 中，后面创建订单明细时使用
            item.put("product", product);
            item.put("itemTotal", itemTotal);
        }

        // ========== 第3步：生成订单号 ==========
        String orderNo = generateOrderNo();

        // ========== 第4步：创建订单主表 ==========
        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setTotalAmount(totalAmount);

        // 实付金额
        BigDecimal paidAmount = new BigDecimal(orderData.get("paidAmount").toString());
        order.setPaidAmount(paidAmount);

        // 优惠金额 = 总金额 - 实付金额
        BigDecimal discountAmount = totalAmount.subtract(paidAmount);
        order.setDiscountAmount(discountAmount);

        // 支付方式
        Integer paymentMethod = Integer.valueOf(orderData.get("paymentMethod").toString());
        order.setPaymentMethod(paymentMethod);

        // 订单状态：1-已完成
        order.setOrderStatus(1);

        // 收银员信息（从当前登录用户获取）
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            order.setCashierId(currentUser.getId());
            order.setCashierName(
                    currentUser.getNickname() != null ? currentUser.getNickname() : currentUser.getUsername());
        }

        // 备注
        if (orderData.containsKey("remark")) {
            order.setRemark(orderData.get("remark").toString());
        }

        // 保存订单
        ordersMapper.insert(order);

        // ========== 第5步：创建订单明细 + 扣减库存 ==========
        for (Map<String, Object> item : cartItems) {
            Long productId = Long.valueOf(item.get("productId").toString());
            Integer quantity = Integer.valueOf(item.get("quantity").toString());
            Product product = (Product) item.get("product");
            BigDecimal itemTotal = (BigDecimal) item.get("itemTotal");

            // 5.1 创建订单明细
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(productId);
            orderItem.setProductName(product.getProductName());
            orderItem.setProductCode(product.getProductCode());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(quantity);
            orderItem.setSubtotal(itemTotal);
            orderItemMapper.insert(orderItem);

            // 5.2 扣减库存（使用乐观锁）
            int rows = inventoryMapper.decreaseStock(productId, quantity);
            if (rows == 0) {
                // 扣减失败（库存不足或并发问题）
                throw new RuntimeException("商品【" + product.getProductName() + "】库存扣减失败，请重试");
            }

            // 5.3 记录库存变动日志
            Inventory currentInventory = inventoryMapper.selectByProductId(productId);
            InventoryLog log = new InventoryLog();
            log.setProductId(productId);
            log.setChangeType(3); // 3-销售
            log.setChangeQuantity(-quantity); // 负数表示减少
            log.setBeforeQuantity(currentInventory.getStockQuantity() + quantity);
            log.setAfterQuantity(currentInventory.getStockQuantity());
            log.setOperator(order.getCashierName());
            log.setCreateTime(new Date());
            log.setRemark("订单：" + orderNo);
            inventoryLogMapper.insert(log);
        }

        // ========== 第6步：返回订单信息 ==========
        return order;
    }

    /**
     * 生成订单号
     * 格式：yyyyMMddHHmmss + 6位随机数
     * 例如：20251027153045123456
     * 
     * @return 订单号
     */
    private String generateOrderNo() {
        String date = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String random = String.format("%06d", new Random().nextInt(999999));
        return date + random;
    }

    /**
     * 订单退款
     * 
     * 业务流程：
     * 1. 查询订单和订单明细
     * 2. 更新订单状态为"已退款"
     * 3. 退回库存
     * 4. 记录库存变动日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refundOrder(Long orderId) throws RuntimeException {
        // 1. 查询订单
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getOrderStatus() == 2) {
            throw new RuntimeException("订单已退款，无法重复退款");
        }

        // 2. 查询订单明细
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);

        // 3. 退回库存
        for (OrderItem item : orderItems) {
            // 增加库存
            inventoryMapper.increaseStock(item.getProductId(), item.getQuantity());

            // 记录库存变动日志
            Inventory currentInventory = inventoryMapper.selectByProductId(item.getProductId());
            InventoryLog log = new InventoryLog();
            log.setProductId(item.getProductId());
            log.setChangeType(4); // 4-退货
            log.setChangeQuantity(item.getQuantity()); // 正数表示增加
            log.setBeforeQuantity(currentInventory.getStockQuantity() - item.getQuantity());
            log.setAfterQuantity(currentInventory.getStockQuantity());
            log.setOperator(SecurityUtils.getCurrentUser().getNickname());
            log.setCreateTime(new Date());
            log.setRemark("订单退款：" + order.getOrderNo());
            inventoryLogMapper.insert(log);
        }

        // 4. 更新订单状态
        order.setOrderStatus(2); // 2-已退款
        ordersMapper.updateById(order);

        return true;
    }
}
