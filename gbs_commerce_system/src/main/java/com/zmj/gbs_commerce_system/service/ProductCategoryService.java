package com.zmj.gbs_commerce_system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.ProductCategory;

import java.util.List;
import java.util.Map;

/**
 * 商品分类 Service 接口
 * 
 * 定义商品分类的业务逻辑方法
 */
public interface ProductCategoryService {

    /**
     * 查询所有分类（树形结构）
     * 返回父子关系的树形数据
     * 
     * @return 分类树
     */
    List<ProductCategory> getCategoryTree();

    /**
     * 查询所有分类（列表）
     * 
     * @return 分类列表
     */
    List<ProductCategory> getAllCategories();

    /**
     * 分页查询分类
     * 
     * @param page        分页对象
     * @param queryParams 查询参数
     * @return 分页结果
     */
    IPage<ProductCategory> getCategoriesWithPagination(Page<ProductCategory> page, Map<String, Object> queryParams);

    /**
     * 根据ID查询分类
     * 
     * @param id 分类ID
     * @return 分类信息
     */
    ProductCategory getCategoryById(Long id);

    /**
     * 新增分类
     * 
     * @param category 分类信息
     * @return 是否成功
     */
    boolean saveCategory(ProductCategory category);

    /**
     * 更新分类
     * 
     * @param category 分类信息
     * @return 是否成功
     */
    boolean updateCategory(ProductCategory category);

    /**
     * 删除分类
     * 删除前需要检查：
     * 1. 是否有子分类
     * 2. 是否有关联商品
     * 
     * @param id 分类ID
     * @return 是否成功
     * @throws RuntimeException 如果有子分类或关联商品
     */
    boolean deleteCategoryById(Long id) throws RuntimeException;

    /**
     * 批量删除分类
     * 
     * @param ids 分类ID列表
     * @return 是否成功
     * @throws RuntimeException 如果有子分类或关联商品
     */
    boolean deleteBatchCategories(List<Long> ids) throws RuntimeException;
}
