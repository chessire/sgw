package com.cas.api.config;

import com.cas.common.infra.config.KafkaConfig;
import com.cas.common.infra.config.RedisConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * Root Application Context 설정
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {
    "com.cas.common.core",
    "com.cas.common.infra",
    "com.cas.api.service"
})
@PropertySource("classpath:application.properties")
@Import({RedisConfig.class, KafkaConfig.class})
public class RootConfig {
    // Redis와 Kafka 설정 Import됨
    // CacheService와 KafkaProducerService는 ComponentScan으로 자동 등록
}

