/**
 * 商品信息管理API
 * 对应后端：ProductController
 */
import http from '../request';

/**
 * 获取所有商品列表
 * GET /product/list
 */
export const getProductList = () => {
    return http.get('/product/list');
};

/**
 * 分页查询商品列表
 * POST /product/page
 */
export const getProductPage = (params) => {
    return http.post('/product/page', params);
};

/**
 * 根据ID获取商品信息
 * GET /product/{id}
 */
export const getProductById = (id) => {
    return http.get(`/product/${id}`);
};

/**
 * 根据商品编码查询商品（扫码功能）
 * GET /product/code/{productCode}
 */
export const getProductByCode = (productCode) => {
    return http.get(`/product/code/${productCode}`);
};

/**
 * 根据条码查询商品
 */
export const getProductByBarcode = (barcode) => {
    return http.get(`/product/barcode/${barcode}`);
};

/**
 * 上传商品图片
 * POST /product/uploadImage
 */
export const uploadProductImage = (formData) => {
    return http.post('/product/uploadImage', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
};

/**
 * 新增商品
 * POST /product/add
 * 需要权限：product:info:add
 */
export const addProduct = (data) => {
    return http.post('/product/add', data);
};

/**
 * 更新商品
 * PUT /product/update/{id}
 * 需要权限：product:info:edit
 */
export const updateProduct = (id, data) => {
    return http.put(`/product/update/${id}`, data);
};

/**
 * 删除商品
 * DELETE /product/delete/{id}
 * 需要权限：product:info:delete
 */
export const deleteProduct = (id) => {
    return http.delete(`/product/delete/${id}`);
};

/**
 * 批量删除商品
 * DELETE /product/batchDelete
 * 需要权限：product:info:delete
 */
export const batchDeleteProduct = (ids) => {
    return http.delete('/product/batchDelete', { data: ids });
};

