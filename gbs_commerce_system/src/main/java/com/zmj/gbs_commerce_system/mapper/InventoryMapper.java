package com.zmj.gbs_commerce_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmj.gbs_commerce_system.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    @Select("SELECT * FROM inventory WHERE product_id = #{productId}")
    Inventory selectByProductId(Long productId);

    @Update("UPDATE inventory SET stock_quantity = stock_quantity - #{quantity} " +
            "WHERE product_id = #{productId} AND stock_quantity >= #{quantity}")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE inventory SET stock_quantity = stock_quantity + #{quantity} " +
            "WHERE product_id = #{productId}")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE inventory SET stock_quantity = stock_quantity - #{quantity}, version = version + 1 " +
            "WHERE product_id = #{productId} AND stock_quantity >= #{quantity} AND version = #{version}")
    int decreaseStockWithOptimisticLock(@Param("productId") Long productId, 
                                         @Param("quantity") Integer quantity, 
                                         @Param("version") Integer version);
}
