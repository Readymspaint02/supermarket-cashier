/**
 * 订单管理API（收银管理）
 * 对应后端：OrderController
 */
import http from '../request';

/**
 * 分页查询订单列表
 * POST /order/page
 * 需要权限：cashier:order:list
 */
export const getOrderPage = (params) => {
    return http.post('/order/page', params);
};

/**
 * 根据ID查询订单
 * GET /order/{id}
 * 需要权限：cashier:order:detail
 */
export const getOrderById = (id) => {
    return http.get(`/order/${id}`);
};

/**
 * 根据订单号查询订单
 * GET /order/no/{orderNo}
 * 需要权限：cashier:order:detail
 */
export const getOrderByOrderNo = (orderNo) => {
    return http.get(`/order/no/${orderNo}`);
};

/**
 * 根据订单ID查询订单明细
 * GET /order/{orderId}/items
 * 需要权限：cashier:order:detail
 */
export const getOrderItems = (orderId) => {
    return http.get(`/order/${orderId}/items`);
};

/**
 * 创建订单（收银结账）⭐核心功能
 * POST /order/checkout
 * 需要权限：cashier:checkout
 * 
 * 请求体示例：
 * {
 *   "cartItems": [
 *     { "productId": 1, "quantity": 2 }
 *   ],
 *   "paidAmount": 6.00,
 *   "discountAmount": 0.00,
 *   "paymentMethod": 1,
 *   "remark": "备注"
 * }
 */
export const checkout = (data) => {
    return http.post('/order/checkout', data);
};

/**
 * 订单退款
 * POST /order/{orderId}/refund
 * 需要权限：cashier:order:refund
 */
export const refundOrder = (orderId) => {
    return http.post(`/order/${orderId}/refund`);
};

