package com.cas.worker.consumer;

import com.cas.common.infra.annotation.TaskConsumer;
import com.cas.common.infra.consumer.BaseTaskConsumer;
import com.cas.common.infra.heavytask.TestUserHeavyTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * TestUserHeavyTask Consumer (사용자별 순차 처리)
 *
 * Kafka에서 TestUserHeavyTask를 수신하여 Redis에 로그를 쌓고 5초 후 출력합니다.
 * HeavyTaskByUser를 상속받은 작업이므로 같은 userId는 순차적으로 처리됩니다.
 */
@Slf4j
@TaskConsumer(description = "TestUserHeavyTask를 처리하는 Consumer (사용자별 순차 처리)")
@RequiredArgsConstructor
public class TestUserHeavyTaskConsumer extends BaseTaskConsumer<TestUserHeavyTask> {

    private final StringRedisTemplate redisTemplate;
    
    private static final String REDIS_LOG_KEY = "user-test-log";

    /**
     * Kafka Listener: user-test-tasks 토픽에서 메시지 수신
     */
    @KafkaListener(topics = "user-test-tasks", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        handleMessage(message, TestUserHeavyTask.class);
    }

    /**
     * TestUserHeavyTask 처리 로직
     */
    @Override
    protected void processTask(TestUserHeavyTask task) throws Exception {
        log.info("========================================");
        log.info("  Processing TestUserHeavyTask");
        log.info("========================================");
        log.info("Task ID       : {}", task.getTaskId());
        log.info("User ID       : {}", task.getUserId());
        log.info("User Index    : {}", task.getUserIndex());
        log.info("User Action   : {}", task.getUserAction());
        log.info("Created At    : {}", task.getCreatedAt());
        log.info("========================================");
        
        // Redis에 userIndex 로그 추가
        String logEntry = String.format("userIndex : %d\n", task.getUserIndex());
        redisTemplate.opsForValue().append(REDIS_LOG_KEY, logEntry);
        log.info("✅ Processed: userIndex={} (User ID: {})", task.getUserIndex(), task.getUserId());
    }
}

