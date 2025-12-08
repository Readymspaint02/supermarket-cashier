package com.zmj.gbs_commerce_system.controller;

import com.zmj.gbs_commerce_system.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/system/file")
public class FileUploadController {
    @Autowired
    private FileUploadService fileUploadService;
    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestParam("file") MultipartFile multipartFile) {
        Map<String, Object> result = new HashMap<>();
        String uploadFileName = null;
        try {
            if (multipartFile.isEmpty()) {
                result.put("code", 500);
                result.put("msg", "上传文件不能为空");
                return result;
            }
            uploadFileName=fileUploadService.uploadFile(multipartFile);
        }catch (IOException e){
            result.put("code", 500);
            result.put("msg", "上传文件失败");
        }
        if (uploadFileName!=null) {
            result.put("code", 200);
            result.put("msg", "上传文件成功");
            result.put("data", "/uploads/"+uploadFileName);
        }
        return result;
    }
}

