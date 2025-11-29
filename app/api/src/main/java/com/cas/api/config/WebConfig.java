package com.cas.api.config;

import com.cas.common.web.config.SecureObjectMapperConfig;
import com.cas.common.web.config.WebMvcConfig;
import com.cas.common.web.filter.LoggingFilter;
import com.cas.common.web.filter.SecurityHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Web Application Context 설정
 * SpringDoc OpenAPI 사용 (순수 Spring Framework)
 * 
 * 보안 강화:
 * - SecureObjectMapperConfig: RCE 취약점 방어
 * - SecurityHeadersFilter: 보안 헤더 및 위험한 Content-Type 차단
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
    "com.cas.common.web",
    "com.cas.api.controller",
    "com.cas.api.config"
})
@Import({WebMvcConfig.class, SecureObjectMapperConfig.class})
public class WebConfig {

    @Bean
    public LoggingFilter loggingFilter() {
        return new LoggingFilter();
    }
    
    /**
     * 보안 헤더 필터 (RCE 방어)
     */
    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }
}

