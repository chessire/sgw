package com.example.batch.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 데이터 동기화 배치 작업
 */
@Slf4j
@Component
public class DataSyncJob {

    /**
     * 데이터 동기화 실행
     */
    public void execute() {
        log.info("Starting data synchronization...");
        
        try {
            // Step 1: 데이터 조회
            log.debug("Step 1: Fetching data from source...");
            simulateWork(500);
            
            // Step 2: 데이터 변환
            log.debug("Step 2: Transforming data...");
            simulateWork(300);
            
            // Step 3: 데이터 저장
            log.debug("Step 3: Saving data to destination...");
            simulateWork(200);
            
            log.info("Data synchronization completed successfully");
            
        } catch (Exception e) {
            log.error("Data synchronization failed", e);
            throw new RuntimeException("Data sync job failed", e);
        }
    }

    /**
     * 작업 시뮬레이션
     */
    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Job interrupted", e);
        }
    }
}
