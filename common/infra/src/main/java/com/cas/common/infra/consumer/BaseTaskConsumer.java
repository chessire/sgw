package com.cas.common.infra.consumer;

import com.cas.common.infra.heavytask.HeavyTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TaskConsumer의 추상 베이스 클래스
 * 
 * 모든 TaskConsumer는 이 클래스를 상속받아야 하며,
 * 공통 로깅, 에러 핸들링, 재시도 로직을 제공합니다.
 * 
 * @param <T> 처리할 HeavyTask의 구체적인 타입
 * 
 * 사용자별 순서 보장:
 * - HeavyTaskByUser의 userId를 Kafka 메시지 key로 사용
 * - Kafka 파티셔닝 + concurrency=1 설정으로 순서 보장
 * 
 * 사용 예시:
 * <pre>
 * {@literal @}TaskConsumer
 * public class TestTaskConsumer extends BaseTaskConsumer&lt;TestTask&gt; {
 *     
 *     {@literal @}KafkaListener(topics = "test-tasks", groupId = "${spring.kafka.consumer.group-id}")
 *     public void consume(String message) {
 *         handleMessage(message, TestTask.class);
 *     }
 *     
 *     {@literal @}Override
 *     protected void processTask(TestTask task) {
 *         // 실제 작업 처리 로직
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class BaseTaskConsumer<T extends HeavyTask> {

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Kafka 메시지를 처리하는 메인 메서드
     * 
     * 1. JSON → HeavyTask 역직렬화 (Polymorphic)
     * 2. 타입 검증
     * 3. processTask() 호출
     * 
     * 순서 보장: Kafka 파티셔닝 (userId key) + concurrency=1
     * 
     * @param message Kafka 메시지 (JSON)
     * @param taskClass 기대하는 Task의 클래스
     */
    protected void handleMessage(String message, Class<T> taskClass) {
        String taskTypeName = taskClass.getSimpleName();
        
        log.info("========================================");
        log.info("  {} Received from Kafka!", taskTypeName);
        log.info("========================================");
        
        try {
            // 1. JSON → HeavyTask 역직렬화 (Polymorphic)
            log.debug("Step 1: Deserializing HeavyTask from JSON (Polymorphic Deserialization)...");
            HeavyTask heavyTask = objectMapper.readValue(message, HeavyTask.class);
            log.debug("HeavyTask type: {}", heavyTask.getTaskType());
            
            // 2. 타입 검증
            if (!taskClass.isInstance(heavyTask)) {
                log.warn("Expected {} but received: {}. Skipping...", 
                         taskTypeName, heavyTask.getTaskType());
                return;
            }
            
            T task = taskClass.cast(heavyTask);
            log.info("{} deserialized successfully: {}", taskTypeName, task.getTaskId());
            
            // 3. 사용자별 순서 보장 로깅 (HeavyTaskByUser인 경우)
            if (task instanceof com.cas.common.infra.heavytask.HeavyTaskByUser) {
                com.cas.common.infra.heavytask.HeavyTaskByUser userTask = 
                    (com.cas.common.infra.heavytask.HeavyTaskByUser) task;
                String userId = userTask.getUserId();
                log.info("User-sequenced task detected. User: {} (Kafka partitioning ensures order)", userId);
            } else {
                log.info("General task (no user-sequencing required)");
            }
            
            // 4. 작업 처리
            processTaskWithSequence(task, message);
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("  {} Processing Failed!", taskTypeName);
            log.error("========================================", e);
            
            handleError(message, taskClass, e);
        }
    }

    /**
     * 작업 처리 로직
     */
    private void processTaskWithSequence(T task, String originalMessage) throws Exception {
        String taskTypeName = task.getClass().getSimpleName();
        String userId = null;
        
        // HeavyTaskByUser인 경우 userId 추출 (로깅용)
        if (task instanceof com.cas.common.infra.heavytask.HeavyTaskByUser) {
            userId = ((com.cas.common.infra.heavytask.HeavyTaskByUser) task).getUserId();
        }
        
        // Task 상태 업데이트
        task.setStatus(HeavyTask.TaskStatus.PROCESSING);
        
        // 실제 작업 처리
        long startTime = System.currentTimeMillis();
        beforeProcess(task);
        processTask(task);
        afterProcess(task);
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        // 완료 처리
        task.setStatus(HeavyTask.TaskStatus.COMPLETED);
        log.info("========================================");
        log.info("  {} Completed Successfully!", taskTypeName);
        log.info("  Task ID: {}", task.getTaskId());
        if (userId != null) {
            log.info("  User ID: {}", userId);
        }
        log.info("  Elapsed Time: {} ms", elapsedTime);
        log.info("========================================");
    }

    /**
     * 실제 Task 처리 로직 (하위 클래스에서 구현)
     * 
     * @param task 처리할 HeavyTask
     * @throws Exception 처리 중 발생한 예외
     */
    protected abstract void processTask(T task) throws Exception;

    /**
     * 에러 핸들링 로직
     * 
     * 하위 클래스에서 오버라이드하여 커스텀 에러 처리 가능
     * (예: 재시도, Dead Letter Queue 전송 등)
     * 
     * @param message 원본 메시지
     * @param taskClass Task 클래스
     * @param exception 발생한 예외
     */
    protected void handleError(String message, Class<T> taskClass, Exception exception) {
        log.error("Error handling for {}: {}", taskClass.getSimpleName(), exception.getMessage());
        
        // 기본 에러 핸들링 (로깅만 수행)
        // 하위 클래스에서 오버라이드하여 재시도, DLQ 전송 등 구현 가능
    }

    /**
     * Task 처리 전 실행되는 Hook 메서드
     * 
     * 필요 시 하위 클래스에서 오버라이드
     * 
     * @param task 처리할 Task
     */
    protected void beforeProcess(T task) {
        // Hook for pre-processing logic
    }

    /**
     * Task 처리 후 실행되는 Hook 메서드
     * 
     * 필요 시 하위 클래스에서 오버라이드
     * 
     * @param task 처리된 Task
     */
    protected void afterProcess(T task) {
        // Hook for post-processing logic
    }
}

