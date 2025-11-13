package com.cas.worker.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 작업 처리 프로세서
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProcessor {

    private final ObjectMapper objectMapper;

    @Value("${worker.task.timeout-seconds:300}")
    private int timeoutSeconds;

    @Value("${worker.task.max-retry:3}")
    private int maxRetry;

    /**
     * 일반 작업 처리
     */
    public void process(String message) {
        log.debug("Processing task: {}", message);
        
        try {
            // JSON 파싱
            Map<String, Object> task = parseTask(message);
            
            // 작업 타입 확인
            String taskType = (String) task.get("type");
            log.info("Task type: {}", taskType);
            
            // 작업 처리 시뮬레이션
            simulateWork(1000);
            
            log.info("Task processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing task", e);
            throw new RuntimeException("Task processing failed", e);
        }
    }

    /**
     * 무거운 작업 처리
     */
    public void processHeavyTask(String message) {
        log.debug("Processing heavy task: {}", message);
        
        try {
            // JSON 파싱
            Map<String, Object> task = parseTask(message);
            
            // 작업 처리 시뮬레이션 (더 긴 시간)
            log.info("Starting heavy computation...");
            simulateWork(5000);
            
            log.info("Heavy task processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing heavy task", e);
            // 재시도 로직
            retryTask(message);
        }
    }

    /**
     * 작업 재시도
     */
    private void retryTask(String message) {
        for (int i = 1; i <= maxRetry; i++) {
            try {
                log.info("Retrying task... Attempt {}/{}", i, maxRetry);
                simulateWork(1000);
                log.info("Task retry succeeded");
                return;
            } catch (Exception e) {
                log.warn("Retry attempt {} failed", i, e);
            }
        }
        log.error("Task failed after {} retries", maxRetry);
    }

    /**
     * JSON 파싱
     */
    private Map<String, Object> parseTask(String message) {
        try {
            return objectMapper.readValue(message, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse JSON, using raw message");
            return Map.of("raw", message, "type", "unknown");
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
            throw new RuntimeException("Task interrupted", e);
        }
    }
}
