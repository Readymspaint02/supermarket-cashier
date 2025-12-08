package com.zmj.gbs_commerce_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmj.gbs_commerce_system.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    
    /**
     * 根据用户ID查询角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Role> selectRolesByUserId(Long userId);
    /**
     * 查询所有角色
     * @return 角色列表
     */
    @Select("SELECT * FROM sys_role WHERE status = 0 ORDER BY role_sort")
    List<Role> selectAllRoles();
    
    /**
     * 根据角色ID删除角色菜单关联
     * @param roleId 角色ID
     */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteRoleMenuByRoleId(Long roleId);

    /**
     * 根据角色ID查询菜单列表
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(Long roleId);

    /**
     * 批量插入角色与菜单的关联关系
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 插入记录数
     */
    @Insert("<script>" +
            "INSERT INTO sys_role_menu (role_id, menu_id) VALUES " +
            "<foreach collection='menuIds' item='menuId' separator=','>" +
            "(#{roleId}, #{menuId})" +
            "</foreach>" +
            "</script>")
    int insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

}