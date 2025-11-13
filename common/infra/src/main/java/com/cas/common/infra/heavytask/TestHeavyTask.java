package com.cas.common.infra.heavytask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 테스트용 일반 HeavyTask (순차성 보장 불필요)
 * 
 * HeavyTask를 직접 상속받아 빠른 병렬 처리를 수행합니다.
 * Redis에서 TestObject를 조회하여 로그로 출력하는 작업
 * 
 * vs TestTask:
 * - TestTask: HeavyTaskByUser 상속, 사용자별 순차 처리
 * - TestHeavyTask: HeavyTask 상속, 빠른 병렬 처리
 */
public class TestHeavyTask extends HeavyTask {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Redis 키 (TestObject가 저장된 키)
     */
    private String redisKey;
    
    /**
     * 작업 설명
     */
    private String description;
    
    /**
     * 요청자 ID
     */
    private String requesterId;
    
    public TestHeavyTask() {
        super();
        this.taskType = "TestHeavyTask";
        this.priority = 1;
    }
    
    public TestHeavyTask(String redisKey, String description, String requesterId) {
        this();
        this.redisKey = redisKey;
        this.description = description;
        this.requesterId = requesterId;
    }
    
    @Override
    public void execute() {
        // Worker에서 실제 실행될 로직
        // Redis에서 TestObject 조회 및 로그 출력
        this.status = TaskStatus.PROCESSING;
    }
    
    /**
     * JSON 문자열로부터 TestHeavyTask 생성
     */
    public static TestHeavyTask fromJsonString(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            return mapper.readValue(json, TestHeavyTask.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize TestHeavyTask from JSON", e);
        }
    }
    
    // Getters and Setters
    
    public String getRedisKey() {
        return redisKey;
    }
    
    public void setRedisKey(String redisKey) {
        this.redisKey = redisKey;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRequesterId() {
        return requesterId;
    }
    
    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }
    
    @Override
    public String toString() {
        return "TestHeavyTask{" +
                "taskId='" + taskId + '\'' +
                ", taskType='" + taskType + '\'' +
                ", redisKey='" + redisKey + '\'' +
                ", description='" + description + '\'' +
                ", requesterId='" + requesterId + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", retryCount=" + retryCount +
                '}';
    }
}

