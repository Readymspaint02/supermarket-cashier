package com.zmj.gbs_commerce_system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Role;
import com.zmj.gbs_commerce_system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/role")
@Tag(name = "角色管理接口")
public class RoleController {
    
    @Autowired
    private RoleService roleService;
    
    @GetMapping("/all")
    @Operation(summary = "获取所有角色列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getAllRoles() {
        List<Role> roles = roleService.findAllRoles();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", roles);
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> getRoles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String roleName) {

        Page<Role> rolePage = new Page<>(page, size);
        IPage<Role> resultPage = roleService.findRoles(rolePage, roleName);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", resultPage.getRecords());
        result.put("total", resultPage.getTotal());

        return result;
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取角色信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "角色不存在")
    })
    public Map<String, Object> getRoleById(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        Role role = roleService.findRoleById(id);
        Map<String, Object> result = new HashMap<>();
        if (role != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", role);
        } else {
            result.put("code", 500);
            result.put("msg", "角色不存在");
        }
        return result;
    }
    
    @PostMapping("/add")
    @Operation(summary = "新增角色")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "500", description = "新增失败")
    })
    public Map<String, Object> addRole(
            @Parameter(description = "角色信息") @Valid @RequestBody Role role) {
        role.setCreateTime(new Date());
        role.setUpdateTime(new Date());

        boolean success = roleService.saveRole(role);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "添加成功");
        } else {
            result.put("code", 500);
            result.put("msg", "添加失败");
        }
        return result;
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "500", description = "更新失败")
    })
    public Map<String, Object> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        role.setUpdateTime(new Date());

        boolean success = roleService.updateRole(role);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "更新成功");
        } else {
            result.put("code", 500);
            result.put("msg", "更新失败");
        }
        return result;
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public Map<String, Object> deleteRole(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        boolean success = roleService.deleteRoleById(id);
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
    
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除角色")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public Map<String, Object> deleteRoles(
            @Parameter(description = "角色ID列表") @RequestBody List<Long> ids) {
        boolean success = roleService.deleteRolesByIds(ids);
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
    /**
     * 根据角色ID获取菜单ID列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    @GetMapping("/{roleId}/menus")
    public Map<String, Object> getMenuIdsByRoleId(@PathVariable Long roleId) {
        List<Long> menuIds = roleService.selectMenuIdsByRoleId(roleId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", menuIds);
        return result;
    }

    /**
     * 更新角色菜单关联关系
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 操作结果
     */
    @PutMapping("/{roleId}/menus")
    public Map<String, Object> updateRoleMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        boolean success = roleService.updateRoleMenus(roleId, menuIds);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "更新成功");
        } else {
            result.put("code", 500);
            result.put("msg", "更新失败");
        }
        return result;
    }
}