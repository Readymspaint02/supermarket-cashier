package com.zmj.gbs_commerce_system.service.impl;

import com.zmj.gbs_commerce_system.entity.Menu;
import com.zmj.gbs_commerce_system.mapper.MenuMapper;
import com.zmj.gbs_commerce_system.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public List<Menu> selectMenuTreeByUserId(Long userId) {
        List<Menu> menus = menuMapper.selectMenusByUserId(userId);
        return buildMenuTree(menus, 0L);
    }

    @Override
    public List<Menu> selectAllMenus() {
        List<Menu> menus = menuMapper.selectList(null);
        return buildMenuTree(menus, 0L);
    }


    @Override
    public Menu getMenuById(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    public boolean insertMenu(Menu menu) {
        int result = menuMapper.insert(menu);
        return result > 0;
    }

    @Override
    public boolean updateMenu(Menu menu) {
        int result = menuMapper.updateById(menu);
        return result > 0;
    }

    @Override
    public boolean deleteMenuById(Long id) {
        menuMapper.deleteRoleMenuByRoleId( id);
        int result = menuMapper.deleteById(id);
        return result > 0;
    }

    private List<Menu> buildMenuTree(List<Menu> menus, Long parentId) {
        List<Menu> menuTree = new ArrayList<>();
        for (Menu menu : menus) {
            if (parentId.equals(menu.getParentId())) {
                List<Menu> children = buildMenuTree(menus, menu.getId());
                menu.setChildren(children);
                menuTree.add(menu);
            }
        }
        return menuTree;
    }
}