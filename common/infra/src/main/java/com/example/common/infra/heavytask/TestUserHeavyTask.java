package com.example.common.infra.heavytask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 테스트용 사용자별 순차 HeavyTask
 * 
 * HeavyTaskByUser를 상속받아 userId 기반 순서 보장을 받습니다.
 * userIndex를 통해 순차 처리 테스트를 수행합니다.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TestUserHeavyTask extends HeavyTaskByUser {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 사용자 액션 인덱스 (순차성 테스트용)
     */
    private int userIndex;
    
    /**
     * 사용자 액션 설명
     */
    private String userAction;
    
    public TestUserHeavyTask() {
        super();
        this.taskType = "TestUserHeavyTask";
        this.priority = 1;
    }
    
    public TestUserHeavyTask(String userId, int userIndex, String userAction) {
        super(userId);
        this.taskType = "TestUserHeavyTask";
        this.priority = 1;
        this.userIndex = userIndex;
        this.userAction = userAction;
    }
    
    @Override
    public void execute() {
        // Worker에서 실제 실행될 로직
        this.status = TaskStatus.PROCESSING;
    }
    
    /**
     * JSON 문자열로부터 TestUserHeavyTask 생성
     */
    public static TestUserHeavyTask fromJsonString(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            return mapper.readValue(json, TestUserHeavyTask.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize TestUserHeavyTask from JSON", e);
        }
    }
    
    @Override
    public String toString() {
        return "TestUserHeavyTask{" +
                "taskId='" + taskId + '\'' +
                ", taskType='" + taskType + '\'' +
                ", userId='" + userId + '\'' +
                ", userIndex=" + userIndex +
                ", userAction='" + userAction + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", retryCount=" + retryCount +
                '}';
    }
}

