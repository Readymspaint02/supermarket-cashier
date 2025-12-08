package com.zmj.gbs_commerce_system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zmj.gbs_commerce_system.entity.Product;
import lombok.Data;

import java.util.Date;

/**
 * 库存信息视图对象（View Object）
 * 
 * 用途：给前端展示库存信息时，需要同时显示商品的详细信息
 * 
 * 为什么需要VO？
 * - 数据库中 inventory 表只存储 product_id
 * - 前端需要显示商品名称、商品编码等信息
 * - 通过 VO 将 Inventory 和 Product 组合在一起返回
 * 
 * 使用场景：
 * 1. 库存查询列表
 * 2. 库存预警列表
 * 3. 库存详情展示
 */
@Data
public class InventoryVO {

    /**
     * 库存ID
     */
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 库存数量
     */
    private Integer stockQuantity;

    /**
     * 预警数量
     */
    private Integer warningQuantity;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 关联的商品信息（完整的 Product 对象）
     * 
     * 前端可以通过 product.productName 获取商品名称
     * 前端可以通过 product.productCode 获取商品编码
     * 前端可以通过 product.productImage 获取商品图片
     */
    private Product product;
}
