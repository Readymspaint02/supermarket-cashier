package com.zmj.gbs_commerce_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Role;
import com.zmj.gbs_commerce_system.mapper.RoleMapper;
import com.zmj.gbs_commerce_system.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {
    
    @Autowired
    private RoleMapper roleMapper;
    
    @Override
    public List<Role> findAllRoles() {
        return roleMapper.selectAllRoles();
    }

    @Override
    public IPage<Role> findRoles(Page<Role> page, String roleName) {
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        if (roleName != null && !roleName.isEmpty()) {
            queryWrapper.like("role_name", roleName);
        }
        queryWrapper.orderByAsc("role_sort");
        return roleMapper.selectPage(page, queryWrapper);
    }
    
    @Override
    public Role findRoleById(Long id) {
        return roleMapper.selectById(id);
    }
    
    @Override
    @Transactional
    public boolean saveRole(Role role) {
        role.setCreateTime(new Date());
        int result = roleMapper.insert(role);
        return result > 0;
    }
    
    @Override
    @Transactional
    public boolean updateRole(Role role) {
        role.setUpdateTime(new Date());
        int result = roleMapper.updateById(role);
        
        return result > 0;
    }

    /**
     * 单个删除
     * @param id 角色ID
     * @return
     */
    @Override
    @Transactional
    public boolean deleteRoleById(Long id) {
        // 先删除角色菜单关联
        roleMapper.deleteRoleMenuByRoleId(id);
        // 再删除角色
        return roleMapper.deleteById(id) > 0;
    }

    /**
     * 批量删除
     * @param ids 角色ID列表
     * @return
     */
    @Override
    @Transactional
    public boolean deleteRolesByIds(List<Long> ids) {
        boolean success = true;
        for (Long id : ids) {
            // 先删除角色菜单关联
            roleMapper.deleteRoleMenuByRoleId(id);
            // 再删除角色
            if (roleMapper.deleteById(id) <= 0) {
                success = false;
            }
        }
        return success;
    }
    @Override
    public List<Long> selectMenuIdsByRoleId(Long roleId) {
        return roleMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    public boolean updateRoleMenus(Long roleId, List<Long> menuIds) {
        // 先删除原有的角色菜单关联关系
        roleMapper.deleteRoleMenuByRoleId(roleId);

        // 批量插入新的角色菜单关联关系
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMapper.insertRoleMenus(roleId, menuIds);
        }

        return true;
    }
}