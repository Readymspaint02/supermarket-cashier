package com.zmj.gbs_commerce_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmj.gbs_commerce_system.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细 Mapper 接口
 * 
 * 提供订单明细相关的数据库操作
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
    
    /**
     * 根据订单ID查询订单明细列表
     * 
     * @param orderId 订单ID
     * @return 订单明细列表
     */
    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItem> selectByOrderId(Long orderId);
}

