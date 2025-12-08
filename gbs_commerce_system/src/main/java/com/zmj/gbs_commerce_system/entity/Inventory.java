package com.zmj.gbs_commerce_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 商品库存实体类
 * 对应数据库表：inventory
 * 
 * 功能说明：
 * 1. 记录每个商品的实时库存数量
 * 2. 一个商品对应一条库存记录（一对一）
 * 3. 支持库存预警
 */
@Data
@TableName("inventory")
public class Inventory {

    /**
     * 库存ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID（唯一）
     * 关联 product 表
     */
    private Long productId;

    /**
     * 库存数量
     * 实时库存，每次入库/出库/销售都会更新
     */
    private Integer stockQuantity;

    /**
     * 预警数量
     * 当库存数量 <= 预警数量时，提示补货
     * 默认值：10
     */
    private Integer warningQuantity;

    /**
     * 更新时间
     * 每次库存变动都会更新
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
