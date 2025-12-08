package com.zmj.gbs_commerce_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细实体类
 * 对应数据库表：order_item
 * 
 * 功能说明：
 * 1. 记录订单中的每个商品信息
 * 2. 一个订单可以有多个明细
 * 3. 冗余商品信息，防止商品信息变更
 */
@Data
@TableName("order_item")
public class OrderItem {
    
    /**
     * 订单明细ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 订单ID
     * 关联 orders 表
     */
    private Long orderId;
    
    /**
     * 商品ID
     * 关联 product 表
     */
    private Long productId;
    
    /**
     * 商品名称（冗余）
     * 即使商品被删除，也能查看历史订单
     */
    private String productName;
    
    /**
     * 商品编码（冗余）
     * 用于追溯
     */
    private String productCode;
    
    /**
     * 商品单价（冗余）
     * 记录当时的价格，即使商品价格变动也不影响
     */
    private BigDecimal price;
    
    /**
     * 购买数量
     */
    private Integer quantity;
    
    /**
     * 小计
     * 小计 = 单价 × 数量
     */
    private BigDecimal subtotal;
}

