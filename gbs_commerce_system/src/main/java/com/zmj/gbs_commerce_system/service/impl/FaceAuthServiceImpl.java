package com.zmj.gbs_commerce_system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmj.gbs_commerce_system.config.BaiduAiProperties;
import com.zmj.gbs_commerce_system.service.FaceAuthService;
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

    @Autowired
    private BaiduAiProperties props;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public Optional<String> matchAndGetUsername(String base64Image) {
        try {
            String accessToken = fetchAccessToken();
            if (accessToken == null || accessToken.isEmpty()) return Optional.empty();

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
            if (root.has("error_code") && root.get("error_code").asInt() == 0) {
                JsonNode userList = root.path("result").path("user_list");
                if (userList.isArray() && userList.size() > 0) {
                    JsonNode first = userList.get(0);
                    double score = first.path("score").asDouble(0);
                    if (score >= 80.0) {
                        String username = first.path("user_id").asText();
                        return Optional.ofNullable(username);
                    }
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String fetchAccessToken() throws Exception {
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
        return root.path("access_token").asText(null);
    }
}
