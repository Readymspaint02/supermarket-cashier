import http from "../request";

// 获取用户列表（超级管理员）
export const getUserList = async (params) => {
    return http('/system/user/list', { method: 'GET', params });
};
// 获取用户列表（超级管理员）
export const getUserPage = async (data) => {
    return http('/system/user/page', { method: 'POST', data });
};
// 获取当前用户信息（普通用户）
export const getCurrentUserInfo = async () => {
    return http('/system/user/info', { method: 'GET' });
};

// 创建用户（超级管理员）
export const createUser = async (data) => {
    return http('/system/user/add', { method: 'POST', data });
};

// 更新用户（超级管理员）
export const updateUser = async (id, data) => {
    return http(`/system/user/update/${id}`, { method: 'PUT', data });
};

// 删除用户（超级管理员）
export const deleteUser = async (id) => {
    return http(`/system/user/delete/${id}`, { method: 'DELETE' });
};
// 修改密码
export const changePassword = async (id, data) => {
    return http(`/system/user/${id}/password`, { method: 'POST', data });
};
//获取当前用户角色对应的权限列表
export const getUserPermissions = async () => {
    return http('/system/user/perms', { method: 'GET' });
};