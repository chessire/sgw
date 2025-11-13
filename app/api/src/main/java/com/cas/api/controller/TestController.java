package com.cas.api.controller;

import com.cas.common.infra.gameobject.TestObject;
import com.cas.common.infra.heavytask.TestHeavyTask;
import com.cas.common.infra.heavytask.TestUserHeavyTask;
import com.cas.common.infra.cache.CacheService;
import com.cas.common.infra.messaging.KafkaProducerService;
import com.cas.common.web.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 테스트 컨트롤러
 * Redis + Kafka 통합 테스트를 위한 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Tag(name = "Test API", description = "Redis + Kafka 통합 테스트 API")
@Profile({"development", "local"})
public class TestController {

    private final CacheService cacheService;
    private final KafkaProducerService kafkaProducerService;
    
    private static final String REDIS_TEST_OBJECT_KEY = "test:object:latest";
    private static final String KAFKA_TEST_TOPIC = "test-tasks";
    private static final String KAFKA_USER_TEST_TOPIC = "user-test-tasks";
    
    // userId "1"의 userIndex를 관리하는 카운터
    private static final AtomicInteger userIndexCounter = new AtomicInteger(0);
    
    /**
     * Redis + Kafka 통합 테스트 API
     * 
     * 1. 랜덤 TestObject 생성
     * 2. Redis에 저장 (Object 자동 JSON 변환)
     * 3. TestHeavyTask를 Kafka 메시지 큐에 전송
     * 4. Worker가 Kafka 메시지를 받아 Redis에서 TestObject 조회 및 로그 출력
     */
    @PostMapping
    @Operation(summary = "Redis + Kafka 통합 테스트 (일반 Task)", description = "랜덤 TestObject를 생성하여 Redis에 저장 (자동 JSON 변환)하고, TestHeavyTask를 Kafka로 전송합니다. (순차성 보장 X, 빠른 병렬 처리)")
    public ApiResponse<Map<String, Object>> test() {
        log.info("========================================");
        log.info("  Test API Called - Redis + Kafka Test (General Task)");
        log.info("========================================");
        
        try {
            // 1. 랜덤 TestObject 생성
            log.info("Step 1: Creating random TestObject...");
            TestObject testObject = new TestObject();
            testObject.initialize();
            log.info("TestObject created: {}", testObject);
            
            // 2. Redis에 저장 (Object 자동 JSON 변환, 기존 데이터 삭제)
            log.info("Step 2: Saving to Redis (key: {})...", REDIS_TEST_OBJECT_KEY);
            cacheService.delete(REDIS_TEST_OBJECT_KEY); // 기존 데이터 삭제
            cacheService.setObject(REDIS_TEST_OBJECT_KEY, testObject); // ✨ Object 자동 JSON 변환
            log.info("Saved to Redis successfully!");
            
            // 3. TestHeavyTask 생성 (일반 Task - 순차성 보장 X)
            log.info("Step 4: Creating TestHeavyTask (General Task)...");
            TestHeavyTask testHeavyTask = new TestHeavyTask(
                REDIS_TEST_OBJECT_KEY,
                "Process TestObject from Redis (No User Sequencing)",
                "API-Server"
            );
            log.info("TestHeavyTask created: {}", testHeavyTask);
            
            // 4. Kafka로 TestHeavyTask 전송 (key 없음 - 랜덤 파티션)
            log.info("Step 4: Sending TestHeavyTask to Kafka (topic: {})...", KAFKA_TEST_TOPIC);
            String taskJson = testHeavyTask.toJsonString();
            kafkaProducerService.send(KAFKA_TEST_TOPIC, null, taskJson); // key 없음 (순차성 불필요)
            log.info("TestHeavyTask sent to Kafka successfully!");
            
            // 5. 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("testObject", testObject);
            responseData.put("redisKey", REDIS_TEST_OBJECT_KEY);
            responseData.put("kafkaTopic", KAFKA_TEST_TOPIC);
            responseData.put("taskId", testHeavyTask.getTaskId());
            responseData.put("taskType", "TestHeavyTask (General Task)");
            responseData.put("sequencing", "NO (Fast Parallel Processing)");
            responseData.put("message", "TestObject saved to Redis and TestHeavyTask sent to Kafka!");
            
            log.info("========================================");
            log.info("  Test API Completed Successfully!");
            log.info("========================================");
            
            return ApiResponse.success(responseData);
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("  Test API Failed!");
            log.error("========================================", e);
            
            return ApiResponse.error("Test API failed: " + e.getMessage());
        }
    }
    
