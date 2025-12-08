package com.zmj.gbs_commerce_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品信息实体类
 * 对应数据库表：product
 * 
 * 功能说明：
 * 1. 存储商品的基本信息
 * 2. 关联商品分类（category_id）
 * 3. 包含价格、图片、描述等信息
 */
@Data
@TableName("product")
public class Product {
    
    /**
     * 商品ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 商品名称
     * 例如：可口可乐 330ml
     */
    private String productName;
    
    /**
     * 商品编码/条形码（唯一）
     * 例如：6901668000015
     * 用于收银时扫描
     */
    private String productCode;

    /**
     * 商品条码（可与商品编码不同）
     */
    private String barcode;
    
    /**
     * 分类ID
     * 关联 product_category 表
     */
    private Long categoryId;
    
    /**
     * 售价（单位：元）
     * 使用 BigDecimal 保证精度
     */
    private BigDecimal price;
    
    /**
     * 成本价（单位：元）
     * 用于计算利润
     */
    private BigDecimal costPrice;
    
    /**
     * 单位
     * 例如：瓶、包、个、kg
     */
    private String unit;
    
    /**
     * 商品图片文件名
     * 例如：1730012345678.jpg
     * 完整URL：/uploads/1730012345678.jpg
     */
    private String productImage;
    
    /**
     * 商品描述
     */
    private String description;
    
    /**
     * 状态
     * 0-正常，1-下架
     */
    private Integer status;
    
    /**
     * 创建者
     */
    private String createBy;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    
    /**
     * 更新者
     */
    private String updateBy;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    
    /**
     * 备注
     */
    private String remark;
}

