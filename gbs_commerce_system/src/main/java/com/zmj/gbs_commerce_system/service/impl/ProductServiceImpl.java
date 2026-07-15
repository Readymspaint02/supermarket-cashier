package com.zmj.gbs_commerce_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.Inventory;
import com.zmj.gbs_commerce_system.entity.Product;
import com.zmj.gbs_commerce_system.mapper.InventoryMapper;
import com.zmj.gbs_commerce_system.mapper.ProductMapper;
import com.zmj.gbs_commerce_system.service.ProductService;
import com.zmj.gbs_commerce_system.util.BarcodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.upload-dir:D:/uploads}")
    private String uploadPath;

    private static final String CACHE_PREFIX = "product:";
    private static final long CACHE_TTL = 30;
    
    @Override
    public List<Product> getAllProducts() {
        return productMapper.selectList(new QueryWrapper<Product>().eq("status", 0));
    }
    
    @Override
    public IPage<Product> getProductsWithPagination(Page<Product> page, Map<String, Object> queryParams) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        
        if (queryParams != null && queryParams.containsKey("productName") && queryParams.get("productName") != null) {
            queryWrapper.like("product_name", queryParams.get("productName"));
        }
        
        if (queryParams != null && queryParams.containsKey("productCode") && queryParams.get("productCode") != null) {
            queryWrapper.like("product_code", queryParams.get("productCode"));
        }
        
        if (queryParams != null && queryParams.containsKey("categoryId") && queryParams.get("categoryId") != null) {
            queryWrapper.eq("category_id", queryParams.get("categoryId"));
        }
        
        if (queryParams != null && queryParams.containsKey("status")) {
            queryWrapper.eq("status", queryParams.get("status"));
        } else {
            queryWrapper.eq("status", 0);
        }
        
        queryWrapper.orderByDesc("create_time");
        
        return productMapper.selectPage(page, queryWrapper);
    }
    
    @Override
    public Product getProductById(Long id) {
        String cacheKey = CACHE_PREFIX + "id:" + id;
        Product product = (Product) redisTemplate.opsForValue().get(cacheKey);
        if (product != null) {
            log.info("命中商品缓存: {}", cacheKey);
            return product;
        }
        product = productMapper.selectById(id);
        if (product != null) {
            redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL, TimeUnit.MINUTES);
        }
        return product;
    }
    
    @Override
    public Product getProductByCode(String productCode) {
        String cacheKey = CACHE_PREFIX + productCode;
        Product product = (Product) redisTemplate.opsForValue().get(cacheKey);
        if (product != null) {
            log.info("命中商品条码缓存: {}", cacheKey);
            return product;
        }
        product = productMapper.selectByProductCode(productCode);
        if (product != null) {
            redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL, TimeUnit.MINUTES);
        }
        return product;
    }

    @Override
    public Product getProductByBarcode(String barcode) {
        String cacheKey = CACHE_PREFIX + "barcode:" + barcode;
        Product product = (Product) redisTemplate.opsForValue().get(cacheKey);
        if (product != null) {
            log.info("命中条码缓存: {}", cacheKey);
            return product;
        }
        product = productMapper.selectByBarcode(barcode);
        if (product == null) {
            product = productMapper.selectByProductCode(barcode);
        }
        if (product != null) {
            redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL, TimeUnit.MINUTES);
        }
        return product;
    }
    
    @Override
    @Transactional
    public boolean saveProduct(Product product) {
        if (product.getProductCode() == null || product.getProductCode().isEmpty()) {
            product.setProductCode(BarcodeUtil.generateEAN13());
            log.info("自动生成商品条码: {}", product.getProductCode());
        }
        if (product.getBarcode() == null || product.getBarcode().isEmpty()) {
            product.setBarcode(product.getProductCode());
        }
        
        product.setProductImage(downloadImageIfNeeded(product.getProductImage()));
        
        if (productMapper.insert(product) > 0) {
            Inventory inventory = new Inventory();
            inventory.setProductId(product.getId());
            inventory.setStockQuantity(0);
            inventory.setWarningQuantity(10);
            inventoryMapper.insert(inventory);
            String cacheKey = CACHE_PREFIX + product.getProductCode();
            redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL, TimeUnit.MINUTES);
            log.info("新增商品写入缓存: {}", cacheKey);
            return true;
        }
        return false;
    }
    
    @Override
    @Transactional
    public boolean updateProduct(Product product) {
        product.setProductImage(downloadImageIfNeeded(product.getProductImage()));
        
        if (productMapper.updateById(product) > 0) {
            redisTemplate.delete(CACHE_PREFIX + product.getProductCode());
            redisTemplate.delete(CACHE_PREFIX + "id:" + product.getId());
            log.info("更新商品清除缓存: {}", product.getProductCode());
            return true;
        }
        return false;
    }

    private String downloadImageIfNeeded(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return imageUrl;
        }
        
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        
        try {
            log.info("开始下载外链图片: {}", imageUrl);
            
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                log.warn("下载图片失败，响应码: {}", responseCode);
                return imageUrl;
            }
            
            String contentType = connection.getContentType();
            String extension = ".jpg";
            if (contentType != null) {
                if (contentType.contains("png")) extension = ".png";
                else if (contentType.contains("gif")) extension = ".gif";
                else if (contentType.contains("webp")) extension = ".webp";
            }
            
            String fileName = UUID.randomUUID().toString() + extension;
            
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            File outputFile = new File(uploadDir, fileName);
            
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            String localPath = "/uploads/" + fileName;
            log.info("图片下载成功: {} -> {}", imageUrl, localPath);
            return localPath;
            
        } catch (Exception e) {
            log.error("下载图片失败: {}, error: {}", imageUrl, e.getMessage());
            return imageUrl;
        }
    }
    
    @Override
    @Transactional
    public boolean deleteProductById(Long id) {
        if (id == null) {
            log.warn("删除商品失败：id为空");
            return false;
        }
        Product product = productMapper.selectById(id);
        if (product == null) {
            log.warn("删除商品失败：商品不存在，id={}", id);
            return false;
        }
        Product p = new Product();
        p.setId(id);
        p.setStatus(1);
        if (productMapper.updateById(p) > 0) {
            redisTemplate.delete(CACHE_PREFIX + product.getProductCode());
            redisTemplate.delete(CACHE_PREFIX + "id:" + id);
            log.info("删除商品成功（软删除），id={}", id);
            return true;
        }
        return false;
    }
    
    @Override
    @Transactional
    public boolean deleteBatchProducts(List<Long> ids) {
        for (Long id : ids) {
            deleteProductById(id);
        }
        return true;
    }
}

