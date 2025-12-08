package com.zmj.gbs_commerce_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 商品分类实体类
 * 对应数据库表：product_category
 * 
 * 功能说明：
 * 1. 支持树形结构（parent_id）
 * 2. 一个分类可以有多个子分类
 * 3. 用于商品的分类管理
 */
@Data
@TableName("product_category")
public class ProductCategory {
    
    /**
     * 分类ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 分类名称
     * 例如：食品饮料、日用百货、数码电器
     */
    private String categoryName;
    
    /**
     * 父分类ID
     * 0 表示顶级分类
     * 其他值表示该分类的父分类ID
     */
    private Long parentId;
    
    /**
     * 排序顺序
     * 数字越小越靠前
     */
    private Integer sortOrder;
    
    /**
     * 状态
     * 0-正常，1-停用
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
    
    /**
     * 子分类列表（非数据库字段）
     * 用于构建树形结构时使用
     */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<ProductCategory> children;
}

