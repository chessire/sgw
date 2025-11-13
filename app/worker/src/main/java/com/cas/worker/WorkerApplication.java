package com.cas.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * 워커 애플리케이션 진입점
 */
@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableKafka
@ComponentScan(
    basePackages = {
        "com.example.worker",
        "com.example.common.infra"
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
            classes = {
                com.cas.common.infra.config.KafkaConfig.class  // Spring Boot Auto-config 사용
            }
        )
    }
)
public class WorkerApplication {

    public static void main(String[] args) {
        log.info("========================================");
        log.info("  Worker Application Starting...");
        log.info("========================================");
        
        SpringApplication.run(WorkerApplication.class, args);
        
        log.info("========================================");
        log.info("  Worker Application Started Successfully!");
        log.info("========================================");
    }
}
