package com.zmj.gbs_commerce_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/image")
@Tag(name = "图片搜索接口")
public class ImageController {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @GetMapping("/search")
    @Operation(summary = "搜索商品图片")
    public Map<String, Object> searchImage(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            result.put("code", 400);
            result.put("msg", "关键词不能为空");
            return result;
        }

        String searchKeyword = keyword.trim();
        List<String> images = new ArrayList<>();
        
        images.addAll(searchFromSogou(searchKeyword));
        
        if (images.isEmpty()) {
            images.addAll(searchFromBing(searchKeyword));
        }

        if (images.isEmpty()) {
            result.put("code", 404);
            result.put("msg", "未找到相关图片，请手动上传");
        } else {
            result.put("code", 200);
            result.put("msg", "搜索成功");
            result.put("data", images);
            log.info("图片搜索成功: keyword={}, 找到{}张", searchKeyword, images.size());
        }

        return result;
    }

    private List<String> searchFromSogou(String keyword) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String searchUrl = "https://pic.sogou.com/pics?query=" + encodedKeyword + "&mode=1";
            
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .header("Referer", "https://www.sogou.com/")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();
            
            String html = doc.html();
            
            Pattern[] patterns = {
                Pattern.compile("\"thumbUrl\"\\s*:\\s*\"(https?://[^\"]+)\""),
                Pattern.compile("\"picUrl\"\\s*:\\s*\"(https?://[^\"]+)\""),
                Pattern.compile("\"oriPicUrl\"\\s*:\\s*\"(https?://[^\"]+)\"")
            };
            
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(html);
                int count = 0;
                while (matcher.find() && count < 5) {
                    String url = matcher.group(1)
                            .replace("\\/", "/")
                            .replace("\\u002F", "/");
                    if (isValidImageUrl(url)) {
                        imageUrls.add(url);
                        count++;
                    }
                }
                if (!imageUrls.isEmpty()) break;
            }
            
            if (imageUrls.isEmpty()) {
                Elements imgElements = doc.select("img[src^=http]");
                int count = 0;
                for (Element img : imgElements) {
                    String src = img.attr("src");
                    if (isValidImageUrl(src) && !src.contains("sogou.com/static")) {
                        imageUrls.add(src);
                        count++;
                        if (count >= 5) break;
                    }
                }
            }

            log.info("搜狗图片搜索: keyword={}, 找到{}张", keyword, imageUrls.size());

        } catch (Exception e) {
            log.error("搜狗图片搜索异常: {}", e.getMessage());
        }

        return imageUrls;
    }

    private List<String> searchFromBing(String keyword) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String searchUrl = "https://www.bing.com/images/search?q=" + encodedKeyword + "&first=1&count=20";
            
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://www.bing.com/")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();
            
            String html = doc.html();
            
            Pattern[] patterns = {
                Pattern.compile("murl\"\\s*:\\s*\"(https?://[^\"]+)\""),
                Pattern.compile("\"imgurl\"\\s*:\\s*\"(https?://[^\"]+)\""),
                Pattern.compile("data-src=\"(https?://[^\"]+)\"")
            };
            
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(html);
                int count = 0;
                while (matcher.find() && count < 5) {
                    String url = matcher.group(1).replace("\\/", "/");
                    if (isValidImageUrl(url) && !url.contains("bing.com")) {
                        imageUrls.add(url);
                        count++;
                    }
                }
                if (!imageUrls.isEmpty()) break;
            }
            
            if (imageUrls.isEmpty()) {
                Elements imgElements = doc.select("img[src^=http], img[data-src^=http]");
                int count = 0;
                for (Element img : imgElements) {
                    String src = img.hasAttr("data-src") ? img.attr("data-src") : img.attr("src");
                    if (isValidImageUrl(src) && !src.contains("bing.com") && !src.contains("th?id=")) {
                        imageUrls.add(src);
                        count++;
                        if (count >= 5) break;
                    }
                }
            }

            log.info("Bing图片搜索: keyword={}, 找到{}张", keyword, imageUrls.size());

        } catch (Exception e) {
            log.error("Bing图片搜索异常: {}", e.getMessage());
        }

        return imageUrls;
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String lower = url.toLowerCase();
        return (lower.startsWith("http://") || lower.startsWith("https://"))
                && (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") 
                    || lower.contains(".webp") || lower.contains(".gif") || lower.contains("img")
                    || lower.contains("image") || lower.contains("pic"));
    }
}