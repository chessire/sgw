package com.cas.web.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Root Application Context 설정
 */
@Configuration
@ComponentScan(basePackages = {
    "com.cas.common.core",
    "com.cas.web.service"
})
@PropertySource("classpath:application.properties")
public class RootConfig {
}

