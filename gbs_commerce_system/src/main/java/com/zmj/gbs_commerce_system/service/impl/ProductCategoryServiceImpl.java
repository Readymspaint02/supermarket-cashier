package com.zmj.gbs_commerce_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.ProductCategory;
import com.zmj.gbs_commerce_system.mapper.ProductCategoryMapper;
import com.zmj.gbs_commerce_system.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分类 Service 实现类
 * 
 * 实现商品分类的业务逻辑
 */
@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {

    @Autowired
    private ProductCategoryMapper categoryMapper;

    /**
     * 查询所有分类并构建树形结构
     * 
     * 算法说明：
     * 1. 查询所有分类
     * 2. 找出所有顶级分类（parent_id = 0）
     * 3. 递归构建每个顶级分类的子树
     */
    @Override
    public List<ProductCategory> getCategoryTree() {
        // 1. 查询所有分类
        List<ProductCategory> allCategories = categoryMapper.selectList(null);

        // 2. 找出所有顶级分类
        List<ProductCategory> topCategories = allCategories.stream()
                .filter(category -> category.getParentId() == 0)
                .collect(Collectors.toList());

        // 3. 递归构建子树
        for (ProductCategory topCategory : topCategories) {
            topCategory.setChildren(getChildren(topCategory.getId(), allCategories));
        }

        return topCategories;
    }

    /**
     * 递归获取子分类
     * 
     * @param parentId      父分类ID
     * @param allCategories 所有分类列表
     * @return 子分类列表
     */
    private List<ProductCategory> getChildren(Long parentId, List<ProductCategory> allCategories) {
        List<ProductCategory> children = new ArrayList<>();

        for (ProductCategory category : allCategories) {
            if (category.getParentId().equals(parentId)) {
                // 递归获取子分类的子分类
                category.setChildren(getChildren(category.getId(), allCategories));
                children.add(category);
            }
        }

        return children;
    }

    @Override
    public List<ProductCategory> getAllCategories() {
        return categoryMapper.selectList(null);
    }

    @Override
    public IPage<ProductCategory> getCategoriesWithPagination(Page<ProductCategory> page,
            Map<String, Object> queryParams) {
        QueryWrapper<ProductCategory> queryWrapper = new QueryWrapper<>();

        // 按分类名称模糊查询
        if (queryParams != null && queryParams.containsKey("categoryName") && queryParams.get("categoryName") != null) {
            queryWrapper.like("category_name", queryParams.get("categoryName"));
        }

        // 按父分类ID查询
        if (queryParams != null && queryParams.containsKey("parentId") && queryParams.get("parentId") != null) {
            queryWrapper.eq("parent_id", queryParams.get("parentId"));
        }

        // 按排序字段排序
        queryWrapper.orderByAsc("sort_order", "id");

        return categoryMapper.selectPage(page, queryWrapper);
    }

    @Override
    public ProductCategory getCategoryById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    @Transactional
    public boolean saveCategory(ProductCategory category) {
        return categoryMapper.insert(category) > 0;
    }

    @Override
    @Transactional
    public boolean updateCategory(ProductCategory category) {
        return categoryMapper.updateById(category) > 0;
    }

    /**
     * 删除分类
     * 
     * 业务规则：
     * 1. 如果有子分类，不能删除
     * 2. 如果有关联商品，不能删除
     */
    @Override
    @Transactional
    public boolean deleteCategoryById(Long id) throws RuntimeException {
        // 1. 检查是否有子分类
        List<ProductCategory> children = categoryMapper.selectByParentId(id);
        if (children != null && !children.isEmpty()) {
            throw new RuntimeException("该分类下有子分类，无法删除");
        }

        // 2. 检查是否有关联商品
        int productCount = categoryMapper.countProductsByCategoryId(id);
        if (productCount > 0) {
            throw new RuntimeException("该分类下有" + productCount + "个商品，无法删除");
        }

        // 3. 执行删除
        return categoryMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean deleteBatchCategories(List<Long> ids) throws RuntimeException {
        // 逐个检查并删除
        for (Long id : ids) {
            deleteCategoryById(id);
        }
        return true;
    }
}
