package com.cas.common.infra.heavytask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 테스트용 무거운 작업 (사용자별 순차 처리)
 * Redis에서 TestObject를 조회하여 로그로 출력하는 작업
 * 
 * HeavyTaskByUser를 상속받아 userId 기반 순서 보장을 받습니다.
 */
public class TestTask extends HeavyTaskByUser {
    
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
    
    public TestTask() {
        super(); // HeavyTaskByUser 생성자 호출
        this.taskType = "TestTask";
        this.priority = 1;
    }
    
    public TestTask(String userId, String redisKey, String description, String requesterId) {
        super(userId); // HeavyTaskByUser(userId) 생성자 호출
        this.taskType = "TestTask";
        this.priority = 1;
        this.redisKey = redisKey;
        this.description = description;
        this.requesterId = requesterId;
    }
    
    // Deprecated constructor (하위 호환성)
    @Deprecated
    public TestTask(String redisKey, String description, String requesterId) {
        this("unknown", redisKey, description, requesterId);
    }
    
    @Override
    public void execute() {
        // Worker에서 실제 실행될 로직
        // Redis에서 TestObject 조회 및 로그 출력
        this.status = TaskStatus.PROCESSING;
    }
    
    /**
     * JSON 문자열로부터 TestTask 생성
     */
    public static TestTask fromJsonString(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            return mapper.readValue(json, TestTask.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize TestTask from JSON", e);
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
        return "TestTask{" +
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

