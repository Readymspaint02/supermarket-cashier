package com.zmj.gbs_commerce_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmj.gbs_commerce_system.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 商品库存 Mapper 接口
 * 
 * 提供库存相关的数据库操作
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 根据商品ID查询库存
     * 
     * @param productId 商品ID
     * @return 库存信息
     */
    @Select("SELECT * FROM inventory WHERE product_id = #{productId}")
    Inventory selectByProductId(Long productId);

    /**
     * 扣减库存（使用乐观锁）
     * 只有当库存充足时才会执行成功
     * 
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return 影响的行数（1-成功，0-失败）
     */
    @Update("UPDATE inventory SET stock_quantity = stock_quantity - #{quantity} " +
            "WHERE product_id = #{productId} AND stock_quantity >= #{quantity}")
    int decreaseStock(Long productId, Integer quantity);

    /**
     * 增加库存
     * 
     * @param productId 商品ID
     * @param quantity  增加数量
     * @return 影响的行数
     */
    @Update("UPDATE inventory SET stock_quantity = stock_quantity + #{quantity} " +
            "WHERE product_id = #{productId}")
    int increaseStock(Long productId, Integer quantity);
}
