package com.zmj.gbs_commerce_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmj.gbs_commerce_system.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 商品信息 Mapper 接口
 * 
 * 提供商品相关的数据库操作
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 根据商品编码查询商品
     * 用于收银时扫描条形码查询商品
     * 
     * @param productCode 商品编码/条形码
     * @return 商品信息
     */
    @Select("SELECT * FROM product WHERE product_code = #{productCode} AND status = 0")
    Product selectByProductCode(String productCode);

    @Select("SELECT * FROM product WHERE barcode = #{barcode} AND status = 0")
    Product selectByBarcode(String barcode);
}
