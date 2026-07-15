import http from "../request";

export const getAuthMenuList = async () => {
    const { code, data: authMenuList } = await http('/system/menu/list')
    if (code == 200) {
        const dashboardMenu = {
            id: 1,
            path: '/dashboard',
            menuName: '首页',
            component: 'views/dashboard/index',
            icon: 'HomeFilled',
            parentId: 0,
            children: []
        }
        const result = [dashboardMenu, ...authMenuList]
        localStorage.setItem('authMenuList', JSON.stringify(result))
        return result
    }
    return []
};
// 获取所有菜单列表（用于菜单管理）
export const getAllMenuList = async () => {
    return http('/system/menu/all', { method: 'GET' });
};
// 获取菜单详情
export const getMenuInfo = async (id) => {
    return http(`/system/menu/${id}`, { method: 'GET' });
};

// 创建菜单
export const createMenu = async (data) => {
    return http('/system/menu/add', { method: 'POST', data });
};

// 更新菜单
export const updateMenu = async (id, data) => {
    return http(`/system/menu/${id}`, { method: 'PUT', data });
};

// 删除菜单
export const deleteMenu = async (id) => {
    return http(`/system/menu/${id}`, { method: 'DELETE' });
};