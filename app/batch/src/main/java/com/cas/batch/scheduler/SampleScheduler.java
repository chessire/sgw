package com.cas.batch.scheduler;

import com.cas.batch.job.DataSyncJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 스케줄러 예제
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SampleScheduler {

    private final DataSyncJob dataSyncJob;

    /**
     * 5분마다 실행되는 데이터 동기화 작업
     */
    @Scheduled(cron = "${batch.scheduler.data-sync.cron:0 */5 * * * *}")
    public void scheduleDataSync() {
        log.info("=== Scheduled Data Sync Started ===");
        
        try {
            dataSyncJob.execute();
            log.info("=== Scheduled Data Sync Completed Successfully ===");
        } catch (Exception e) {
            log.error("=== Scheduled Data Sync Failed ===", e);
        }
    }

    /**
     * 매일 자정에 실행되는 일일 배치 작업
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleDailyBatch() {
        log.info("=== Daily Batch Started ===");
        
        try {
            // 일일 배치 로직 구현
            log.info("Daily batch processing...");
            Thread.sleep(1000); // 시뮬레이션
            
            log.info("=== Daily Batch Completed Successfully ===");
        } catch (Exception e) {
            log.error("=== Daily Batch Failed ===", e);
        }
    }

    /**
     * 매시간 실행되는 시간별 배치 작업
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduleHourlyBatch() {
        log.info("=== Hourly Batch Started ===");
        
        try {
            // 시간별 배치 로직 구현
            log.info("Hourly batch processing...");
            
            log.info("=== Hourly Batch Completed Successfully ===");
        } catch (Exception e) {
            log.error("=== Hourly Batch Failed ===", e);
        }
    }
}
