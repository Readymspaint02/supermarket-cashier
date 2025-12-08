package com.zmj.gbs_commerce_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmj.gbs_commerce_system.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品分类 Mapper 接口
 * 
 * 继承 MyBatis Plus 的 BaseMapper
 * 自动提供基础 CRUD 方法：
 * - insert(entity) 新增
 * - deleteById(id) 根据ID删除
 * - updateById(entity) 根据ID更新
 * - selectById(id) 根据ID查询
 * - selectList(wrapper) 条件查询列表
 * - selectPage(page, wrapper) 分页查询
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {

    /**
     * 查询指定分类下的所有子分类
     * 用于删除分类前检查是否有子分类
     * 
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @Select("SELECT * FROM product_category WHERE parent_id = #{parentId}")
    List<ProductCategory> selectByParentId(Long parentId);

    /**
     * 查询指定分类下的商品数量
     * 用于删除分类前检查是否有关联商品
     * 
     * @param categoryId 分类ID
     * @return 商品数量
     */
    @Select("SELECT COUNT(*) FROM product WHERE category_id = #{categoryId}")
    int countProductsByCategoryId(Long categoryId);
}
