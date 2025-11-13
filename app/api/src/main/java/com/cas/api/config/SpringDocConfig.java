package com.cas.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * SpringDoc OpenAPI 설정
 * 개발 환경에서만 활성화
 * 보안: Production 환경에서는 완전히 비활성화됨
 */
@Configuration
@Profile("development")
public class SpringDocConfig {

    /**
     * OpenAPI 메타데이터 설정
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Backend API Documentation")
                        .description("REST API 명세서 (SpringDoc OpenAPI 1.6.15 - 보안 강화)")
                        .version("1.0.0"));
    }

    /**
     * API 그룹 설정
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .pathsToMatch("/**")
                .build();
    }
}

