package com.zmj.gbs_commerce_system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zmj.gbs_commerce_system.entity.Product;
import lombok.Data;

import java.util.Date;

/**
 * 库存变动记录视图对象（View Object）
 * 
 * 用途：给前端展示库存变动记录时，需要同时显示商品的详细信息
 * 
 * 为什么需要VO？
 * - 数据库中 inventory_log 表只存储 product_id
 * - 前端需要显示商品名称、商品编码等信息
 * - 通过 VO 将 InventoryLog 和 Product 组合在一起返回
 * 
 * 使用场景：
 * 1. 库存变动记录列表（入库记录、出库记录）
 * 2. 库存流水查询
 */
@Data
public class InventoryLogVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 变动类型
     * 1-入库，2-出库，3-销售，4-退货，5-盘点
     */
    private Integer changeType;

    /**
     * 变动数量
     * 正数表示增加，负数表示减少
     */
    private Integer changeQuantity;

    /**
     * 变动前数量
     */
    private Integer beforeQuantity;

    /**
     * 变动后数量
     */
    private Integer afterQuantity;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 关联的商品信息（完整的 Product 对象）
     * 
     * 前端可以通过 product.productName 获取商品名称
     * 前端可以通过 product.productCode 获取商品编码
     */
    private Product product;
}
