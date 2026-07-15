package com.zmj.gbs_commerce_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
            images.addAll(searchFromBaidu(searchKeyword));
        }

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
            String url = "https://pic.sogou.com/pics?query=" + encodedKeyword + "&mode=1";
            
            log.info("搜狗图片搜索: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .header("Referer", "https://www.sogou.com/")
                    .timeout(20000)
                    .followRedirects(true)
                    .get();
            
            String html = doc.html();
            log.info("搜狗响应长度: {}", html.length());
            
            String[] jsonPatterns = {
                "\"thumbUrl\"\\s*:\\s*\"(https?:\\\\?/?\\\\?/?[^\"]+)\"",
                "\"picUrl\"\\s*:\\s*\"(https?:\\\\?/?\\\\?/?[^\"]+)\"",
                "\"oriPicUrl\"\\s*:\\s*\"(https?:\\\\?/?\\\\?/?[^\"]+)\""
            };
            
            for (String patternStr : jsonPatterns) {
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(html);
                int count = 0;
                while (matcher.find() && count < 8) {
                    String imgUrl = matcher.group(1)
                            .replace("\\/", "/")
                            .replace("\\u002F", "/");
                    if (imgUrl.startsWith("http") && !imgUrl.contains("sogou.com/static")) {
                        imageUrls.add(imgUrl);
                        count++;
                    }
                }
                if (!imageUrls.isEmpty()) break;
            }
            
            log.info("搜狗找到: {}张图片", imageUrls.size());

        } catch (Exception e) {
            log.error("搜狗图片搜索异常: {}", e.getMessage());
        }

        return imageUrls;
    }

    private List<String> searchFromBaidu(String keyword) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = "https://image.baidu.com/search/index?tn=baiduimage&word=" + encodedKeyword;
            
            log.info("百度图片搜索: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .header("Referer", "https://www.baidu.com/")
                    .timeout(20000)
                    .followRedirects(true)
                    .get();
            
            String html = doc.html();
            log.info("百度响应长度: {}", html.length());
            
            String[] jsonPatterns = {
                "\"hoverURL\"\\s*:\\s*\"(https?:\\\\?/?\\\\?/?[^\"]+)\"",
                "\"objURL\"\\s*:\\s*\"(https?:\\\\?/?\\\\?/?[^\"]+)\"",
                "\"middleURL\"\\s*:\\s*\"(https?:\\\\?/?\\\\?/?[^\"]+)\""
            };
            
            for (String patternStr : jsonPatterns) {
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(html);
                int count = 0;
                while (matcher.find() && count < 8) {
                    String imgUrl = matcher.group(1)
                            .replace("\\/", "/")
                            .replace("\\u002F", "/");
                    if (imgUrl.startsWith("http") && !imgUrl.contains("baidu.com")) {
                        imageUrls.add(imgUrl);
                        count++;
                    }
                }
                if (!imageUrls.isEmpty()) break;
            }
            
            log.info("百度找到: {}张图片", imageUrls.size());

        } catch (Exception e) {
            log.error("百度图片搜索异常: {}", e.getMessage());
        }

        return imageUrls;
    }

    private List<String> searchFromBing(String keyword) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = "https://www.bing.com/images/search?q=" + encodedKeyword + "&first=1&count=20";
            
            log.info("Bing图片搜索: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://www.bing.com/")
                    .timeout(20000)
                    .followRedirects(true)
                    .get();
            
            String html = doc.html();
            log.info("Bing响应长度: {}", html.length());
            
            String[] jsonPatterns = {
                "murl\"\\s*:\\s*\"(https?:\\\\?/?\\\\?/?[^\"]+)\"",
                "\"imgurl\"\\s*:\\s*\"(https?:\\\\?/?\\\\?/?[^\"]+)\""
            };
            
            for (String patternStr : jsonPatterns) {
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(html);
                int count = 0;
                while (matcher.find() && count < 8) {
                    String imgUrl = matcher.group(1)
                            .replace("\\/", "/")
                            .replace("\\u002F", "/");
                    if (imgUrl.startsWith("http") && !imgUrl.contains("bing.com")) {
                        imageUrls.add(imgUrl);
                        count++;
                    }
                }
                if (!imageUrls.isEmpty()) break;
            }
            
            log.info("Bing找到: {}张图片", imageUrls.size());

        } catch (Exception e) {
            log.error("Bing图片搜索异常: {}", e.getMessage());
        }

        return imageUrls;
    }
}