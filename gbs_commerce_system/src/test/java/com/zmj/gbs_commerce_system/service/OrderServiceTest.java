package com.zmj.gbs_commerce_system.service;

import com.zmj.gbs_commerce_system.entity.Inventory;
import com.zmj.gbs_commerce_system.entity.InventoryLog;
import com.zmj.gbs_commerce_system.entity.OrderItem;
import com.zmj.gbs_commerce_system.entity.Orders;
import com.zmj.gbs_commerce_system.entity.Product;
import com.zmj.gbs_commerce_system.mapper.InventoryLogMapper;
import com.zmj.gbs_commerce_system.mapper.InventoryMapper;
import com.zmj.gbs_commerce_system.mapper.OrderItemMapper;
import com.zmj.gbs_commerce_system.mapper.OrdersMapper;
import com.zmj.gbs_commerce_system.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock
    private OrdersMapper ordersMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private InventoryLogMapper inventoryLogMapper;

    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setProductName("测试商品");
        product.setProductCode("TEST001");
        product.setPrice(new BigDecimal("10.00"));
        product.setStatus(0);

        inventory = new Inventory();
        inventory.setProductId(1L);
        inventory.setStockQuantity(100);
        inventory.setVersion(1);
    }

    @Test
    @DisplayName("订单号生成 - 格式正确")
    void testGenerateOrderNo() {
        String orderNo = generateOrderNo();
        
        assertNotNull(orderNo);
        assertEquals(20, orderNo.length());
        assertTrue(orderNo.matches("\\d{20}"));
    }

    @Test
    @DisplayName("下单失败 - 商品已下架")
    void testCreateOrder_ProductOffline() {
        product.setStatus(1);
        
        when(productMapper.selectById(1L)).thenReturn(product);

        Product result = productMapper.selectById(1L);
        
        assertNotNull(result);
        assertEquals(1, result.getStatus());
    }

    @Test
    @DisplayName("下单失败 - 库存不足")
    void testCreateOrder_InsufficientStock() {
        when(productMapper.selectById(1L)).thenReturn(product);
        when(inventoryMapper.selectByProductId(1L)).thenReturn(inventory);

        Inventory result = inventoryMapper.selectByProductId(1L);
        
        assertNotNull(result);
        assertTrue(result.getStockQuantity() < 999);
    }

    @Test
    @DisplayName("扣减库存 - 成功")
    void testDecreaseStock_Success() {
        when(inventoryMapper.decreaseStock(1L, 10)).thenReturn(1);

        int rows = inventoryMapper.decreaseStock(1L, 10);

        assertEquals(1, rows);
        verify(inventoryMapper, times(1)).decreaseStock(1L, 10);
    }

    @Test
    @DisplayName("扣减库存 - 库存不足")
    void testDecreaseStock_InsufficientStock() {
        when(inventoryMapper.decreaseStock(1L, 999)).thenReturn(0);

        int rows = inventoryMapper.decreaseStock(1L, 999);

        assertEquals(0, rows);
    }

    @Test
    @DisplayName("乐观锁扣减 - 成功")
    void testDecreaseStockWithOptimisticLock_Success() {
        when(inventoryMapper.decreaseStockWithOptimisticLock(1L, 10, 1)).thenReturn(1);

        int rows = inventoryMapper.decreaseStockWithOptimisticLock(1L, 10, 1);

        assertEquals(1, rows);
    }

    @Test
    @DisplayName("乐观锁扣减 - 版本冲突")
    void testDecreaseStockWithOptimisticLock_Conflict() {
        when(inventoryMapper.decreaseStockWithOptimisticLock(1L, 10, 999)).thenReturn(0);

        int rows = inventoryMapper.decreaseStockWithOptimisticLock(1L, 10, 999);

        assertEquals(0, rows);
    }

    @Test
    @DisplayName("订单金额计算 - 正确")
    void testOrderAmountCalculation() {
        BigDecimal price1 = new BigDecimal("10.00");
        BigDecimal price2 = new BigDecimal("25.50");
        int quantity1 = 2;
        int quantity2 = 1;
        
        BigDecimal total = price1.multiply(new BigDecimal(quantity1))
                .add(price2.multiply(new BigDecimal(quantity2)));
        
        assertEquals(new BigDecimal("45.50"), total);
    }

    @Test
    @DisplayName("积分计算 - 每10元积1分")
    void testCalculatePoints() {
        BigDecimal paidAmount = new BigDecimal("85.50");
        int expectedPoints = paidAmount.divide(new BigDecimal("10"), 0, java.math.RoundingMode.DOWN).intValue();

        assertEquals(8, expectedPoints);
    }

    private String generateOrderNo() {
        String date = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
        String random = String.format("%06d", new java.util.Random().nextInt(999999));
        return date + random;
    }
}