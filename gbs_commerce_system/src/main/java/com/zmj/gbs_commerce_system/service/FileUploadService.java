package com.zmj.gbs_commerce_system.service;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class FileUploadService {
    @Value("${app.upload-dir}")
    private String uploadDir;
    public String uploadFile(MultipartFile multipartFile) throws IOException{
        String uploadFileName = multipartFile.getOriginalFilename();
        // 条件成立：表示添加文件，获取图片信息并将其存储到后端指定的路径
        if (StrUtil.isNotBlank(uploadFileName)) {
            // 对上传文件进行重命名，系统当前毫秒数作为文件前缀，后缀不变
            // 获取前端上传文件的后缀
            int index = StrUtil.indexOf(uploadFileName, '.');
            String suffix = uploadFileName.substring(index);
            // 系统当前毫秒数作为上传文件的前缀
            long prefix = System.currentTimeMillis();
            uploadFileName = prefix + suffix;
            // 创建父路径
            File parentPath = new File(uploadDir);
            // 条件成立：表示路径不存在，将其创建
            if (!parentPath.exists()) {
                parentPath.mkdirs();
            }
            File fullPath = new File(parentPath, uploadFileName);
            try {
                multipartFile.transferTo(fullPath);
                return uploadFileName;
            } catch (Exception e) {
                throw new IOException("上传失败");
            }
        }
        return null;
    }
}
