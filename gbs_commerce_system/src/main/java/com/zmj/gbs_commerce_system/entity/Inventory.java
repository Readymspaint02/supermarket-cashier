package com.zmj.gbs_commerce_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
 * 4. 支持乐观锁（version字段）防止并发超卖
 * 
 * 面试考点：
 * - Q1：乐观锁如何实现？
 *   A1：MyBatis-Plus通过@Version注解自动处理乐观锁。
 *       更新时会自动校验version，version不匹配则更新失败。
 *       SQL示例：UPDATE inventory SET stock=?, version=version+1 
 *                WHERE id=? AND version=?
 * 
 * - Q2：乐观锁适合什么场景？
 *   A2：读多写少、冲突概率低的场景。超市收银系统中，
 *       同一商品同时被多人购买的概率较低，乐观锁效率高。
 * 
 * - Q3：乐观锁 vs 悲观锁？
 *   A3：- 乐观锁：不加锁，更新时校验版本号，冲突则重试
 *       - 悲观锁：加锁（如SELECT FOR UPDATE），阻塞其他操作
 *       乐观锁无阻塞性能高，悲观锁强一致性但性能低。
 */
@Data
@TableName("inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Integer stockQuantity;

    private Integer warningQuantity;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @Version
    private Integer version;
}
