package com.zmj.gbs_commerce_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体类
 * 对应数据库表：orders
 * 
 * 功能说明：
 * 1. 记录订单主要信息
 * 2. 关联订单明细（order_item）
 * 3. 记录收银员信息
 */
@Data
@TableName("orders")
public class Orders {

    /**
     * 订单ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号（唯一）
     * 格式：yyyyMMddHHmmss + 6位随机数
     * 例如：20251027153045123456
     */
    private String orderNo;

    /**
     * 订单总金额
     * 所有商品的价格总和
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
     * 实际支付的金额（可能有优惠）
     */
    private BigDecimal paidAmount;

    /**
     * 优惠金额
     * 优惠金额 = 总金额 - 实付金额
     */
    private BigDecimal discountAmount;

    /**
     * 支付方式
     * 1-现金，2-微信，3-支付宝，4-银行卡
     */
    private Integer paymentMethod;

    /**
     * 订单状态
     * 1-已完成，2-已退款，3-部分退款
     */
    private Integer orderStatus;

    /**
     * 收银员ID
     * 关联 sys_user 表
     */
    private Long cashierId;

    /**
     * 收银员姓名
     * 冗余字段，避免用户信息变更后查询不到
     */
    private String cashierName;

    /**
     * 创建时间（下单时间）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 备注
     */
    private String remark;
}
