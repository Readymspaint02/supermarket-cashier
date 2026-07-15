-- ============================================================
-- 将现有商品编码替换为EAN-13条形码
-- 执行前请备份数据库！
-- ============================================================

-- 1. 查看当前商品编码情况
SELECT id, product_name, product_code, barcode FROM product WHERE product_code NOT REGEXP '^[0-9]{13}$' AND status = 0;

-- 2. 更新商品编码为EAN-13条形码（手动执行每条UPDATE）
-- 格式：690 + 9位随机数 + 校验位
-- 例如：
UPDATE product SET product_code = '6901234567890', barcode = '6901234567890' WHERE id = 1;
UPDATE product SET product_code = '6901234567891', barcode = '6901234567891' WHERE id = 2;
-- ... 需要为每个商品生成唯一条形码

-- 3. 验证更新结果
SELECT id, product_name, product_code, barcode FROM product WHERE status = 0 ORDER BY id;
