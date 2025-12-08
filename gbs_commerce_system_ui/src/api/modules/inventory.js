/**
 * 库存管理API
 * 对应后端：InventoryController
 */
import http from '../request';

/**
 * 根据商品ID查询库存
 * GET /inventory/product/{productId}
 */
export const getInventoryByProductId = (productId) => {
    return http.get(`/inventory/product/${productId}`);
};

/**
 * 分页查询库存列表
 * POST /inventory/page
 */
export const getInventoryPage = (params) => {
    return http.post('/inventory/page', params);
};

/**
 * 查询库存预警列表
 * GET /inventory/warning
 */
export const getWarningInventories = () => {
    return http.get('/inventory/warning');
};

/**
 * 商品入库
 * POST /inventory/in
 * 需要权限：inventory:in:add
 */
export const stockIn = (data) => {
    return http.post('/inventory/in', data);
};

/**
 * 商品出库
 * POST /inventory/out
 * 需要权限：inventory:out:add
 */
export const stockOut = (data) => {
    return http.post('/inventory/out', data);
};

/**
 * 分页查询库存变动记录
 * POST /inventory/log/page
 */
export const getInventoryLogPage = (params) => {
    return http.post('/inventory/log/page', params);
};

