package com.zmj.gbs_commerce_system.controller;

import com.zmj.gbs_commerce_system.entity.Menu;
import com.zmj.gbs_commerce_system.service.MenuService;
import com.zmj.gbs_commerce_system.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/menu")
@Tag(name = "菜单接口",description = "菜单管理接口")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping("/list")
    @Operation(summary = "获取菜单列表",description = "获取菜单列表")
    public Map<String, Object> getMenuList() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        List<Menu> menus = menuService.selectMenuTreeByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "成功");
        result.put("data", menus);
        return result;
    }
    
    /**
     * 获取菜单列表（所有菜单，用于菜单管理）
     *
     * @return 菜单列表
     */
    @GetMapping("/all")
    public Map<String, Object> getAllMenuList() {
        List<Menu> menus = menuService.selectAllMenus();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", menus);
        return result;
    }
    /**
     * 根据ID获取菜单详情
     *
     * @param id 菜单ID
     * @return 菜单信息
     */
    @GetMapping(value = "/{id}")
    public Map<String, Object> getMenuById(@PathVariable Long id) {
        Menu menu = menuService.getMenuById(id);
        Map<String, Object> result = new HashMap<>();
        if (menu != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", menu);
        } else {
            result.put("code", 404);
            result.put("msg", "菜单不存在");
        }
        return result;
    }
    
    /**
     * 新增菜单
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @PostMapping("/add")
    public Map<String, Object> addMenu(@RequestBody Menu menu) {
        boolean success = menuService.insertMenu(menu);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "新增成功");
        } else {
            result.put("code", 500);
            result.put("msg", "新增失败");
        }
        return result;
    }
    
    /**
     * 修改菜单
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateMenu(@RequestBody Menu menu) {
        boolean success = menuService.updateMenu(menu);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "修改成功");
        } else {
            result.put("code", 500);
            result.put("msg", "修改失败");
        }
        return result;
    }
    
    /**
     * 删除菜单
     *
     * @param id 菜单ID
     * @return 结果
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteMenu(@PathVariable Long id) {
        boolean success = menuService.deleteMenuById(id);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "删除成功");
        } else {
            result.put("code", 500);
            result.put("msg", "删除失败");
        }
        return result;
    }
}