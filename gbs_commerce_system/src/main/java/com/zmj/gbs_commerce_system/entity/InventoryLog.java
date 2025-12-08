package com.zmj.gbs_commerce_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 库存变动记录实体类
 * 对应数据库表：inventory_log
 * 
 * 功能说明：
 * 1. 记录所有库存变动的历史
 * 2. 用于审计和追溯
 * 3. 记录变动前后的数量
 */
@Data
@TableName("inventory_log")
public class InventoryLog {

    /**
     * 记录ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID
     * 关联 product 表
     */
    private Long productId;

    /**
     * 变动类型
     * 1-入库，2-出库，3-销售，4-退货，5-盘点
     */
    private Integer changeType;

    /**
     * 变动数量
     * 正数表示增加（入库、退货）
     * 负数表示减少（出库、销售）
     */
    private Integer changeQuantity;

    /**
     * 变动前数量
     * 用于追溯和验证
     */
    private Integer beforeQuantity;

    /**
     * 变动后数量
     * 用于追溯和验证
     */
    private Integer afterQuantity;

    /**
     * 操作人
     * 记录是谁操作的
     */
    private String operator;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 备注
     * 例如：入库原因、出库去向等
     */
    private String remark;
}
