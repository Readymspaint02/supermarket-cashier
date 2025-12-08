// package com.zmj.gbs_commerce_system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 1. 定义安全模式（Token 认证）
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY) // API Key 类型
                .in(SecurityScheme.In.HEADER) // 凭证位置：请求头
                .name("Authorization") // 请求头名称（需与 Shiro Filter 提取的名称一致）
                .description("Token 格式：Bearer {token}"); // 描述提示

        // 2. 全局添加安全要求（所有接口都需携带该凭证）ne
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("Authorization");

        // 3. 构建 OpenAPI 文档
        return new OpenAPI()
                .info(new Info().title("Shiro 授权接口测试").version("1.0").description("Swagger 测试 Shiro 授权接口"))
                .addSecurityItem(securityRequirement) // 全局启用凭证
                .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("Authorization", securityScheme))
                .servers(List.of(
                        new Server().url("https://iloveagent.club/api").description("HTTPS"),
                        new Server().url("http://iloveagent.club/api").description("HTTP")));
    }

}