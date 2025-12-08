import http from "../request";

// 获取角色列表（超级管理员）
export const getRoleAll = async (params) => {
    return http('/system/role/all', { method: 'GET', params });
};

// 获取角色列表（管理员权限）分页
export const getRoleList = async () => {
    return http('/system/role/list', { method: 'GET' });
};
// 获取单个角色信息（管理员权限）
export const getRoleInfo = async (id) => {
    return http(`/system/role/${id}`, { method: 'GET' });
};

// 创建角色（管理员权限）
export const createRole = async (data) => {
    return http('/system/role/add', { method: 'POST', data });
};

// 更新角色（管理员权限）
export const updateRole = async (id, data) => {
    return http(`/system/role/${id}`, { method: 'PUT', data });
};

// 删除角色（管理员权限）
export const deleteRole = async (id) => {
    return http(`/system/role/${id}`, { method: 'DELETE' });
};
// 根据角色ID获取菜单ID列表
export const getRoleMenuIds = async (roleId) => {
    return http(`/system/role/${roleId}/menus`, { method: 'GET' });
};

// 更新角色菜单关联关系
export const updateRoleMenus = async (roleId, menuIds) => {
    return http(`/system/role/${roleId}/menus`, {
        method: 'PUT',
        data: menuIds,
        headers: {
            'Content-Type': 'application/json'
        }
    });
};