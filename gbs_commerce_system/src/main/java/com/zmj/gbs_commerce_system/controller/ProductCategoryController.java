package com.zmj.gbs_commerce_system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.ProductCategory;
import com.zmj.gbs_commerce_system.service.ProductCategoryService;
import com.zmj.gbs_commerce_system.utils.PageParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品分类管理 Controller
 * 
 * 提供商品分类的增删改查接口
 * 
 * 接口路径：/api/product/category/...
 */
@RestController
@RequestMapping("/product/category")
@Tag(name = "商品分类管理接口")
public class ProductCategoryController {

    @Autowired
    private ProductCategoryService categoryService;

    /**
     * 获取分类树
     * 返回树形结构的分类数据
     * 
     * 使用场景：
     * - 前端展示分类树
     * - 新增/编辑商品时选择分类
     */
    @GetMapping("/tree")
    @Operation(summary = "获取分类树")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getCategoryTree() {
        List<ProductCategory> tree = categoryService.getCategoryTree();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", tree);
        return result;
    }

    /**
     * 获取所有分类列表
     * 返回平铺的列表数据
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有分类列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getAllCategories() {
        List<ProductCategory> categories = categoryService.getAllCategories();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", categories);
        return result;
    }

    /**
     * 分页获取分类列表
     * 
     * 请求参数示例：
     * {
     * "pageNum": 1,
     * "pageSize": 10,
     * "queryParams": {
     * "categoryName": "食品", // 可选：按名称模糊查询
     * "parentId": 0 // 可选：按父分类查询
     * }
     * }
     */
    @PostMapping("/page")
    @Operation(summary = "分页获取分类列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getCategoriesWithPagination(
            @RequestBody PageParams pageParams) {
        Integer pageNum = pageParams.getPageNum() != null ? pageParams.getPageNum() : 1;
        Integer pageSize = pageParams.getPageSize() != null ? pageParams.getPageSize() : 10;
        Page<ProductCategory> page = new Page<>(pageNum, pageSize);
        IPage<ProductCategory> categoryPage = categoryService.getCategoriesWithPagination(page,
                pageParams.getQueryParams());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", categoryPage);
        return result;
    }

    /**
     * 根据ID获取分类信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取分类信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "分类不存在")
    })
    public Map<String, Object> getCategoryById(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        ProductCategory category = categoryService.getCategoryById(id);
        Map<String, Object> result = new HashMap<>();
        if (category != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", category);
        } else {
            result.put("code", 500);
            result.put("msg", "分类不存在");
        }
        return result;
    }

    /**
     * 新增分类
     * 
     * 权限要求：product:category:add
     * 
     * 请求体示例：
     * {
     * "categoryName": "零食",
     * "parentId": 1,
     * "sortOrder": 1,
     * "status": 0,
     * "remark": "备注"delete
     * }
     */
    @RequiresPermissions("product:category:add")
    @PostMapping("/add")
    @Operation(summary = "新增分类")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "500", description = "新增失败")
    })
    public Map<String, Object> addCategory(
            @Parameter(description = "分类信息") @RequestBody ProductCategory category) {
        boolean success = categoryService.saveCategory(category);
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
     * 更新分类
     * 
     * 权限要求：product:category:edit
     */
    @RequiresPermissions("product:category:edit")
    @PutMapping("/update/{id}")
    @Operation(summary = "更新分类信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "500", description = "更新失败")
    })
    public Map<String, Object> updateCategory(@PathVariable Long id,
            @Parameter(description = "分类信息") @RequestBody ProductCategory category) {
        category.setId(id);
        boolean success = categoryService.updateCategory(category);
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

    /**
     * 删除分类
     * 
     * 权限要求：product:category:delete
     * 
     * 业务规则：
     * - 如果有子分类，不能删除
     * - 如果有关联商品，不能删除
     */
    @RequiresPermissions("product:category:delete")
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除分类")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public Map<String, Object> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = categoryService.deleteCategoryById(id);
            if (success) {
                result.put("code", 200);
                result.put("msg", "删除成功");
            } else {
                result.put("code", 500);
                result.put("msg", "删除失败");
            }
        } catch (RuntimeException e) {
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * 批量删除分类
     * 
     * 权限要求：product:category:delete
     * 
     * 请求体示例：[1, 2, 3]
     */
    @RequiresPermissions("product:category:delete")
    @DeleteMapping("/batchDelete")
    @Operation(summary = "批量删除分类")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public Map<String, Object> batchDeleteCategories(
            @Parameter(description = "分类ID数组") @RequestBody Long[] ids) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = categoryService.deleteBatchCategories(Arrays.asList(ids));
            if (success) {
                result.put("code", 200);
                result.put("msg", "批量删除成功");
            } else {
                result.put("code", 500);
                result.put("msg", "批量删除失败");
            }
        } catch (RuntimeException e) {
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }
}
