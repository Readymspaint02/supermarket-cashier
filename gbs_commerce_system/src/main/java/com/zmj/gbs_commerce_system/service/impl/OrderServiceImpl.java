package com.zmj.gbs_commerce_system.service.impl;

/**
 * ============================================================
 * 【订单-01】OrderServiceImpl - 订单创建与退款（事务控制）
 * ============================================================
 * 
 * 文件作用：
 * 订单核心业务实现，包含订单创建、退款、查询等功能。
 * 重点：订单创建使用@Transactional保证多表操作的原子性。
 * 
 * 技术原理：
 * - @Transactional：Spring事务管理，方法内所有数据库操作要么全部成功，要么全部回滚
 * - 库存扣减：使用SQL原子操作（UPDATE ... WHERE stock >= quantity）防止并发超卖
 * - RabbitMQ：订单支付成功后发送消息，异步处理会员积分
 * 
 * 业务流程（订单创建）：
 * 1. 验证购物车不为空
 * 2. 验证商品状态（是否上架）
 * 3. 验证库存是否充足
 * 4. 计算订单总金额
 * 5. 生成订单号（时间戳+随机数）
 * 6. 插入订单表
 * 7. 遍历商品，插入订单项表
 * 8. 扣减库存（原子操作）
 * 9. 记录库存变动日志
 * 10. 发送消息到RabbitMQ（异步处理积分）
 * 
 * 面试考点：
 * - Q1：订单创建涉及哪些表？如何保证一致性？
 *   A1：涉及4张表：订单表(orders)、订单项表(order_item)、
 *       库存表(inventory)、库存日志表(inventory_log)。
 *       使用@Transactional保证原子性，任何一步失败全部回滚。
 * 
 * - Q2：库存扣减如何防止超卖？
 *   A2：使用SQL原子操作：UPDATE inventory 
 *       SET stock = stock - #{quantity}
 *       WHERE product_id = #{productId} AND stock >= #{quantity}
 *       如果库存不足，UPDATE返回0行，判断rows==0抛异常触发回滚。
 * 
 * - Q3：为什么要发送消息到RabbitMQ？
 *   A3：会员积分计算和更新需要查数据库，同步处理会拖慢支付响应。
 *       通过RabbitMQ异步解耦，支付响应时间从2秒降到200ms。
 * 
 * - Q4：@Transactional什么时候会失效？
 *   A4：1. 方法不是public
 *       2. 异常被catch捕获未抛出
 *       3. 异常类型不是RuntimeException（可用rollbackFor指定）
 *       4. 同类方法调用（绕过代理）
 *       5. 数据库不支持事务（如MyISAM）
 * 
 * - Q5：订单号如何生成？为什么不用UUID？
 *   A5：时间戳+随机数，格式：yyyyMMddHHmmss + 6位随机数。
 *       不用UUID因为：UUID太长（36位），不适合做订单号；
 *       时间戳可读性强，便于查询和排序。
 * 
 * 关联文件：
 * - mapper/OrdersMapper.java（订单表操作）
 * - mapper/OrderItemMapper.java（订单项表操作）
 * - mapper/InventoryMapper.java（库存表操作，扣减库存）
 * - config/RabbitMQConfig.java（消息队列配置）
 * - mq/consumer/MemberPointsConsumer.java（积分消费者）
 * 
 * 参考文档：
 * - 梳理项目.md 3.2 订单模块
 * - 项目难点讲解.txt 难点二：订单创建的事务一致性
 * ============================================================
 */

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.config.RabbitMQConfig;
import com.zmj.gbs_commerce_system.dto.OrderPaidMessage;
import com.zmj.gbs_commerce_system.entity.*;
import com.zmj.gbs_commerce_system.mapper.InventoryLogMapper;
import com.zmj.gbs_commerce_system.mapper.InventoryMapper;
import com.zmj.gbs_commerce_system.mapper.MemberMapper;
import com.zmj.gbs_commerce_system.mapper.OrderItemMapper;
import com.zmj.gbs_commerce_system.mapper.OrdersMapper;
import com.zmj.gbs_commerce_system.mapper.ProductMapper;
import com.zmj.gbs_commerce_system.service.OrderService;
import com.zmj.gbs_commerce_system.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    // 【订单-01-依赖注入】数据库Mapper
    @Autowired
    private OrdersMapper ordersMapper;         // 订单表

    @Autowired
    private OrderItemMapper orderItemMapper;   // 订单项表

    @Autowired
    private ProductMapper productMapper;       // 商品表（查询商品信息）

    @Autowired
    private InventoryMapper inventoryMapper;   // 库存表（扣减库存）

    @Autowired
    private InventoryLogMapper inventoryLogMapper; // 库存日志表（记录变动）

    @Autowired
    private MemberMapper memberMapper; // 会员表（查询会员信息）

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedissonClient redissonClient;     // RabbitMQ（发送消息）

    // 【订单-01-分页查询】订单列表分页查询
    @Override
    public IPage<Orders> getOrdersWithPagination(Page<Orders> page, Map<String, Object> queryParams) {
        QueryWrapper<Orders> queryWrapper = new QueryWrapper<>();

        // 构建查询条件（动态筛选）
        if (queryParams != null && queryParams.containsKey("orderNo") && queryParams.get("orderNo") != null) {
            queryWrapper.like("order_no", queryParams.get("orderNo")); // 订单号模糊查询
        }

        if (queryParams != null && queryParams.containsKey("cashierId") && queryParams.get("cashierId") != null) {
            queryWrapper.eq("cashier_id", queryParams.get("cashierId")); // 收银员ID精确查询
        }

        if (queryParams != null && queryParams.containsKey("orderStatus") && queryParams.get("orderStatus") != null) {
            queryWrapper.eq("order_status", queryParams.get("orderStatus")); // 订单状态精确查询
        }

        if (queryParams != null && queryParams.containsKey("paymentMethod") && queryParams.get("paymentMethod") != null) {
            queryWrapper.eq("payment_method", queryParams.get("paymentMethod")); // 支付方式精确查询
        }

        if (queryParams != null && queryParams.containsKey("startTime") && queryParams.get("startTime") != null) {
            String startTime = queryParams.get("startTime").toString().trim();
            if (!startTime.isEmpty()) {
                queryWrapper.ge("create_time", startTime); // 创建时间 >= startTime
            }
        }
        if (queryParams != null && queryParams.containsKey("endTime") && queryParams.get("endTime") != null) {
            String endTime = queryParams.get("endTime").toString().trim();
            if (!endTime.isEmpty()) {
                queryWrapper.le("create_time", endTime); // 创建时间 <= endTime
            }
        }

        queryWrapper.orderByDesc("create_time"); // 按创建时间倒序排序

        return ordersMapper.selectPage(page, queryWrapper); // MyBatis-Plus分页查询
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

    // 【订单-01-核心方法】订单创建（事务控制）
    // @Transactional(rollbackFor = Exception.class)：任何异常都回滚
    // 面试考点：为什么要指定rollbackFor？
    // 答：默认只对RuntimeException回滚，指定Exception.class可以对所有异常回滚
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders createOrder(Map<String, Object> orderData) {
        // ========== 步骤1：获取购物车数据 ==========
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cartItems = (List<Map<String, Object>>) orderData.get("cartItems");

        // 验证购物车不为空
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("购物车为空"); // 抛异常触发事务回滚
        }

        // ========== 步骤2：验证商品状态和库存 ==========
        BigDecimal totalAmount = BigDecimal.ZERO; // 订单总金额

        for (Map<String, Object> item : cartItems) {
            Long productId = Long.valueOf(item.get("productId").toString());
            Integer quantity = Integer.valueOf(item.get("quantity").toString());

            // 验证商品是否存在且已上架
            Product product = productMapper.selectById(productId);
            if (product == null || product.getStatus() != 0) {
                // 面试考点：抛异常会触发事务回滚
                throw new RuntimeException("商品【ID:" + productId + "】已下架或不存在");
            }

            // 验证库存是否充足
            Inventory inventory = inventoryMapper.selectByProductId(productId);
            if (inventory == null || inventory.getStockQuantity() < quantity) {
                throw new RuntimeException("商品【" + product.getProductName() + "】库存不足！" +
                        "当前库存：" + (inventory != null ? inventory.getStockQuantity() : 0) +
                        "，需要：" + quantity);
            }

            // 计算商品小计
            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(quantity));
            totalAmount = totalAmount.add(itemTotal);

            // 暂存商品信息，后续创建订单项时使用
            item.put("product", product);
            item.put("itemTotal", itemTotal);
        }

        // ========== 步骤3：生成订单号 ==========
        // 面试考点：订单号为什么用时间戳+随机数？
        // 答：时间戳可读性强、便于排序；随机数防止重复
        String orderNo = generateOrderNo();

        // ========== 步骤4：创建订单对象 ==========
        Orders order = new Orders();
        order.setOrderNo(orderNo);               // 订单号
        order.setTotalAmount(totalAmount);       // 订单总金额

        BigDecimal paidAmount = new BigDecimal(orderData.get("paidAmount").toString());
        order.setPaidAmount(paidAmount);         // 实付金额

        BigDecimal discountAmount = totalAmount.subtract(paidAmount);
        order.setDiscountAmount(discountAmount); // 优惠金额 = 总金额 - 实付金额

        Integer paymentMethod = Integer.valueOf(orderData.get("paymentMethod").toString());
        order.setPaymentMethod(paymentMethod);   // 支付方式（1现金 2微信 3支付宝 5刷脸）

        order.setOrderStatus(1); // 订单状态：1已支付

        // 获取当前登录用户（收银员）
        // 面试考点：SecurityUtils如何获取当前用户？
        // 答：从Shiro的Subject中获取，Subject绑定在ThreadLocal中
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            order.setCashierId(currentUser.getId());
            order.setCashierName(
                    currentUser.getNickname() != null ? currentUser.getNickname() : currentUser.getUsername());
        }

        // 会员ID（可选）
        if (orderData.containsKey("memberId") && orderData.get("memberId") != null) {
            String memberId = orderData.get("memberId").toString();
            order.setMemberId(memberId);
            
            Member member = memberMapper.selectByMemberId(memberId);
            if (member != null) {
                order.setMemberName(member.getName());
            }
        }

        // 备注（可选）
        if (orderData.containsKey("remark")) {
            order.setRemark(orderData.get("remark").toString());
        }

        // ========== 步骤5：插入订单表 ==========
        ordersMapper.insert(order); // MyBatis-Plus自动生成SQL

        // ========== 步骤6：遍历商品，创建订单项 + 扣减库存 + 记录日志 ==========
        for (Map<String, Object> item : cartItems) {
            Long productId = Long.valueOf(item.get("productId").toString());
            Integer quantity = Integer.valueOf(item.get("quantity").toString());
            Product product = (Product) item.get("product"); // 从暂存中获取
            BigDecimal itemTotal = (BigDecimal) item.get("itemTotal");

            // 创建订单项
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());           // 关联订单ID
            orderItem.setProductId(productId);             // 商品ID
            orderItem.setProductName(product.getProductName()); // 商品名称（冗余存储，避免后续查询）
            orderItem.setProductCode(product.getProductCode());   // 商品编码
            orderItem.setPrice(product.getPrice());        // 商品单价
            orderItem.setQuantity(quantity);               // 购买数量
            orderItem.setSubtotal(itemTotal);              // 小计金额
            orderItemMapper.insert(orderItem);             // 插入订单项表

            // ========== 扣减库存（分布式锁 + 乐观锁双重保障）==========
            // 面试考点：如何防止并发超卖？
            // 答：使用双重锁机制：
            //     1. 分布式锁（宏观）：多实例并发控制
            //     2. 乐观锁（微观）：单实例内并发控制
            
            String lockKey = "lock:product:" + productId;
            RLock lock = redissonClient.getLock(lockKey);
            
            try {
                boolean locked = false;
                try {
                    locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("获取分布式锁被中断：" + e.getMessage());
                }
                if (!locked) {
                    throw new RuntimeException("商品【" + product.getProductName() + "】获取锁超时，请重试");
                }
                
                // 获取锁成功后，查询最新库存和版本号
                Inventory latestInventory = inventoryMapper.selectByProductId(productId);
                if (latestInventory == null || latestInventory.getStockQuantity() < quantity) {
                    throw new RuntimeException("商品【" + product.getProductName() + "】库存不足！");
                }
                
                // 使用乐观锁扣减库存
                int rows = inventoryMapper.decreaseStockWithOptimisticLock(
                    productId, quantity, latestInventory.getVersion());
                
                if (rows == 0) {
                    // 乐观锁冲突或库存不足，抛异常触发事务回滚
                    throw new RuntimeException("商品【" + product.getProductName() + "】库存扣减失败（并发冲突），请重试");
                }
                
                // 记录库存变动日志
                Inventory currentInventory = inventoryMapper.selectByProductId(productId);
                InventoryLog log = new InventoryLog();
                log.setProductId(productId);
                log.setChangeType(3);
                log.setChangeQuantity(-quantity);
                log.setBeforeQuantity(currentInventory.getStockQuantity() + quantity);
                log.setAfterQuantity(currentInventory.getStockQuantity());
                log.setOperator(order.getCashierName());
                log.setCreateTime(new Date());
                log.setRemark("订单：" + orderNo);
                inventoryLogMapper.insert(log);
                
            } finally {
                // 释放分布式锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        // ========== 步骤7：发送消息到RabbitMQ（异步处理会员积分） ==========
        sendOrderPaidMessage(order);

        log.info("订单创建成功: orderNo={}, totalAmount={}, paidAmount={}", orderNo, totalAmount, paidAmount);

        return order;
    }

    // 【订单-01-辅助方法】发送订单支付消息到RabbitMQ
    // 面试考点：为什么要发送消息？
    // 答：会员积分计算需要查数据库和更新，同步处理会拖慢支付响应。
    //     通过RabbitMQ异步解耦，支付响应时间从2秒降到200ms。
    private void sendOrderPaidMessage(Orders order) {
        try {
            OrderPaidMessage message = OrderPaidMessage.of(
                    order.getId(),
                    order.getOrderNo(),
                    order.getMemberId(),
                    order.getTotalAmount(),
                    order.getPaidAmount(),
                    order.getCashierId(),
                    order.getCashierName(),
                    order.getPaymentMethod()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_PAID_ROUTING_KEY,
                    message
            );

            log.info("订单支付消息已发送: orderNo={}", order.getOrderNo());
        } catch (Exception e) {
            log.error("发送订单支付消息失败: orderNo={}, error={}", order.getOrderNo(), e.getMessage());
        }
    }

    private String generateOrderNo() {
        String date = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String random = String.format("%06d", new Random().nextInt(999999));
        return date + random;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refundOrder(Long orderId) throws RuntimeException {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getOrderStatus() == 2) {
            throw new RuntimeException("订单已退款，无法重复退款");
        }

        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);

        for (OrderItem item : orderItems) {
            inventoryMapper.increaseStock(item.getProductId(), item.getQuantity());

            Inventory currentInventory = inventoryMapper.selectByProductId(item.getProductId());
            InventoryLog log = new InventoryLog();
            log.setProductId(item.getProductId());
            log.setChangeType(4);
            log.setChangeQuantity(item.getQuantity());
            log.setBeforeQuantity(currentInventory.getStockQuantity() - item.getQuantity());
            log.setAfterQuantity(currentInventory.getStockQuantity());
            log.setOperator(SecurityUtils.getCurrentUser().getNickname());
            log.setCreateTime(new Date());
            log.setRemark("订单退款：" + order.getOrderNo());
            inventoryLogMapper.insert(log);
        }

        order.setOrderStatus(2);
        ordersMapper.updateById(order);

        log.info("订单退款成功: orderNo={}", order.getOrderNo());

        return true;
    }
}
