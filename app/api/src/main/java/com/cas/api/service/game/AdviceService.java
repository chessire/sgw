package com.cas.api.service.game;

import com.cas.api.constant.GameConstants;
import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.enums.GameMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * NPC 조언 Service
 * - 조언 사용 가능 여부 확인
 * - 조언 횟수 관리 (최대 3회)
 * - 다음 라운드 힌트 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdviceService {
    
    private final GameSessionService gameSessionService;
    
    // NPC 타입
    private static final String NPC_POYONGI = "POYONGI";   // 포용이 (1)
    private static final String NPC_CHAEUMI = "CHAEUMI";   // 채우미 (2)
    
    // 힌트 타입
    private static final String HINT_STOCK = "STOCK";
    private static final String HINT_DEPOSIT = "DEPOSIT";
    private static final String HINT_FUND = "FUND";
    private static final String HINT_BOND = "BOND";
    
    // 예측 방향
    private static final String PREDICTION_UP = "상승";
    private static final String PREDICTION_DOWN = "하락";
    private static final String PREDICTION_STABLE = "보합";
    
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
    
    /**
     * 다음 라운드 힌트 생성
     * 
     * @param session 게임 세션
     * @param currentRoundNo 현재 라운드 번호
     * @param gameMode 게임 모드 (TUTORIAL/COMPETITION)
     * @return 힌트 데이터
     */
    public HintResult generateHint(GameSessionDto session, Integer currentRoundNo, GameMode gameMode) {
        log.info("Generating hint: uid={}, round={}, mode={}", 
            session.getUid(), currentRoundNo, gameMode);
        
        // NPC 타입 결정
        String npcType = session.getNpcType();
        if (npcType == null) {
            npcType = NPC_POYONGI; // 기본값
        }
        
        // 조언 사용 횟수에 따른 힌트 타입 결정
        int adviceUsedCount = session.getAdviceUsedCount() != null ? session.getAdviceUsedCount() : 0;
        
        // 힌트 타입 선택 (조언 횟수에 따라 다른 타입)
        String hintType = selectHintType(adviceUsedCount, gameMode);
        
        // 힌트 키 생성 (예: CHAEUMI_TYPE_2_STOCK_HINT)
        String hintKey = String.format("%s_TYPE_%d_%s_HINT", 
            npcType.toUpperCase(), 
            adviceUsedCount + 1, 
            hintType);
        
        // 다음 라운드 번호
        int nextRound = currentRoundNo + 1;
        
        // 힌트 대상 및 예측 생성 (라운드 기반)
        HintTarget hintTarget = generateHintTarget(hintType, nextRound, gameMode);
        
        HintResult result = new HintResult(
            hintKey,
            hintType,
            hintTarget.getTarget(),
            hintTarget.getPrediction(),
            nextRound,
            npcType
        );
        
        log.info("Generated hint: key={}, target={}, prediction={}", 
            hintKey, hintTarget.getTarget(), hintTarget.getPrediction());
        
        return result;
    }
    
    /**
     * 힌트 타입 선택 (조언 횟수에 따라)
     */
    private String selectHintType(int adviceUsedCount, GameMode gameMode) {
        // 조언 횟수에 따라 다른 타입의 힌트 제공
        List<String> hintTypes = Arrays.asList(HINT_STOCK, HINT_FUND, HINT_BOND, HINT_DEPOSIT);
        return hintTypes.get(adviceUsedCount % hintTypes.size());
    }
    
    /**
     * 힌트 대상 및 예측 생성
     */
    private HintTarget generateHintTarget(String hintType, int nextRound, GameMode gameMode) {
        // 힌트 타입에 따른 대상 목록 (실제 게임 데이터와 연동 필요)
        Map<String, List<String>> targets = new HashMap<>();
        targets.put(HINT_STOCK, Arrays.asList("에버금융", "테크놀로지", "바이오헬스", "그린에너지", "스마트모빌리티"));
        targets.put(HINT_FUND, Arrays.asList("안정형펀드", "성장형펀드", "배당형펀드", "글로벌펀드", "인덱스펀드"));
        targets.put(HINT_BOND, Arrays.asList("국채", "회사채A", "회사채B", "지방채", "특수채"));
        targets.put(HINT_DEPOSIT, Arrays.asList("정기예금", "적금", "CMA", "MMF", "파킹통장"));
        
        List<String> targetList = targets.getOrDefault(hintType, targets.get(HINT_STOCK));
        
        // 라운드 기반으로 대상 선택 (결정적이되 랜덤하게 보이도록)
        int targetIndex = (nextRound * 7 + hintType.hashCode()) % targetList.size();
        if (targetIndex < 0) targetIndex = -targetIndex;
        String target = targetList.get(targetIndex);
        
        // 예측 방향 결정 (라운드 기반)
        String[] predictions = {PREDICTION_UP, PREDICTION_DOWN, PREDICTION_STABLE};
        int predictionIndex = (nextRound * 13 + target.hashCode()) % predictions.length;
        if (predictionIndex < 0) predictionIndex = -predictionIndex;
        String prediction = predictions[predictionIndex];
        
        return new HintTarget(target, prediction);
    }
    
    /**
     * 힌트 결과 DTO
     */
    @Data
    @AllArgsConstructor
    public static class HintResult {
        private String key;          // 힌트 키 (예: CHAEUMI_TYPE_2_STOCK_HINT)
        private String hintType;     // 힌트 타입 (STOCK/FUND/BOND/DEPOSIT)
        private String target;       // 대상 (예: 에버금융)
        private String prediction;   // 예측 (상승/하락/보합)
        private int nextRound;       // 다음 라운드 번호
        private String npcType;      // NPC 타입
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("key", key);
            map.put("hintType", hintType);
            map.put("target", target);
            map.put("prediction", prediction);
            map.put("nextRound", nextRound);
            map.put("npcType", npcType);
            return map;
        }
    }
    
    /**
     * 힌트 대상 내부 클래스
     */
    @Data
    @AllArgsConstructor
    private static class HintTarget {
        private String target;
        private String prediction;
    }
}

