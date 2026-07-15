package com.zmj.gbs_commerce_system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Product;

import java.util.List;
import java.util.Map;

/**
 * 商品信息 Service 接口
 * 
 * 定义商品管理的业务逻辑方法
 */
public interface ProductService {

    /**
     * 查询所有商品
     * 
     * @return 商品列表
     */
    List<Product> getAllProducts();

    /**
     * 分页查询商品
     * 支持按商品名称、商品编码、分类ID查询
     * 
     * @param page        分页对象
     * @param queryParams 查询参数
     * @return 分页结果
     */
    IPage<Product> getProductsWithPagination(Page<Product> page, Map<String, Object> queryParams);

    /**
     * 根据ID查询商品
     * 
     * @param id 商品ID
     * @return 商品信息
     */
    Product getProductById(Long id);

    /**
     * 根据商品编码查询商品
     * 用于收银时扫描条形码
     * 
     * @param productCode 商品编码
     * @return 商品信息
     */
    Product getProductByCode(String productCode);

    /**
     * 根据条码查询商品
     *
     * @param barcode 商品条码
     * @return 商品信息
     */
    Product getProductByBarcode(String barcode);

    /**
     * 新增商品
     * 同时会创建对应的库存记录
     * 
     * @param product 商品信息
     * @return 是否成功
     */
    boolean saveProduct(Product product);

    /**
     * 更新商品
     * 
     * @param product 商品信息
     * @return 是否成功
     */
    boolean updateProduct(Product product);

    /**
     * 删除商品
     * 注意：建议使用软删除（修改status）
     * 
     * @param id 商品ID
     * @return 是否成功
     */
    boolean deleteProductById(Long id);

    /**
     * 批量删除商品
     * 
     * @param ids 商品ID列表
     * @return 是否成功
     */
    boolean deleteBatchProducts(List<Long> ids);

    int regenerateAllBarcodes();
}