    /**
     * 사용자별 순차 처리 테스트 API (1000개 생성)
     * 
     * userId "1"로 고정하여 TestUserHeavyTask 1000개를 생성하고 Kafka로 전송합니다.
     * userIndex는 0부터 시작하여 자동으로 증가합니다.
     * Worker는 순차적으로 처리하여 userIndex 순서를 보장합니다.
     */
    @PostMapping("/user-test")
    @Operation(summary = "사용자별 순차 처리 테스트 (1000개)", description = "userId=1로 고정하여 TestUserHeavyTask 1000개를 생성하고 Kafka로 전송합니다.")
    public ApiResponse<Map<String, Object>> userTest() {
        log.info("========================================");
        log.info("  User Test API Called - Creating 1000 TestUserHeavyTasks");
        log.info("========================================");
        
        try {
            String userId = "1";
            int startIndex = userIndexCounter.get();
            int count = 1000;
            
            log.info("Creating {} TestUserHeavyTasks...", count);
            log.info("Starting userIndex: {}", startIndex);
            
            // Phase 1: 1000개의 TestUserHeavyTask 미리 생성
            log.info("Phase 1: Generating {} tasks in memory...", count);
            long startTime = System.currentTimeMillis();
            List<TestUserHeavyTask> tasks = new ArrayList<>(count);
            
            for (int i = 0; i < count; i++) {
                int userIndex = userIndexCounter.getAndIncrement();
                String userAction = "Action-" + userIndex;
                TestUserHeavyTask task = new TestUserHeavyTask(userId, userIndex, userAction);
                tasks.add(task);
                
                if ((i + 1) % 200 == 0) {
                    log.info("Generated: {} / {} tasks", i + 1, count);
                }
            }
            long generateTime = System.currentTimeMillis() - startTime;
            log.info("✅ Phase 1 complete: {} tasks generated in {} ms", count, generateTime);
            
            int endIndex = userIndexCounter.get() - 1;
            
            // Phase 2: 빠르게 Kafka에 전송 (Worker 소비보다 빠르게)
            log.info("Phase 2: Sending {} tasks to Kafka rapidly...", count);
            startTime = System.currentTimeMillis();
            
            for (int i = 0; i < tasks.size(); i++) {
                TestUserHeavyTask task = tasks.get(i);
                String taskJson = task.toJsonString();
                kafkaProducerService.send(KAFKA_USER_TEST_TOPIC, userId, taskJson);
                
                if ((i + 1) % 200 == 0) {
                    log.info("Sent: {} / {} tasks", i + 1, count);
                }
            }
            
            long sendTime = System.currentTimeMillis() - startTime;
            log.info("✅ Phase 2 complete: {} tasks sent in {} ms", count, sendTime);
            
            // 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("taskType", "TestUserHeavyTask");
            responseData.put("userId", userId);
            responseData.put("count", count);
            responseData.put("startIndex", startIndex);
            responseData.put("endIndex", endIndex);
            responseData.put("generateTimeMs", generateTime);
            responseData.put("sendTimeMs", sendTime);
            responseData.put("totalTimeMs", generateTime + sendTime);
            responseData.put("sequencing", "YES (User-based Sequential Processing)");
            responseData.put("kafkaTopic", KAFKA_USER_TEST_TOPIC);
            responseData.put("message", String.format("%d TestUserHeavyTasks created (userIndex: %d ~ %d) and sent to Kafka! (Generate: %dms, Send: %dms)", 
                                                      count, startIndex, endIndex, generateTime, sendTime));
            
            log.info("========================================");
            log.info("  User Test API Completed Successfully!");
            log.info("  Created: {} tasks", count);
            log.info("  userIndex range: {} ~ {}", startIndex, endIndex);
            log.info("========================================");
            
            return ApiResponse.success(responseData);
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("  User Test API Failed!");
            log.error("========================================", e);
            
            return ApiResponse.error("User Test API failed: " + e.getMessage());
        }
    }
    
    /**
     * Redis user-test-log 조회 API
     * 
     * Redis에 저장된 user-test-log를 조회합니다.
     */
    @PostMapping("/get-user-test-log")
    @Operation(summary = "사용자 테스트 로그 조회", description = "Redis에 저장된 user-test-log를 조회합니다.")
    public ApiResponse<Map<String, Object>> getUserTestLog() {
        log.info("========================================");
        log.info("  Get User Test Log API Called");
        log.info("========================================");
        
        try {
            // Redis에서 user-test-log 조회
            String redisLogKey = "user-test-log";
            String logs = cacheService.get(redisLogKey);
            
            // 로그 라인 수 계산
            int lineCount = 0;
            if (logs != null && !logs.isEmpty()) {
                lineCount = logs.split("\n").length;
            }
            
            // 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("redisKey", redisLogKey);
            responseData.put("logs", logs != null ? logs : "");
            responseData.put("lineCount", lineCount);
            responseData.put("isEmpty", logs == null || logs.isEmpty());
            responseData.put("message", lineCount > 0 
                ? String.format("Retrieved %d log entries", lineCount)
                : "No logs found");
            
            log.info("Retrieved {} log entries from Redis", lineCount);
            
            return ApiResponse.success(responseData);
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("  Get User Test Log API Failed!");
            log.error("========================================", e);
            
            return ApiResponse.error("Get User Test Log API failed: " + e.getMessage());
        }
    }
    
    /**
     * 사용자 테스트 초기화 API
     * 
     * userIndex 카운터와 Redis의 user-test-log를 초기화합니다.
     */
    @PostMapping("/clean-user-test")
    @Operation(summary = "사용자 테스트 초기화", description = "userIndex 카운터와 Redis의 user-test-log를 초기화합니다.")
    public ApiResponse<Map<String, Object>> cleanUserTest() {
        log.info("========================================");
        log.info("  Clean User Test API Called");
        log.info("========================================");
        
        try {
            // 1. userIndex 카운터 초기화
            int previousCount = userIndexCounter.getAndSet(0);
            log.info("userIndex counter reset: {} -> 0", previousCount);
            
            // 2. Redis의 user-test-log 삭제
            String redisLogKey = "user-test-log";
            cacheService.delete(redisLogKey);
            log.info("Redis user-test-log deleted");
            
            // 3. 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("previousCount", previousCount);
            responseData.put("currentCount", 0);
            responseData.put("redisLogDeleted", true);
            responseData.put("message", "User test data cleaned successfully!");
            
            log.info("========================================");
            log.info("  Clean User Test API Completed!");
            log.info("========================================");
            
            return ApiResponse.success(responseData);
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("  Clean User Test API Failed!");
            log.error("========================================", e);
            
            return ApiResponse.error("Clean User Test API failed: " + e.getMessage());
        }
    }
}

