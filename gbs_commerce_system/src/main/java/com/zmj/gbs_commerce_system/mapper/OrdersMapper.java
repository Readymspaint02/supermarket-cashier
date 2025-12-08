package com.zmj.gbs_commerce_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmj.gbs_commerce_system.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 订单 Mapper 接口
 * 
 * 提供订单相关的数据库操作
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
    
    /**
     * 根据订单号查询订单
     * 
     * @param orderNo 订单号
     * @return 订单信息
     */
    @Select("SELECT * FROM orders WHERE order_no = #{orderNo}")
    Orders selectByOrderNo(String orderNo);
}

