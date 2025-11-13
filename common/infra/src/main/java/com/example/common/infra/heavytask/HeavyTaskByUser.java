package com.example.common.infra.heavytask;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 사용자별 순차 처리가 필요한 HeavyTask
 * 
 * userId 필드를 기반으로 작업 순서를 보장합니다.
 * - 같은 userId를 가진 작업은 반드시 순차적으로 처리됨
 * - Redis TaskQueueService를 통한 동시성 제어
 * 
 * 사용 예시:
 * <pre>
 * public class UserDataSyncTask extends HeavyTaskByUser {
 *     // 사용자별로 순차 처리되어야 하는 데이터 동기화 작업
 * }
 * 
 * public class ChatMessageTask extends HeavyTaskByUser {
 *     // 사용자별로 메시지 순서를 보장해야 하는 작업
 * }
 * </pre>
 * 
 * vs HeavyTask:
 * - HeavyTask: 순차성 보장 불필요, 빠른 병렬 처리
 * - HeavyTaskByUser: 사용자별 순차성 보장 필요
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class HeavyTaskByUser extends HeavyTask {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 작업을 요청한 사용자 ID
     * 이 필드를 기준으로 순서가 보장됩니다.
     */
    protected String userId;
    
    /**
     * 생성자
     */
    protected HeavyTaskByUser() {
        super();
    }
    
    /**
     * userId를 포함하는 생성자
     */
    protected HeavyTaskByUser(String userId) {
        super();
        this.userId = userId;
    }
    
    /**
     * userId 유효성 검증
     */
    public boolean hasValidUserId() {
        return userId != null && !userId.trim().isEmpty();
    }
}

