package com.example.api.config;

import com.example.common.web.config.WebMvcConfig;
import com.example.common.web.filter.LoggingFilter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Web Application Context 설정
 * SpringDoc OpenAPI 3.0 사용 (순수 Spring Framework 환경)
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
    "com.example.common.web",
    "com.example.api.controller",
    "org.springdoc"  // SpringDoc 패키지 스캔
})
@Import(WebMvcConfig.class)
public class WebConfig {

    @Bean
    public LoggingFilter loggingFilter() {
        return new LoggingFilter();
    }
}

