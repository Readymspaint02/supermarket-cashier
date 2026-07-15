package com.zmj.gbs_commerce_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/image")
@Tag(name = "图片搜索接口")
public class ImageController {

    @GetMapping("/search")
    @Operation(summary = "搜索商品图片")
    public Map<String, Object> searchImage(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            result.put("code", 400);
            result.put("msg", "关键词不能为空");
            return result;
        }

        try {
            List<String> images = searchFromBaidu(keyword.trim());
            
            if (images.isEmpty()) {
                result.put("code", 404);
                result.put("msg", "No images found");
                result.put("debug", "百度图片搜索");
            } else {
                result.put("code", 200);
                result.put("msg", "搜索成功");
                result.put("data", images);
            }
        } catch (Exception e) {
            log.error("图片搜索失败: {}", e.getMessage());
            result.put("code", 500);
            result.put("msg", "图片搜索失败: " + e.getMessage());
        }

        return result;
    }

    private List<String> searchFromBaidu(String keyword) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String searchUrl = "https://image.baidu.com/search/index?tn=baiduimage&word=" + encodedKeyword;
            
            URL url = new URL(searchUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                log.warn("百度图片搜索响应码: {}", responseCode);
                return imageUrls;
            }

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String htmlContent = response.toString();
            
            Pattern pattern = Pattern.compile("\"hoverURL\":\"(https?://[^\"\\s]+)\"");
            Matcher matcher = pattern.matcher(htmlContent);
            
            int count = 0;
            while (matcher.find() && count < 5) {
                String imageUrl = matcher.group(1);
                if (!imageUrl.contains("baidu.com")) {
                    imageUrls.add(imageUrl);
                    count++;
                }
            }

            if (imageUrls.isEmpty()) {
                pattern = Pattern.compile("\"objURL\":\"(https?://[^\"\\s]+)\"");
                matcher = pattern.matcher(htmlContent);
                count = 0;
                while (matcher.find() && count < 5) {
                    String imageUrl = matcher.group(1);
                    imageUrls.add(imageUrl);
                    count++;
                }
            }

            log.info("百度图片搜索完成: keyword={}, 找到{}张图片", keyword, imageUrls.size());

        } catch (Exception e) {
            log.error("百度图片搜索异常: {}", e.getMessage());
        }

        return imageUrls;
    }
}