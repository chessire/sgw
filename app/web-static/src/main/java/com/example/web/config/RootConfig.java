package com.example.web.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Root Application Context 설정
 */
@Configuration
@ComponentScan(basePackages = {
    "com.example.common.core",
    "com.example.web.service"
})
@PropertySource("classpath:application.properties")
public class RootConfig {
}

