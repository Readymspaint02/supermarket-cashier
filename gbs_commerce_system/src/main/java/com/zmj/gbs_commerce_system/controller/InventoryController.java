package com.zmj.gbs_commerce_system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Inventory;
import com.zmj.gbs_commerce_system.entity.InventoryLog;
import com.zmj.gbs_commerce_system.service.InventoryService;
import com.zmj.gbs_commerce_system.utils.PageParams;
import com.zmj.gbs_commerce_system.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 库存管理 Controller
 * 
 * 提供库存的入库、出库、查询接口
 * 
 * 接口路径：/api/inventory/...
 */
@RestController
@RequestMapping("/inventory")
@Tag(name = "库存管理接口")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    /**
     * 根据商品ID查询库存
     * 
     * 接口路径：GET /api/inventory/product/{productId}
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "根据商品ID查询库存（包含商品详细信息）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "库存记录不存在")
    })
    public Map<String, Object> getInventoryByProductId(
            @Parameter(description = "商品ID") @PathVariable Long productId) {
        // Service 返回 InventoryVO（包含商品信息）
        var inventoryVO = inventoryService.getInventoryByProductId(productId);
        Map<String, Object> result = new HashMap<>();
        if (inventoryVO != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", inventoryVO);
        } else {
            result.put("code", 500);
            result.put("msg", "库存记录不存在");
        }
        return result;
    }

    /**
     * 分页查询库存列表
     * 
     * 请求参数示例：
     * {
     * "pageNum": 1,
     * "pageSize": 10,
     * "queryParams": {
     * "warning": true // 可选：只查询库存预警的商品
     * }
     * }
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询库存列表（包含商品详细信息）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getInventoriesWithPagination(
            @RequestBody PageParams pageParams) {
        Integer pageNum = pageParams.getPageNum() != null ? pageParams.getPageNum() : 1;
        Integer pageSize = pageParams.getPageSize() != null ? pageParams.getPageSize() : 10;
        Page<Inventory> page = new Page<>(pageNum, pageSize);
        // Service 返回 IPage<InventoryVO>（包含商品信息）
        var inventoryPage = inventoryService.getInventoriesWithPagination(page,
                pageParams.getQueryParams());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", inventoryPage);
        return result;
    }

    /**
     * 查询库存预警列表
     * 
     * 返回库存数量 <= 预警数量的商品
     * 
     * 接口路径：GET /api/inventory/warning
     */
    @GetMapping("/warning")
    @Operation(summary = "查询库存预警列表（包含商品详细信息）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getWarningInventories() {
        // Service 返回 List<InventoryVO>（包含商品信息）
        var warningList = inventoryService.getWarningInventories();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", warningList);
        result.put("count", warningList.size());
        return result;
    }

    /**
     * 商品入库
     * 
     * 权限要求：inventory:in:add
     * 
     * 请求体示例：
     * {
     * "productId": 1,
     * "quantity": 100,
     * "remark": "采购入库"
     * }
     * 
     * 业务流程：
     * 1. 更新库存数量
     * 2. 记录变动日志
     */
    @RequiresPermissions("inventory:in:add")
    @PostMapping("/in")
    @Operation(summary = "商品入库")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "入库成功"),
            @ApiResponse(responseCode = "500", description = "入库失败")
    })
    public Map<String, Object> stockIn(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long productId = Long.valueOf(params.get("productId").toString());
            Integer quantity = Integer.valueOf(params.get("quantity").toString());
            String remark = params.containsKey("remark") ? params.get("remark").toString() : "";

            // 获取当前操作人
            String operator = SecurityUtils.getCurrentUser().getNickname();

            boolean success = inventoryService.stockIn(productId, quantity, operator, remark);
            if (success) {
                result.put("code", 200);
                result.put("msg", "入库成功");
            } else {
                result.put("code", 500);
                result.put("msg", "入库失败");
            }
        } catch (RuntimeException e) {
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * 商品出库
     * 
     * 权限要求：inventory:out:add
     * 
     * 请求体示例：
     * {
     * "productId": 1,
     * "quantity": 50,
     * "remark": "销售出库"
     * }
     * 
     * 业务流程：
     * 1. 检查库存是否充足
     * 2. 更新库存数量
     * 3. 记录变动日志
     */
    @RequiresPermissions("inventory:out:add")
    @PostMapping("/out")
    @Operation(summary = "商品出库")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "出库成功"),
            @ApiResponse(responseCode = "500", description = "出库失败（库存不足）")
    })
    public Map<String, Object> stockOut(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long productId = Long.valueOf(params.get("productId").toString());
            Integer quantity = Integer.valueOf(params.get("quantity").toString());
            String remark = params.containsKey("remark") ? params.get("remark").toString() : "";

            // 获取当前操作人
            String operator = SecurityUtils.getCurrentUser().getNickname();

            boolean success = inventoryService.stockOut(productId, quantity, operator, remark);
            if (success) {
                result.put("code", 200);
                result.put("msg", "出库成功");
            } else {
                result.put("code", 500);
                result.put("msg", "出库失败");
            }
        } catch (RuntimeException e) {
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * 分页查询库存变动记录
     * 
     * 请求参数示例：
     * {
     * "pageNum": 1,
     * "pageSize": 10,
     * "queryParams": {
     * "productId": 1, // 可选：按商品ID查询
     * "changeType": 1, // 可选：按变动类型查询（1-入库，2-出库，3-销售，4-退货，5-盘点）
     * "operator": "张三" // 可选：按操作人查询
     * }
     * }
     */
    @PostMapping("/log/page")
    @Operation(summary = "分页查询库存变动记录（包含商品详细信息）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getInventoryLogsWithPagination(
            @RequestBody PageParams pageParams) {
        Integer pageNum = pageParams.getPageNum() != null ? pageParams.getPageNum() : 1;
        Integer pageSize = pageParams.getPageSize() != null ? pageParams.getPageSize() : 10;
        Page<InventoryLog> page = new Page<>(pageNum, pageSize);
        // Service 返回 IPage<InventoryLogVO>（包含商品信息）
        var logPage = inventoryService.getInventoryLogsWithPagination(page,
                pageParams.getQueryParams());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", logPage);
        return result;
    }
}
