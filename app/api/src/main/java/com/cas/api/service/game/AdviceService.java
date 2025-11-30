package com.cas.api.service.game;

import com.cas.api.constant.GameConstants;
import com.cas.api.dto.domain.GameSessionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * NPC 조언 Service
 * - 조언 사용 가능 여부 확인
 * - 조언 횟수 관리 (최대 3회)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdviceService {
    
    private final GameSessionService gameSessionService;
    
    /**
     * 조언 사용 가능 여부 확인
     * 
     * @param session 게임 세션
     * @return 사용 가능 여부
     */
    public boolean canUseAdvice(GameSessionDto session) {
        int usedCount = session.getAdviceUsedCount() != null ? session.getAdviceUsedCount() : 0;
        boolean available = usedCount < GameConstants.MAX_ADVICE_COUNT;
        
        if (!available) {
            log.warn("Advice not available: uid={}, usedCount={}", 
                session.getUid(), usedCount);
        }
        
        return available;
    }
    
    /**
     * 조언 사용 처리
     * 
     * @param session 게임 세션
     * @return 성공 여부
     */
    public boolean useAdvice(GameSessionDto session) {
        if (!canUseAdvice(session)) {
            return false;
        }
        
        // 조언 횟수 증가
        boolean success = gameSessionService.useAdvice(session);
        
        if (success) {
            log.info("Advice used: uid={}, count={}", 
                session.getUid(), session.getAdviceUsedCount());
        }
        
        return success;
    }
    
    /**
     * 남은 조언 횟수 조회
     * 
     * @param session 게임 세션
     * @return 남은 횟수
     */
    public int getRemainingAdviceCount(GameSessionDto session) {
        int usedCount = session.getAdviceUsedCount() != null ? session.getAdviceUsedCount() : 0;
        int remaining = GameConstants.MAX_ADVICE_COUNT - usedCount;
        
        log.debug("Remaining advice count: uid={}, remaining={}", 
            session.getUid(), remaining);
        
        return remaining;
    }
    
    /**
     * NPC 조언 내용 조회 (game_data.json에서)
     * 
     * @param npcType NPC 타입 (포용이/채우미)
     * @param adviceKey 조언 키
     * @return 조언 내용 (실제로는 프론트엔드가 game_data.json에서 조회)
     */
    public String getAdviceContent(String npcType, String adviceKey) {
        log.debug("Getting advice content: npcType={}, adviceKey={}", npcType, adviceKey);
        
        // 실제 조언 내용은 프론트엔드의 game_data.json에 있음
        // 백엔드는 조언 사용 횟수만 관리
        return adviceKey;
    }
}

