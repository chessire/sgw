package com.cas.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 배치 애플리케이션 진입점
 */
@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableScheduling
public class BatchApplication {

    public static void main(String[] args) {
        log.info("========================================");
        log.info("  Batch Application Starting...");
        log.info("========================================");
        
        SpringApplication.run(BatchApplication.class, args);
        
        log.info("========================================");
        log.info("  Batch Application Started Successfully!");
        log.info("========================================");
    }
}
