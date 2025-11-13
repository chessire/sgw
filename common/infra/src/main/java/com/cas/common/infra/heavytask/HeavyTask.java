package com.cas.common.infra.heavytask;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 무거운 작업을 나타내는 추상 클래스
 * Worker에서 처리할 작업들은 이 클래스를 상속받아야 합니다.
 * 
 * Jackson Polymorphic Deserialization 사용:
 * - @JsonTypeInfo: taskType 필드를 discriminator로 사용
 * - @JsonSubTypes: 하위 클래스 등록
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "taskType",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TestTask.class, name = "TestTask"),
    @JsonSubTypes.Type(value = TestHeavyTask.class, name = "TestHeavyTask"),
    @JsonSubTypes.Type(value = TestUserHeavyTask.class, name = "TestUserHeavyTask"),
    @JsonSubTypes.Type(value = HeavyTaskByUser.class, name = "HeavyTaskByUser")
})
public abstract class HeavyTask implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 태스크 고유 ID
     */
    protected String taskId;
    
    /**
     * 태스크 타입
     */
    protected String taskType;
    
    /**
     * 태스크 우선순위 (높을수록 우선)
     */
    protected int priority;
    
    /**
     * 태스크 생성 시간
     */
    protected LocalDateTime createdAt;
    
    /**
     * 태스크 상태 (PENDING, PROCESSING, COMPLETED, FAILED)
     */
    protected TaskStatus status;
    
    /**
     * 재시도 횟수
     */
    protected int retryCount;
    
    /**
     * 최대 재시도 횟수
     */
    protected int maxRetries;
    
    /**
     * HeavyTask 생성자
     */
    protected HeavyTask() {
        this.taskId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
        this.retryCount = 0;
        this.maxRetries = 3;
        this.priority = 0;
    }
    
    /**
     * 태스크를 실행합니다.
     */
    public abstract void execute();
    
    /**
     * 태스크를 JSON 문자열로 변환합니다.
     */
    public String toJsonString() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize HeavyTask to JSON", e);
        }
    }
    
    /**
     * 재시도 가능 여부를 확인합니다.
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }
    
    /**
     * 재시도 횟수를 증가시킵니다.
     */
    public void incrementRetry() {
        this.retryCount++;
    }
    
    // Getters and Setters
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getTaskType() {
        return taskType;
    }
    
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    /**
     * 태스크 상태 열거형
     */
    public enum TaskStatus {
        PENDING,      // 대기 중
        PROCESSING,   // 처리 중
        COMPLETED,    // 완료
        FAILED        // 실패
    }
}

