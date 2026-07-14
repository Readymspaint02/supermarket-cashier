package com.zmj.gbs_commerce_system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmj.gbs_commerce_system.config.BaiduAiProperties;
import com.zmj.gbs_commerce_system.service.FaceAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class FaceAuthServiceImpl implements FaceAuthService {

    private static final Logger log = LoggerFactory.getLogger(FaceAuthServiceImpl.class);

    @Autowired
    private BaiduAiProperties props;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private String cachedAccessToken = null;
    private long tokenExpireTime = 0;

    @Override
    public Optional<String> matchAndGetUsername(String base64Image) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                log.warn("获取百度Access Token失败");
                return Optional.empty();
            }

            Map<String, Object> body = new HashMap<>();
            body.put("image", base64Image);
            body.put("image_type", "BASE64");
            body.put("group_id_list", props.getGroupId());
            body.put("max_face_num", 1);
            body.put("match_threshold", 80);
            body.put("max_user_num", 1);
            body.put("quality_control", "NORMAL");
            body.put("liveness_control", "NORMAL");

            String url = "https://aip.baidubce.com/rest/2.0/face/v3/search?access_token=" + accessToken;
            String json = mapper.writeValueAsString(body);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            JsonNode root = mapper.readTree(resp.body());
            log.debug("人脸搜索响应: {}", resp.body());
            
            if (root.has("error_code") && root.get("error_code").asInt() == 0) {
                JsonNode userList = root.path("result").path("user_list");
                if (userList.isArray() && userList.size() > 0) {
                    JsonNode first = userList.get(0);
                    double score = first.path("score").asDouble(0);
                    if (score >= 80.0) {
                        String username = first.path("user_id").asText();
                        log.info("人脸匹配成功: userId={}, score={}", username, score);
                        return Optional.ofNullable(username);
                    }
                }
            } else {
                log.warn("人脸搜索失败: {}", root.path("error_msg").asText());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("人脸搜索异常", e);
            return Optional.empty();
        }
    }

    @Override
    public RegisterResult registerFace(String userId, String base64Image, String userInfo) {
        try {
            if (props.getApiKey() == null || props.getApiKey().isEmpty()) {
                log.warn("百度AI未配置，请检查application.yml中的baidu.ai.api-key");
                return RegisterResult.fail("人脸识别服务未配置，请联系管理员");
            }
            
            if (props.getGroupId() == null || props.getGroupId().isEmpty()) {
                log.warn("百度AI用户组未配置，请检查application.yml中的baidu.ai.group-id");
                return RegisterResult.fail("人脸识别用户组未配置，请联系管理员");
            }
            
            String accessToken = getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                log.warn("获取百度Access Token失败");
                return RegisterResult.fail("人脸识别服务连接失败，请稍后重试");
            }

            Map<String, Object> body = new HashMap<>();
            body.put("image", base64Image);
            body.put("image_type", "BASE64");
            body.put("group_id", props.getGroupId());
            body.put("user_id", userId);
            if (userInfo != null && !userInfo.isEmpty()) {
                body.put("user_info", userInfo);
            }
            body.put("quality_control", "LOW");
            body.put("liveness_control", "NONE");
            body.put("action_type", "REPLACE");

            String url = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/add?access_token=" + accessToken;
            String json = mapper.writeValueAsString(body);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            JsonNode root = mapper.readTree(resp.body());
            log.info("人脸注册响应: {}", resp.body());
            
            if (root.has("error_code") && root.get("error_code").asInt() == 0) {
                log.info("人脸注册成功: userId={}", userId);
                return RegisterResult.success();
            } else {
                String errorMsg = root.path("error_msg").asText();
                int errorCode = root.path("error_code").asInt();
                log.warn("人脸注册失败: errorCode={}, errorMsg={}", errorCode, errorMsg);
                
                String userMsg = translateBaiduError(errorCode, errorMsg);
                return RegisterResult.fail(userMsg);
            }
        } catch (Exception e) {
            log.error("人脸注册异常", e);
            return RegisterResult.fail("人脸注册异常: " + e.getMessage());
        }
    }

    private String translateBaiduError(int errorCode, String errorMsg) {
        switch (errorCode) {
            case 216015:
                return "未检测到人脸，请确保照片清晰且正对摄像头";
            case 216016:
                return "检测到多张人脸，请确保照片中只有一人";
            case 216018:
                return "人脸太小，请靠近摄像头";
            case 216019:
                return "人脸模糊，请保持稳定";
            case 216020:
                return "人脸不完整，请确保整张脸在画面中";
            case 216021:
                return "人脸遮挡，请摘下眼镜或口罩";
            case 216022:
                return "人脸角度过大，请正对摄像头";
            case 216100:
                return "人脸库不存在，请先创建用户组";
            case 216101:
                return "用户组参数错误";
            case 216102:
                return "用户ID参数错误";
            case 216103:
                return "人脸图片参数错误";
            case 216110:
                return "该用户人脸数量已达上限";
            default:
                return "人脸注册失败: " + errorMsg;
        }
    }

    @Override
    public boolean verifyFaceForMember(String base64Image, String expectedUserId) {
        if (props.getApiKey() == null || props.getApiKey().isEmpty()) {
            log.warn("百度人脸识别未配置，跳过验证");
            return true;
        }
        
        try {
            String accessToken = getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                log.warn("获取百度Access Token失败");
                return false;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("image", base64Image);
            body.put("image_type", "BASE64");
            body.put("group_id_list", props.getGroupId());
            body.put("user_id", expectedUserId);
            body.put("max_face_num", 1);
            body.put("match_threshold", 80);
            body.put("max_user_num", 1);
            body.put("quality_control", "NORMAL");
            body.put("liveness_control", "NORMAL");

            String url = "https://aip.baidubce.com/rest/2.0/face/v3/search?access_token=" + accessToken;
            String json = mapper.writeValueAsString(body);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            JsonNode root = mapper.readTree(resp.body());
            log.info("人脸验证响应: {}", resp.body());
            
            if (root.has("error_code") && root.get("error_code").asInt() == 0) {
                JsonNode userList = root.path("result").path("user_list");
                if (userList.isArray() && userList.size() > 0) {
                    JsonNode first = userList.get(0);
                    String matchedUserId = first.path("user_id").asText();
                    double score = first.path("score").asDouble(0);
                    
                    if (matchedUserId.equals(expectedUserId) && score >= 80.0) {
                        log.info("人脸验证成功: expectedUserId={}, matchedUserId={}, score={}", 
                                expectedUserId, matchedUserId, score);
                        return true;
                    } else {
                        log.warn("人脸验证失败: expectedUserId={}, matchedUserId={}, score={}", 
                                expectedUserId, matchedUserId, score);
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("人脸验证异常", e);
            return false;
        }
    }

    private synchronized String getAccessToken() throws Exception {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedAccessToken;
        }
        
        String url = "https://aip.baidubce.com/oauth/2.0/token"
                + "?grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(props.getApiKey(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(props.getSecretKey(), StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(resp.body());
        
        cachedAccessToken = root.path("access_token").asText(null);
        int expiresIn = root.path("expires_in").asInt(0);
        tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
        
        return cachedAccessToken;
    }
}
