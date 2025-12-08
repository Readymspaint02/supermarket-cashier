package com.zmj.gbs_commerce_system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Inventory;
import com.zmj.gbs_commerce_system.entity.InventoryLog;
import com.zmj.gbs_commerce_system.vo.InventoryLogVO;
import com.zmj.gbs_commerce_system.vo.InventoryVO;

import java.util.List;
import java.util.Map;

/**
 * 库存管理 Service 接口
 * 
 * 定义库存管理的业务逻辑方法
 */
public interface InventoryService {

    /**
     * 根据商品ID查询库存
     * 
     * @param productId 商品ID
     * @return 库存信息（包含商品详细信息）
     */
    InventoryVO getInventoryByProductId(Long productId);

    /**
     * 分页查询库存
     * 支持库存预警筛选
     * 
     * @param page        分页对象
     * @param queryParams 查询参数
     * @return 分页结果（包含商品详细信息）
     */
    IPage<InventoryVO> getInventoriesWithPagination(Page<Inventory> page, Map<String, Object> queryParams);

    /**
     * 查询库存预警列表
     * 返回库存数量 <= 预警数量的商品
     * 
     * @return 预警列表（包含商品详细信息）
     */
    List<InventoryVO> getWarningInventories();

    /**
     * 商品入库
     * 1. 更新库存数量
     * 2. 记录变动日志
     * 
     * @param productId 商品ID
     * @param quantity  入库数量
     * @param operator  操作人
     * @param remark    备注
     * @return 是否成功
     */
    boolean stockIn(Long productId, Integer quantity, String operator, String remark);

    /**
     * 商品出库
     * 1. 检查库存是否充足
     * 2. 更新库存数量
     * 3. 记录变动日志
     * 
     * @param productId 商品ID
     * @param quantity  出库数量
     * @param operator  操作人
     * @param remark    备注
     * @return 是否成功
     * @throws RuntimeException 库存不足时抛出异常
     */
    boolean stockOut(Long productId, Integer quantity, String operator, String remark) throws RuntimeException;

    /**
     * 扣减库存（用于销售）
     * 使用乐观锁，防止超卖
     * 
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return 是否成功
     */
    boolean decreaseStock(Long productId, Integer quantity);

    /**
     * 增加库存（用于退货）
     * 
     * @param productId 商品ID
     * @param quantity  增加数量
     * @return 是否成功
     */
    boolean increaseStock(Long productId, Integer quantity);

    /**
     * 分页查询库存变动记录
     * 
     * @param page        分页对象
     * @param queryParams 查询参数
     * @return 分页结果（包含商品详细信息）
     */
    IPage<InventoryLogVO> getInventoryLogsWithPagination(Page<InventoryLog> page, Map<String, Object> queryParams);
}
