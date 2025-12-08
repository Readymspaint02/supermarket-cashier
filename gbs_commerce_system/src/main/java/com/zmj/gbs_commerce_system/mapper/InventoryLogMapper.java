package com.zmj.gbs_commerce_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmj.gbs_commerce_system.entity.InventoryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存变动记录 Mapper 接口
 * 
 * 提供库存变动记录相关的数据库操作
 * 主要用于记录和查询库存变动历史
 */
@Mapper
public interface InventoryLogMapper extends BaseMapper<InventoryLog> {
    // 使用 MyBatis Plus 提供的基础方法即可
    // 如需复杂查询，可在 Service 层使用 QueryWrapper
}
