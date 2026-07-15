package com.zmj.gbs_commerce_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// @RestController
// @RequestMapping("/image")
// @Tag(name = "Image Crawler API")
// 已注释：与ImageController冲突，使用ImageController代替
public class ImageCrawlerController {

    @GetMapping("/search")
    @Operation(summary = "Search product image from Baidu")
    public Map<String, Object> searchImage(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = "https://image.baidu.com/search/index?tn=baiduimage&word=" + encodedKeyword;
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://www.baidu.com/")
                    .timeout(15000)
                    .get();
            
            List<String> imageUrls = new ArrayList<>();
            
            Elements imgElements = doc.select("img");
            for (Element img : imgElements) {
                String src = img.attr("src");
                String dataImgurl = img.attr("data-imgurl");
                
                if (dataImgurl != null && !dataImgurl.isEmpty() && dataImgurl.startsWith("http")) {
                    imageUrls.add(dataImgurl);
                } else if (src != null && src.startsWith("http") && !src.contains("baidu.com") && !src.contains("nstatic")) {
                    imageUrls.add(src);
                }
                
                if (imageUrls.size() >= 10) break;
            }
            
            if (imageUrls.isEmpty()) {
                String html = doc.html();
                Pattern pattern = Pattern.compile("\"hoverURL\":\"(https?://[^\"]+)\"");
                Matcher matcher = pattern.matcher(html);
                while (matcher.find() && imageUrls.size() < 10) {
                    imageUrls.add(matcher.group(1).replace("\\/", "/"));
                }
            }
            
            if (imageUrls.isEmpty()) {
                Pattern pattern = Pattern.compile("\"objURL\":\"(https?://[^\"]+)\"");
                Matcher matcher = pattern.matcher(doc.html());
                while (matcher.find() && imageUrls.size() < 10) {
                    imageUrls.add(matcher.group(1).replace("\\/", "/"));
                }
            }
            
            if (!imageUrls.isEmpty()) {
                result.put("code", 200);
                result.put("msg", "Success");
                result.put("data", imageUrls);
            } else {
                result.put("code", 404);
                result.put("msg", "No images found");
                result.put("debug", doc.title());
            }
            
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "Error: " + e.getMessage());
        }
        
        return result;
    }
}