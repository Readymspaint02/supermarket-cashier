package com.zmj.gbs_commerce_system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "huawei.asr")
public class HuaweiAsrProperties {
    private String ak;
    private String sk;
    private String region;
    private String projectId;
}