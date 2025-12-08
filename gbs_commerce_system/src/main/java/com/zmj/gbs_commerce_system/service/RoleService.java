package com.zmj.gbs_commerce_system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Role;

import java.util.List;

public interface RoleService {
    
    /**
     * 查询所有角色
     * @return 角色列表
     */
    List<Role> findAllRoles();
    
    /**
     * 分页查询角色
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return 角色分页数据
     */
    IPage<Role> findRoles(Page<Role> page, String roleName);
    /**
     * 根据ID查询角色
     * @param id 角色ID
     * @return 角色信息
     */
    Role findRoleById(Long id);
    
    /**
     * 保存角色
     * @param role 角色信息
     * @return 是否保存成功
     */
    boolean saveRole(Role role);
    
    /**
     * 更新角色
     * @param role 角色信息
     * @return 是否更新成功
     */
    boolean updateRole(Role role);
    
    /**
     * 删除角色
     * @param id 角色ID
     * @return 是否删除成功
     */
    boolean deleteRoleById(Long id);
    
    /**
     * 批量删除角色
     * @param ids 角色ID列表
     * @return 是否删除成功
     */
    boolean deleteRolesByIds(List<Long> ids);
    /**
     * 根据角色ID查询菜单ID列表
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> selectMenuIdsByRoleId(Long roleId);

    /**
     * 更新角色菜单关联关系
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 是否更新成功
     */
    boolean updateRoleMenus(Long roleId, List<Long> menuIds);
}