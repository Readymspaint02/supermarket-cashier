package com.zmj.gbs_commerce_system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Product;
import com.zmj.gbs_commerce_system.service.FileUploadService;
import com.zmj.gbs_commerce_system.service.ProductService;
import com.zmj.gbs_commerce_system.utils.PageParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品信息管理 Controller
 * 
 * 提供商品的增删改查接口
 * 
 * 接口路径：/api/product/...
 */
@RestController
@RequestMapping("/product")
@Tag(name = "商品信息管理接口")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 获取所有商品列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有商品列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", products);
        return result;
    }

    /**
     * 分页获取商品列表
     * 
     * 请求参数示例：
     * {
     * "pageNum": 1,
     * "pageSize": 10,
     * "queryParams": {
     * "productName": "可乐", // 可选：按商品名称模糊查询
     * "productCode": "6901", // 可选：按商品编码模糊查询
     * "categoryId": 1, // 可选：按分类ID查询
     * "status": 0 // 可选：按状态查询（0-正常，1-下架）
     * }
     * }
     */
    @PostMapping("/page")
    @Operation(summary = "分页获取商品列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, Object> getProductsWithPagination(
            @RequestBody PageParams pageParams) {
        Integer pageNum = pageParams.getPageNum() != null ? pageParams.getPageNum() : 1;
        Integer pageSize = pageParams.getPageSize() != null ? pageParams.getPageSize() : 10;
        Page<Product> page = new Page<>(pageNum, pageSize);
        IPage<Product> productPage = productService.getProductsWithPagination(page, pageParams.getQueryParams());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", productPage);
        return result;
    }

    /**
     * 根据ID获取商品信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取商品信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "商品不存在")
    })
    public Map<String, Object> getProductById(
            @Parameter(description = "商品ID") @PathVariable Long id) {
        Product product = productService.getProductById(id);
        Map<String, Object> result = new HashMap<>();
        if (product != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", product);
        } else {
            result.put("code", 500);
            result.put("msg", "商品不存在");
        }
        return result;
    }

    /**
     * 根据商品编码查询商品
     * 
     * 使用场景：收银时扫描条形码
     * 
     * 接口路径：GET /api/product/code/{productCode}
     * 例如：GET /api/product/code/6901668000015
     */
    @GetMapping("/code/{productCode}")
    @Operation(summary = "根据商品编码查询商品")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "商品不存在")
    })
    public Map<String, Object> getProductByCode(
            @Parameter(description = "商品编码/条形码") @PathVariable String productCode) {
        Product product = productService.getProductByCode(productCode);
        Map<String, Object> result = new HashMap<>();
        if (product != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", product);
        } else {
            result.put("code", 500);
            result.put("msg", "商品不存在或已下架");
        }
        return result;
    }

    /**
     * 根据商品条码查询商品
     */
    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "根据条码查询商品")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "商品不存在")
    })
    public Map<String, Object> getProductByBarcode(
            @Parameter(description = "商品条码") @PathVariable String barcode) {
        Product product = productService.getProductByBarcode(barcode);
        Map<String, Object> result = new HashMap<>();
        if (product != null) {
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", product);
        } else {
            result.put("code", 500);
            result.put("msg", "商品不存在或已下架");
        }
        return result;
    }

    /**
     * 上传商品图片
     * 
     * 复用 FileUploadService
     * 
     * 请求类型：multipart/form-data
     * 参数：file
     */
    @PostMapping("/uploadImage")
    @Operation(summary = "上传商品图片")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功"),
            @ApiResponse(responseCode = "500", description = "上传失败")
    })
    public Map<String, Object> uploadProductImage(
            @Parameter(description = "图片文件") @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            String fileName = fileUploadService.uploadFile(file);
            // 返回相对路径（/uploads/xxx.jpg），前端会拼接完整URL
            String relativePath = "/uploads/" + fileName;
            result.put("code", 200);
            result.put("msg", "上传成功");
            result.put("data", relativePath); // ✅ 统一使用 data 字段
        } catch (IOException e) {
            result.put("code", 500);
            result.put("msg", "上传失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 新增商品
     * 
     * 权限要求：product:info:add
     * 
     * 请求体示例：
     * {
     * "productName": "可口可乐 330ml",
     * "productCode": "6901668000015",
     * "categoryId": 11,
     * "price": 3.00,
     * "costPrice": 2.20,
     * "unit": "瓶",
     * "productImage": "1730012345678.jpg",
     * "description": "经典可乐",
     * "status": 0
     * }
     * 
     * 注意：新增商品时会自动创建库存记录
     */
    @RequiresPermissions("product:info:add")
    @PostMapping("/add")
    @Operation(summary = "新增商品")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "500", description = "新增失败")
    })
    public Map<String, Object> addProduct(
            @Parameter(description = "商品信息") @RequestBody Product product) {
        boolean success = productService.saveProduct(product);
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
     * 更新商品
     * 
     * 权限要求：product:info:edit
     */
    @RequiresPermissions("product:info:edit")
    @PutMapping("/update/{id}")
    @Operation(summary = "更新商品信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "500", description = "更新失败")
    })
    public Map<String, Object> updateProduct(@PathVariable Long id,
            @Parameter(description = "商品信息") @RequestBody Product product) {
        product.setId(id);
        boolean success = productService.updateProduct(product);
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
     * 删除商品（软删除）
     * 
     * 权限要求：product:info:delete
     * 
     * 注意：这是软删除（修改status=1），不是物理删除
     * 因为可能有历史订单关联
     */
    @RequiresPermissions("product:info:delete")
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除商品")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public Map<String, Object> deleteProduct(
            @Parameter(description = "商品ID") @PathVariable Long id) {
        boolean success = productService.deleteProductById(id);
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
     * 批量删除商品
     * 
     * 权限要求：product:info:delete
     * 
     * 请求体示例：[1, 2, 3]
     */
    @RequiresPermissions("product:info:delete")
    @DeleteMapping("/batchDelete")
    @Operation(summary = "批量删除商品")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public Map<String, Object> batchDeleteProducts(
            @Parameter(description = "商品ID数组") @RequestBody Long[] ids) {
        boolean success = productService.deleteBatchProducts(Arrays.asList(ids));
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "批量删除成功");
        } else {
            result.put("code", 500);
            result.put("msg", "批量删除失败");
        }
        return result;
    }
}
