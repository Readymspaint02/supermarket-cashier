package com.zmj.gbs_commerce_system.service;

import com.zmj.gbs_commerce_system.entity.Menu;
import java.util.List;

public interface MenuService {
    List<Menu> selectMenuTreeByUserId(Long userId);
    List<Menu> selectAllMenus();
    
    // 菜单管理相关方法
    Menu getMenuById(Long id);
    boolean insertMenu(Menu menu);
    boolean updateMenu(Menu menu);
    boolean deleteMenuById(Long id);
}