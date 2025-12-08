package com.zmj.gbs_commerce_system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.OrderItem;
import com.zmj.gbs_commerce_system.entity.Orders;

import java.util.List;
import java.util.Map;

/**
 * 订单管理 Service 接口
 * 
 * 定义订单（收银）管理的业务逻辑方法
 */
public interface OrderService {

    /**
     * 分页查询订单
     * 支持按订单号、收银员、时间范围查询
     * 
     * @param page        分页对象
     * @param queryParams 查询参数
     * @return 分页结果
     */
    IPage<Orders> getOrdersWithPagination(Page<Orders> page, Map<String, Object> queryParams);

    /**
     * 根据ID查询订单
     * 
     * @param id 订单ID
     * @return 订单信息
     */
    Orders getOrderById(Long id);

    /**
     * 根据订单号查询订单
     * 
     * @param orderNo 订单号
     * @return 订单信息
     */
    Orders getOrderByOrderNo(String orderNo);

    /**
     * 根据订单ID查询订单明细
     * 
     * @param orderId 订单ID
     * @return 订单明细列表
     */
    List<OrderItem> getOrderItemsByOrderId(Long orderId);

    /**
     * 创建订单（收银结账）
     * 核心业务逻辑：
     * 1. 验证商品和库存
     * 2. 生成订单号
     * 3. 创建订单主表
     * 4. 创建订单明细
     * 5. 扣减库存（使用乐观锁）
     * 6. 记录库存变动日志
     * 
     * 注意：
     * - 使用事务保证数据一致性
     * - 使用乐观锁防止超卖
     * - 失败时自动回滚
     * 
     * @param orderData 订单数据（包含订单信息和商品列表）
     * @return 订单信息
     * @throws RuntimeException 库存不足或其他业务异常
     */
    Orders createOrder(Map<String, Object> orderData) throws RuntimeException;

    /**
     * 订单退款
     * 1. 更新订单状态
     * 2. 退回库存
     * 3. 记录库存变动日志
     * 
     * @param orderId 订单ID
     * @return 是否成功
     * @throws RuntimeException 业务异常
     */
    boolean refundOrder(Long orderId) throws RuntimeException;
}
