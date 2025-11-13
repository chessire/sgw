package com.cas.common.infra.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Task Queue Service
 * 
 * 사용자별 작업 순서 보장을 위한 큐 관리 서비스
 * 
 * Redis 데이터 구조:
 * - occupiedUserIds (Set): 현재 작업 중인 사용자 ID 집합
 * - userTaskQueue:{userId} (List): 사용자별 대기 중인 작업 큐
 * 
 * 동작 방식:
 * 1. tryAcquire(): userId가 사용 가능한지 확인하고 점유
 * 2. enqueue(): 사용 중이면 대기 큐에 작업 추가
 * 3. release(): 작업 완료 후 다음 대기 작업 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskQueueService {

    private final StringRedisTemplate redisTemplate;

    private static final String OCCUPIED_USERS_KEY = "task:occupied:users";
    private static final String USER_QUEUE_PREFIX = "task:queue:";
    private static final long OCCUPATION_TTL_SECONDS = 300; // 5분

    /**
     * 사용자 ID를 점유 시도
     * 
     * @param userId 사용자 ID
     * @return 점유 성공 여부
     */
    public boolean tryAcquire(String userId) {
        try {
            Long added = redisTemplate.opsForSet().add(OCCUPIED_USERS_KEY, userId);
            
            if (added != null && added > 0) {
                // TTL 설정 (메모리 누수 방지) - 반환값 무시
                redisTemplate.expire(getUserQueueKey(userId), OCCUPATION_TTL_SECONDS, TimeUnit.SECONDS);
                log.debug("User {} acquired for task processing", userId);
                return true;
            }
            
            log.debug("User {} is already occupied", userId);
            return false;
            
        } catch (Exception e) {
            log.error("Failed to acquire user: {}", userId, e);
            return false;
        }
    }

    /**
     * 작업을 사용자별 대기 큐에 추가
     * 
     * @param userId 사용자 ID
     * @param taskJson 작업 JSON (HeavyTask 직렬화)
     */
    public void enqueue(String userId, String taskJson) {
        try {
            String queueKey = getUserQueueKey(userId);
            redisTemplate.opsForList().rightPush(queueKey, taskJson);
            
            // 큐 TTL 설정
            redisTemplate.expire(queueKey, OCCUPATION_TTL_SECONDS, TimeUnit.SECONDS);
            
            long queueSize = redisTemplate.opsForList().size(queueKey);
            log.info("Task enqueued for user {}. Queue size: {}", userId, queueSize);
            
        } catch (Exception e) {
            log.error("Failed to enqueue task for user: {}", userId, e);
        }
    }

    /**
     * 사용자 점유 해제 및 다음 대기 작업 반환
     * 
     * @param userId 사용자 ID
     * @return 다음 대기 중인 작업 JSON (없으면 null)
     */
    public String release(String userId) {
        try {
            String queueKey = getUserQueueKey(userId);
            
            // 대기 중인 작업 확인
            String nextTaskJson = redisTemplate.opsForList().leftPop(queueKey);
            
            if (nextTaskJson == null) {
                // 대기 작업 없음 → 점유 해제
                redisTemplate.opsForSet().remove(OCCUPIED_USERS_KEY, userId);
                log.debug("User {} released (no more tasks)", userId);
                return null;
            } else {
                // 대기 작업 있음 → 점유 유지
                log.info("User {} has next task in queue", userId);
                return nextTaskJson;
            }
            
        } catch (Exception e) {
            log.error("Failed to release user: {}", userId, e);
            // 에러 발생 시 점유 해제
            redisTemplate.opsForSet().remove(OCCUPIED_USERS_KEY, userId);
            return null;
        }
    }

    /**
     * 특정 사용자가 현재 점유 중인지 확인
     * 
     * @param userId 사용자 ID
     * @return 점유 여부
     */
    public boolean isOccupied(String userId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(OCCUPIED_USERS_KEY, userId));
        } catch (Exception e) {
            log.error("Failed to check occupation status for user: {}", userId, e);
            return false;
        }
    }

    /**
     * 특정 사용자의 대기 큐 크기 조회
     * 
     * @param userId 사용자 ID
     * @return 대기 중인 작업 수
     */
    public long getQueueSize(String userId) {
        try {
            String queueKey = getUserQueueKey(userId);
            Long size = redisTemplate.opsForList().size(queueKey);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get queue size for user: {}", userId, e);
            return 0;
        }
    }

    /**
     * 사용자별 큐 키 생성
     */
    private String getUserQueueKey(String userId) {
        return USER_QUEUE_PREFIX + userId;
    }

    /**
     * 디버깅용: 현재 점유 중인 모든 사용자 조회
     */
    public java.util.Set<String> getOccupiedUsers() {
        try {
            return redisTemplate.opsForSet().members(OCCUPIED_USERS_KEY);
        } catch (Exception e) {
            log.error("Failed to get occupied users", e);
            return java.util.Collections.emptySet();
        }
    }

    /**
     * 강제로 사용자 점유 해제 (관리자 기능)
     */
    public void forceRelease(String userId) {
        try {
            redisTemplate.opsForSet().remove(OCCUPIED_USERS_KEY, userId);
            String queueKey = getUserQueueKey(userId);
            redisTemplate.delete(queueKey);
            log.warn("User {} forcefully released", userId);
        } catch (Exception e) {
            log.error("Failed to force release user: {}", userId, e);
        }
    }
}

