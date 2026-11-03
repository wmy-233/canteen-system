package com.canteen.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置（OpenAPI3 + SpringDoc）
 * 启动后访问：http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("canteen-api")
                .packagesToScan("com.canteen.controller")
                .pathsToMatch("/**")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("智慧校园食堂点餐系统 API")
                .description("《Java 高级开发技术》期末大作业 - 前后端分离项目")
                .version("0.1.0"));
    }
}
