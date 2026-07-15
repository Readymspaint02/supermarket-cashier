package com.zmj.gbs_commerce_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/image")
@Tag(name = "图片搜索接口")
public class ImageController {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @GetMapping("/proxy")
    @Operation(summary = "代理图片搜索页面")
    public String proxyImageSearch(
            @RequestParam String type,
            @RequestParam String keyword) {
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url;
            String referer;
            
            if ("bing".equals(type)) {
                url = "https://www.bing.com/images/search?q=" + encodedKeyword + "&first=1&count=20";
                referer = "https://www.bing.com/";
            } else {
                url = "https://pic.sogou.com/pics?query=" + encodedKeyword + "&mode=1";
                referer = "https://www.sogou.com/";
            }
            
            log.info("代理请求图片搜索: type={}, keyword={}", type, keyword);
            
            String html = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", referer)
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .timeout(20000)
                    .followRedirects(true)
                    .execute()
                    .body();
            
            log.info("代理请求成功: type={}, 响应长度={}", type, html.length());
            return html;
            
        } catch (Exception e) {
            log.error("代理请求失败: type={}, keyword={}, error={}", type, keyword, e.getMessage());
            return "";
        }
    }

    @GetMapping("/search")
    @Operation(summary = "搜索商品图片（备用）")
    public String searchImage(@RequestParam String keyword) {
        return proxyImageSearch("sogou", keyword);
    }
}