package com.zmj.gbs_commerce_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Inventory;
import com.zmj.gbs_commerce_system.entity.InventoryLog;
import com.zmj.gbs_commerce_system.entity.Product;
import com.zmj.gbs_commerce_system.mapper.InventoryLogMapper;
import com.zmj.gbs_commerce_system.mapper.InventoryMapper;
import com.zmj.gbs_commerce_system.mapper.ProductMapper;
import com.zmj.gbs_commerce_system.service.InventoryService;
import com.zmj.gbs_commerce_system.vo.InventoryLogVO;
import com.zmj.gbs_commerce_system.vo.InventoryVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存管理 Service 实现类
 * 
 * 实现库存管理的业务逻辑
 * 
 * 重要提示：
 * - 所有库存变动操作必须使用 @Transactional 保证事务一致性
 * - 库存扣减使用乐观锁防止超卖
 * - 每次库存变动都要记录日志
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public InventoryVO getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryMapper.selectByProductId(productId);
        if (inventory == null) {
            return null;
        }
        return convertToVO(inventory);
    }

    @Override
    public IPage<InventoryVO> getInventoriesWithPagination(Page<Inventory> page, Map<String, Object> queryParams) {
        QueryWrapper<Inventory> queryWrapper = new QueryWrapper<>();

        // 只查询有预警的库存
        if (queryParams != null && queryParams.containsKey("warning") &&
                Boolean.TRUE.equals(queryParams.get("warning"))) {
            queryWrapper.apply("stock_quantity <= warning_quantity");
        }

        // 按更新时间倒序
        queryWrapper.orderByDesc("update_time");

        IPage<Inventory> inventoryPage = inventoryMapper.selectPage(page, queryWrapper);

        // 转换为 VO（包含商品信息）
        Page<InventoryVO> voPage = new Page<>(inventoryPage.getCurrent(), inventoryPage.getSize(),
                inventoryPage.getTotal());
        List<InventoryVO> voList = inventoryPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public List<InventoryVO> getWarningInventories() {
        QueryWrapper<Inventory> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("stock_quantity <= warning_quantity");
        queryWrapper.orderByAsc("stock_quantity");
        List<Inventory> inventoryList = inventoryMapper.selectList(queryWrapper);

        // 转换为 VO（包含商品信息）
        return inventoryList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 商品入库
     * 
     * 业务流程：
     * 1. 查询当前库存
     * 2. 更新库存数量
     * 3. 记录变动日志
     */
    @Override
    @Transactional
    public boolean stockIn(Long productId, Integer quantity, String operator, String remark) {
        // 1. 查询当前库存
        Inventory inventory = inventoryMapper.selectByProductId(productId);
        if (inventory == null) {
            throw new RuntimeException("商品库存记录不存在");
        }

        int beforeQuantity = inventory.getStockQuantity();
        int afterQuantity = beforeQuantity + quantity;

        // 2. 更新库存
        inventory.setStockQuantity(afterQuantity);
        inventoryMapper.updateById(inventory);

        // 3. 记录变动日志
        InventoryLog log = new InventoryLog();
        log.setProductId(productId);
        log.setChangeType(1); // 1-入库
        log.setChangeQuantity(quantity);
        log.setBeforeQuantity(beforeQuantity);
        log.setAfterQuantity(afterQuantity);
        log.setOperator(operator);
        log.setCreateTime(new Date());
        log.setRemark(remark);
        inventoryLogMapper.insert(log);

        return true;
    }

    /**
     * 商品出库
     * 
     * 业务流程：
     * 1. 查询当前库存
     * 2. 检查库存是否充足
     * 3. 更新库存数量
     * 4. 记录变动日志
     */
    @Override
    @Transactional
    public boolean stockOut(Long productId, Integer quantity, String operator, String remark) throws RuntimeException {
        // 1. 查询当前库存
        Inventory inventory = inventoryMapper.selectByProductId(productId);
        if (inventory == null) {
            throw new RuntimeException("商品库存记录不存在");
        }

        int beforeQuantity = inventory.getStockQuantity();

        // 2. 检查库存是否充足
        if (beforeQuantity < quantity) {
            throw new RuntimeException("库存不足！当前库存：" + beforeQuantity + "，需要：" + quantity);
        }

        int afterQuantity = beforeQuantity - quantity;

        // 3. 更新库存
        inventory.setStockQuantity(afterQuantity);
        inventoryMapper.updateById(inventory);

        // 4. 记录变动日志
        InventoryLog log = new InventoryLog();
        log.setProductId(productId);
        log.setChangeType(2); // 2-出库
        log.setChangeQuantity(-quantity); // 负数表示减少
        log.setBeforeQuantity(beforeQuantity);
        log.setAfterQuantity(afterQuantity);
        log.setOperator(operator);
        log.setCreateTime(new Date());
        log.setRemark(remark);
        inventoryLogMapper.insert(log);

        return true;
    }

    /**
     * 扣减库存（使用乐观锁）
     * 
     * 用于收银结账时扣减库存
     * 使用乐观锁SQL：UPDATE ... WHERE stock_quantity >= ?
     * 
     * @return true-成功，false-失败（库存不足）
     */
    @Override
    @Transactional
    public boolean decreaseStock(Long productId, Integer quantity) {
        int rows = inventoryMapper.decreaseStock(productId, quantity);
        return rows > 0;
    }

    /**
     * 增加库存
     * 
     * 用于退货时增加库存
     */
    @Override
    @Transactional
    public boolean increaseStock(Long productId, Integer quantity) {
        int rows = inventoryMapper.increaseStock(productId, quantity);
        return rows > 0;
    }

    @Override
    public IPage<InventoryLogVO> getInventoryLogsWithPagination(Page<InventoryLog> page,
            Map<String, Object> queryParams) {
        QueryWrapper<InventoryLog> queryWrapper = new QueryWrapper<>();

        // 按商品ID查询
        if (queryParams != null && queryParams.containsKey("productId") && queryParams.get("productId") != null) {
            queryWrapper.eq("product_id", queryParams.get("productId"));
        }

        // 按变动类型查询
        if (queryParams != null && queryParams.containsKey("changeType") && queryParams.get("changeType") != null) {
            queryWrapper.eq("change_type", queryParams.get("changeType"));
        }

        // 按操作人查询
        if (queryParams != null && queryParams.containsKey("operator") && queryParams.get("operator") != null) {
            queryWrapper.like("operator", queryParams.get("operator"));
        }

        // 按时间倒序
        queryWrapper.orderByDesc("create_time");

        IPage<InventoryLog> logPage = inventoryLogMapper.selectPage(page, queryWrapper);

        // 转换为 VO（包含商品信息）
        Page<InventoryLogVO> voPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        List<InventoryLogVO> voList = logPage.getRecords().stream()
                .map(this::convertLogToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 将 Inventory 实体转换为 InventoryVO
     * 
     * 转换过程：
     * 1. 复制 Inventory 的所有属性到 VO
     * 2. 根据 productId 查询商品信息
     * 3. 将商品信息设置到 VO 中
     */
    private InventoryVO convertToVO(Inventory inventory) {
        InventoryVO vo = new InventoryVO();
        // 复制基本属性
        BeanUtils.copyProperties(inventory, vo);
        // 查询并设置商品信息
        Product product = productMapper.selectById(inventory.getProductId());
        vo.setProduct(product);
        return vo;
    }

    /**
     * 将 InventoryLog 实体转换为 InventoryLogVO
     * 
     * 转换过程：
     * 1. 复制 InventoryLog 的所有属性到 VO
     * 2. 根据 productId 查询商品信息
     * 3. 将商品信息设置到 VO 中
     */
    private InventoryLogVO convertLogToVO(InventoryLog log) {
        InventoryLogVO vo = new InventoryLogVO();
        // 复制基本属性
        BeanUtils.copyProperties(log, vo);
        // 查询并设置商品信息
        Product product = productMapper.selectById(log.getProductId());
        vo.setProduct(product);
        return vo;
    }
}
