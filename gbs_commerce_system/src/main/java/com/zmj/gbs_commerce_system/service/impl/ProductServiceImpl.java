package com.zmj.gbs_commerce_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Inventory;
import com.zmj.gbs_commerce_system.entity.Product;
import com.zmj.gbs_commerce_system.mapper.InventoryMapper;
import com.zmj.gbs_commerce_system.mapper.ProductMapper;
import com.zmj.gbs_commerce_system.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 商品信息 Service 实现类
 * 
 * 实现商品管理的业务逻辑
 */
@Service
public class ProductServiceImpl implements ProductService {
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private InventoryMapper inventoryMapper;
    
    @Override
    public List<Product> getAllProducts() {
        return productMapper.selectList(new QueryWrapper<Product>().eq("status", 0));
    }
    
    @Override
    public IPage<Product> getProductsWithPagination(Page<Product> page, Map<String, Object> queryParams) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        
        // 按商品名称模糊查询
        if (queryParams != null && queryParams.containsKey("productName") && queryParams.get("productName") != null) {
            queryWrapper.like("product_name", queryParams.get("productName"));
        }
        
        // 按商品编码模糊查询
        if (queryParams != null && queryParams.containsKey("productCode") && queryParams.get("productCode") != null) {
            queryWrapper.like("product_code", queryParams.get("productCode"));
        }
        
        // 按分类ID查询
        if (queryParams != null && queryParams.containsKey("categoryId") && queryParams.get("categoryId") != null) {
            queryWrapper.eq("category_id", queryParams.get("categoryId"));
        }
        
        // 按状态查询（默认查询正常状态）
        if (queryParams != null && queryParams.containsKey("status")) {
            queryWrapper.eq("status", queryParams.get("status"));
        } else {
            queryWrapper.eq("status", 0);
        }
        
        // 按创建时间倒序
        queryWrapper.orderByDesc("create_time");
        
        return productMapper.selectPage(page, queryWrapper);
    }
    
    @Override
    public Product getProductById(Long id) {
        return productMapper.selectById(id);
    }
    
    @Override
    public Product getProductByCode(String productCode) {
        return productMapper.selectByProductCode(productCode);
    }

    @Override
    public Product getProductByBarcode(String barcode) {
        Product product = productMapper.selectByBarcode(barcode);
        if (product == null) {
            product = productMapper.selectByProductCode(barcode);
        }
        return product;
    }
    
    /**
     * 新增商品
     * 
     * 业务逻辑：
     * 1. 保存商品信息
     * 2. 创建对应的库存记录（初始库存为0）
     */
    @Override
    @Transactional
    public boolean saveProduct(Product product) {
        // 1. 保存商品信息
        if (productMapper.insert(product) > 0) {
            // 2. 创建库存记录
            Inventory inventory = new Inventory();
            inventory.setProductId(product.getId());
            inventory.setStockQuantity(0);  // 初始库存为0
            inventory.setWarningQuantity(10);  // 默认预警值10
            inventoryMapper.insert(inventory);
            return true;
        }
        return false;
    }
    
    @Override
    @Transactional
    public boolean updateProduct(Product product) {
        return productMapper.updateById(product) > 0;
    }
    
    /**
     * 删除商品
     * 
     * 注意：建议使用软删除（修改status=1）
     * 而不是物理删除，因为可能有历史订单关联
     */
    @Override
    @Transactional
    public boolean deleteProductById(Long id) {
        // 软删除：修改状态为1（下架）
        Product product = new Product();
        product.setId(id);
        product.setStatus(1);
        return productMapper.updateById(product) > 0;
    }
    
    @Override
    @Transactional
    public boolean deleteBatchProducts(List<Long> ids) {
        for (Long id : ids) {
            deleteProductById(id);
        }
        return true;
    }
}

