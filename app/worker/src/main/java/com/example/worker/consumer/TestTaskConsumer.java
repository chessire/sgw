package com.example.worker.consumer;

import com.example.common.infra.annotation.TaskConsumer;
import com.example.common.infra.consumer.BaseTaskConsumer;
import com.example.common.infra.gameobject.GameObject;
import com.example.common.infra.gameobject.GameObjectParser;
import com.example.common.infra.gameobject.TestObject;
import com.example.common.infra.heavytask.TestTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * TestTask Consumer (사용자별 순차 작업)
 *
 * Kafka에서 TestTask를 수신하여 Redis의 TestObject를 조회하고 로그로 출력합니다.
 * BaseTaskConsumer를 상속받아 공통 로직을 재사용합니다.
 * 
 * HeavyTaskByUser를 상속받은 작업이므로:
 * - 사용자별 순차 처리 보장
 * - Redis 큐잉을 통한 동시성 제어
 * - 같은 userId는 반드시 순차적으로 처리됨
 */
@Slf4j
@TaskConsumer(description = "TestTask를 처리하는 Consumer (사용자별 순차 처리)")
@RequiredArgsConstructor
public class TestTaskConsumer extends BaseTaskConsumer<TestTask> {

    private final StringRedisTemplate redisTemplate;
    
    /**
     * Kafka Listener: test-tasks 토픽에서 메시지 수신
     */
    @KafkaListener(topics = "test-tasks", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        handleMessage(message, TestTask.class);
    }

    /**
     * TestTask 처리 로직
     * 
     * Redis에서 GameObject를 조회하여 상세 정보를 로그로 출력합니다.
     */
    @Override
    protected void processTask(TestTask task) throws Exception {
        log.info("========================================");
        log.info("  Processing TestTask: {}", task.getTaskId());
        log.info("========================================");
        
        // 1. Redis에서 GameObject 조회
        String redisKey = task.getRedisKey();
        log.info("Step 1: Fetching GameObject from Redis (key: {})...", redisKey);
        
        String gameObjectJson = redisTemplate.opsForValue().get(redisKey);
        
        if (gameObjectJson == null) {
            log.warn("GameObject not found in Redis for key: {}", redisKey);
            task.setStatus(TestTask.TaskStatus.FAILED);
            throw new IllegalStateException("GameObject not found in Redis: " + redisKey);
        }
        
        // 2. JSON → GameObject 역직렬화 (Factory Pattern)
        log.info("Step 2: Parsing GameObject from JSON using GameObjectParser...");
        log.debug("JSON data (length: {} bytes): {}", gameObjectJson.length(), 
                 gameObjectJson.substring(0, Math.min(100, gameObjectJson.length())) + "...");
        
        GameObject gameObject = GameObjectParser.parse(gameObjectJson);
        log.info("GameObject parsed: type={}, id={}", 
                 gameObject.getObjectType(), gameObject.getObjectId());
        
        // 3. TestObject로 캐스팅 (타입 체크)
        if (!(gameObject instanceof TestObject)) {
            log.warn("Expected TestObject but got: {}", gameObject.getObjectType());
            task.setStatus(TestTask.TaskStatus.FAILED);
            throw new IllegalArgumentException("Invalid GameObject type: " + gameObject.getObjectType());
        }
        
        TestObject testObject = (TestObject) gameObject;
        
        // 4. TestObject 상세 정보 로그 출력
        logTestObjectDetails(testObject);
    }

    /**
     * TestObject 상세 정보 로그 출력
     */
    private void logTestObjectDetails(TestObject testObject) {
        log.info("========================================");
        log.info("  GameObject Retrieved Successfully!");
        log.info("  Type: {} (Parsed by GameObjectParser)", testObject.getObjectType());
        log.info("========================================");
        log.info("Object ID      : {}", testObject.getObjectId());
        log.info("Player Name    : {}", testObject.getPlayerName());
        log.info("Level          : {}", testObject.getLevel());
        log.info("Health         : {}", testObject.getHealth());
        log.info("Mana           : {}", testObject.getMana());
        log.info("Position       : ({}, {}, {})", 
                 String.format("%.2f", testObject.getPositionX()),
                 String.format("%.2f", testObject.getPositionY()),
                 String.format("%.2f", testObject.getPositionZ()));
        log.info("Score          : {}", testObject.getScore());
        log.info("Experience     : {}", testObject.getExperience());
        log.info("Gold           : {}", testObject.getGold());
        log.info("Created At     : {}", testObject.getCreatedAt());
        log.info("Updated At     : {}", testObject.getUpdatedAt());
        log.info("Active         : {}", testObject.isActive());
        log.info("========================================");
    }
}

