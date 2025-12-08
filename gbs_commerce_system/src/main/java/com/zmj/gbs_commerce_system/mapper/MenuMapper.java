package com.zmj.gbs_commerce_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmj.gbs_commerce_system.entity.Menu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 0 AND m.menu_type IN ('M', 'C') " +
            "ORDER BY m.parent_id, m.order_num")
    List<Menu> selectMenusByUserId(Long userId);

    @Select("SELECT DISTINCT m.perms FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 0 AND m.perms IS NOT NULL")
    List<String> selectPermsByUserId(Long userId);

    /**
     * 根据菜单ID删除角色菜单关联
     * @param menuId 角色ID
     */
    @Delete("DELETE FROM sys_role_menu WHERE menu_id = #{menuId}")
    void deleteRoleMenuByRoleId(Long menuId);
}