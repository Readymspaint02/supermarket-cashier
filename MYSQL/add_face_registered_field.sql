-- 会员表新增人脸注册状态字段
-- 用于记录会员是否已在百度人脸库注册过人脸

ALTER TABLE members 
ADD COLUMN face_registered TINYINT(1) DEFAULT 0 COMMENT '人脸注册状态: 0-未注册, 1-已注册';

-- 更新说明：
-- 当会员进行人脸注册成功后，该字段会被更新为1
-- 前端根据此字段动态显示"人脸注册"或"人脸更新"按钮
