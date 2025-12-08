/**
 * 商品分类管理API
 * 对应后端：ProductCategoryController
 */
import http from '../request';

/**
 * 获取分类树（树形结构）
 * GET /product/category/tree
 */
export const getCategoryTree = () => {
    return http.get('/product/category/tree');
};

/**
 * 获取所有分类列表（平铺列表）
 * GET /product/category/list
 */
export const getCategoryList = () => {
    return http.get('/product/category/list');
};

/**
 * 分页查询分类列表
 * POST /product/category/page
 */
export const getCategoryPage = (params) => {
    return http.post('/product/category/page', params);
};

/**
 * 根据ID获取分类信息
 * GET /product/category/{id}
 */
export const getCategoryById = (id) => {
    return http.get(`/product/category/${id}`);
};

/**
 * 新增分类
 * POST /product/category/add
 * 需要权限：product:category:add
 */
export const addCategory = (data) => {
    return http.post('/product/category/add', data);
};

/**
 * 更新分类
 * PUT /product/category/update/{id}
 * 需要权限：product:category:edit
 */
export const updateCategory = (id, data) => {
    return http.put(`/product/category/update/${id}`, data);
};

/**
 * 删除分类
 * DELETE /product/category/delete/{id}
 * 需要权限：product:category:delete
 */
export const deleteCategory = (id) => {
    return http.delete(`/product/category/delete/${id}`);
};

/**
 * 批量删除分类
 * DELETE /product/category/batchDelete
 * 需要权限：product:category:delete
 */
export const batchDeleteCategory = (ids) => {
    return http.delete('/product/category/batchDelete', { data: ids });
};

