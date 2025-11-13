package com.cas.api.config;

import com.cas.common.web.config.WebMvcConfig;
import com.cas.common.web.filter.LoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

/**
 * Web Application Context 설정
 * SpringDoc OpenAPI 사용 (순수 Spring Framework)
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
    "com.cas.common.web",
    "com.cas.api.controller",
    "com.cas.api.config"
})
@Import(WebMvcConfig.class)
public class WebConfig {

    @Bean
    public LoggingFilter loggingFilter() {
        return new LoggingFilter();
    }
}

