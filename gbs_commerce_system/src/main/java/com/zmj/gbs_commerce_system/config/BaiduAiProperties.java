package com.zmj.gbs_commerce_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "baidu.ai")
public class BaiduAiProperties {
    private String appId;
    private String apiKey;
    private String secretKey;
    private String groupId;

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
}
