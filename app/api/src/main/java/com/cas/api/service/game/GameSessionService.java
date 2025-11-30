package com.cas.api.service.game;

import com.cas.api.constant.GameConstants;
import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.enums.GameMode;
import com.cas.common.infra.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 게임 세션 관리 Service
 * Redis를 사용하여 게임 상태를 저장/조회/업데이트/삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameSessionService {
    
    private final CacheService cacheService;
    
    /**
     * Redis 키 생성
     */
    private String generateKey(String uid, GameMode gameMode) {
        return String.format(GameConstants.REDIS_KEY_GAME_SESSION, uid, gameMode.getCode());
    }
    
    /**
     * 새로운 게임 세션 생성
     */
    public GameSessionDto createSession(String uid, GameMode gameMode, GameSessionDto initialData) {
        log.info("Creating new game session: uid={}, mode={}", uid, gameMode);
        
        String key = generateKey(uid, gameMode);
        
        // 초기 데이터 설정
        initialData.setUid(uid);
        initialData.setGameMode(gameMode);
        initialData.setStartedAt(LocalDateTime.now());
        initialData.setUpdatedAt(LocalDateTime.now());
        initialData.setCompleted(false);
        initialData.setCurrentRound(1);
        initialData.setAdviceUsedCount(0);
        initialData.setLoanUsed(false);
        initialData.setIllegalLoanUsed(false);
        initialData.setInsuranceSubscribed(false);
        initialData.setInsurableEventOccurred(false);
        
        // Redis에 저장 (24시간 TTL)
        cacheService.setObject(key, initialData, GameConstants.TTL_ACTIVE_SESSION, TimeUnit.SECONDS);
        
        log.info("Game session created successfully: {}", key);
        return initialData;
    }
    
    /**
     * 게임 세션 조회
     */
    public GameSessionDto getSession(String uid, GameMode gameMode) {
        String key = generateKey(uid, gameMode);
        log.debug("Getting game session: {}", key);
        
        GameSessionDto session = cacheService.getObject(key, GameSessionDto.class);
        
        if (session == null) {
            log.warn("Game session not found: {}", key);
            return null;
        }
        
        return session;
    }
    
    /**
     * 게임 세션 업데이트
     */
    public void updateSession(String uid, GameMode gameMode, GameSessionDto sessionData) {
        String key = generateKey(uid, gameMode);
        log.debug("Updating game session: {}", key);
        
        // 업데이트 시간 갱신
        sessionData.setUpdatedAt(LocalDateTime.now());
        
        // TTL 결정 (완료된 게임은 7일, 진행 중은 24시간)
        int ttl = sessionData.getCompleted() 
            ? GameConstants.TTL_COMPLETED_GAME 
            : GameConstants.TTL_ACTIVE_SESSION;
        
        // Redis에 저장
        cacheService.setObject(key, sessionData, ttl, TimeUnit.SECONDS);
        
        log.debug("Game session updated successfully: {}", key);
    }
    
    /**
     * 게임 세션 삭제
     */
    public void deleteSession(String uid, GameMode gameMode) {
        String key = generateKey(uid, gameMode);
        log.info("Deleting game session: {}", key);
        
        cacheService.delete(key);
        
        log.info("Game session deleted successfully: {}", key);
    }
    
    /**
     * 게임 세션 존재 여부 확인
     */
    public boolean existsSession(String uid, GameMode gameMode) {
        GameSessionDto session = getSession(uid, gameMode);
        return session != null;
    }
    
    /**
     * 라운드 증가
     */
    public void incrementRound(GameSessionDto session) {
        int currentRound = session.getCurrentRound();
        int maxRounds = session.getGameMode().getMaxRounds();
        
        if (currentRound < maxRounds) {
            session.setCurrentRound(currentRound + 1);
            log.debug("Round incremented: {} -> {}", currentRound, currentRound + 1);
        } else {
            // 마지막 라운드 완료
            session.setCompleted(true);
            log.info("Game completed: uid={}, mode={}", session.getUid(), session.getGameMode());
        }
    }
    
    /**
     * 조언 사용
     */
    public boolean useAdvice(GameSessionDto session) {
        int used = session.getAdviceUsedCount();
        
        if (used >= GameConstants.MAX_ADVICE_COUNT) {
            log.warn("Advice count exhausted: uid={}, count={}", session.getUid(), used);
            return false;
        }
        
        session.setAdviceUsedCount(used + 1);
        log.info("Advice used: uid={}, count={} -> {}", 
            session.getUid(), used, used + 1);
        return true;
    }
    
    /**
     * 보험 가입
     */
    public void subscribeInsurance(GameSessionDto session) {
        session.setInsuranceSubscribed(true);
        log.info("Insurance subscribed: uid={}", session.getUid());
    }
    
    /**
     * 보험 해지
     */
    public void cancelInsurance(GameSessionDto session) {
        session.setInsuranceSubscribed(false);
        log.info("Insurance cancelled: uid={}", session.getUid());
    }
    
    /**
     * 대출 사용
     */
    public boolean useLoan(GameSessionDto session) {
        if (session.getLoanUsed()) {
            log.warn("Loan already used: uid={}", session.getUid());
            return false;
        }
        
        session.setLoanUsed(true);
        log.info("Loan used: uid={}", session.getUid());
        return true;
    }
    
    /**
     * 불법사금융 사용 (패널티)
     */
    public void useIllegalLoan(GameSessionDto session) {
        session.setIllegalLoanUsed(true);
        log.warn("Illegal loan used: uid={}", session.getUid());
    }
    
    /**
     * 보험 처리 가능 이벤트 발생 (12라운드 중 1회만)
     */
    public boolean markInsurableEventOccurred(GameSessionDto session) {
        if (session.getInsurableEventOccurred()) {
            log.debug("Insurable event already occurred: uid={}", session.getUid());
            return false;
        }
        
        session.setInsurableEventOccurred(true);
        log.info("Insurable event occurred: uid={}", session.getUid());
        return true;
    }
}

