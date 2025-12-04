package com.cas.api.service.game;

import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.dto.domain.PortfolioDto;
import com.cas.api.dto.response.StartSettlementResultDto;
import com.cas.api.enums.GameMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 라운드 진행 Service
 * - 라운드 시작 처리
 * - 라운드 진행 처리
 * - 라운드 종료 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoundService {
    
    private final SettlementService settlementService;
    private final MarketEventService marketEventService;
    private final GameSessionService gameSessionService;
    
    /**
     * 라운드 시작 처리
     * 1. 시장 시세 업데이트 (주식, 펀드)
     * 2. 라운드 시작 정산 (월급, 생활비, 이자)
     * 3. 배당금 지급 (해당 라운드인 경우)
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     * @return 정산 결과 (자동 납입 실패 정보 포함)
     */
    public StartSettlementResultDto startRound(GameSessionDto session, PortfolioDto portfolio) {
        log.info("Starting round: uid={}, mode={}, round={}", 
            session.getUid(), session.getGameMode(), session.getCurrentRound());
        
        int currentRound = session.getCurrentRound();
        
        // 1. 시장 시세 업데이트
        marketEventService.updateStockPrices(session, portfolio);
        marketEventService.updateFundNavs(session, portfolio);
        
        // 2. 라운드 시작 정산 (자동 납입 포함)
        StartSettlementResultDto settlementResult = 
            settlementService.processStartSettlementWithAutoPayment(session, portfolio);
        
        // 3. 배당금 지급 (3라운드, 6라운드)
        if (currentRound == 3 || currentRound == 6) {
            settlementService.processDividendSettlement(portfolio, currentRound);
        }
        
        log.info("Round started: cash={}, autoPaymentFailures={}", 
            portfolio.getCash(), settlementResult.getAutoPaymentFailures().size());
        
        return settlementResult;
    }
    
    /**
     * 라운드 종료 처리
     * 1. 만기 상품 정산
     * 2. 포트폴리오 평가
     * 3. 라운드 증가
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     */
    public void endRound(GameSessionDto session, PortfolioDto portfolio) {
        log.info("Ending round: uid={}, mode={}, round={}", 
            session.getUid(), session.getGameMode(), session.getCurrentRound());
        
        int currentRound = session.getCurrentRound();
        
        // 1. 만기 상품 정산 및 포트폴리오 평가
        settlementService.processEndSettlement(portfolio, currentRound);
        
        // 2. 라운드 증가
        gameSessionService.incrementRound(session);
        
        // 3. 게임 종료 여부 확인
        if (session.getCompleted()) {
            log.info("Game completed: uid={}, mode={}", session.getUid(), session.getGameMode());
            
            // 최종 정산
            Long finalNetWorth = settlementService.processFinalSettlement(portfolio);
            log.info("Final net worth: {}", finalNetWorth);
        }
        
        log.info("Round ended: nextRound={}, completed={}", 
            session.getCurrentRound(), session.getCompleted());
    }
    
    /**
     * 현재 라운드가 유효한지 확인
     * 
     * @param session 게임 세션
     * @return 유효 여부
     */
    public boolean isValidRound(GameSessionDto session) {
        int currentRound = session.getCurrentRound();
        int maxRounds = session.getGameMode().getMaxRounds();
        
        boolean valid = currentRound >= 1 && currentRound <= maxRounds && !session.getCompleted();
        
        if (!valid) {
            log.warn("Invalid round: currentRound={}, maxRounds={}, completed={}", 
                currentRound, maxRounds, session.getCompleted());
        }
        
        return valid;
    }
    
    /**
     * 특정 라운드에서 특정 액션이 가능한지 확인
     * 
     * @param session 게임 세션
     * @param requiredRound 필요 라운드
     * @return 가능 여부
     */
    public boolean isActionAvailableInRound(GameSessionDto session, int requiredRound) {
        int currentRound = session.getCurrentRound();
        boolean available = currentRound == requiredRound;
        
        if (!available) {
            log.debug("Action not available: currentRound={}, requiredRound={}", 
                currentRound, requiredRound);
        }
        
        return available;
    }
    
    /**
     * 게임 진행률 계산
     * 
     * @param session 게임 세션
     * @return 진행률 (0.0 ~ 1.0)
     */
    public double calculateProgress(GameSessionDto session) {
        int currentRound = session.getCurrentRound();
        int maxRounds = session.getGameMode().getMaxRounds();
        
        if (session.getCompleted()) {
            return 1.0;
        }
        
        // 현재 라운드 / 전체 라운드
        double progress = (double) (currentRound - 1) / maxRounds;
        
        log.debug("Game progress: {}% ({}/{})", 
            (int)(progress * 100), currentRound - 1, maxRounds);
        
        return progress;
    }
    
    /**
     * 남은 라운드 수 계산
     * 
     * @param session 게임 세션
     * @return 남은 라운드 수
     */
    public int getRemainingRounds(GameSessionDto session) {
        if (session.getCompleted()) {
            return 0;
        }
        
        int currentRound = session.getCurrentRound();
        int maxRounds = session.getGameMode().getMaxRounds();
        
        int remaining = maxRounds - currentRound + 1;
        
        log.debug("Remaining rounds: {}", remaining);
        
        return remaining;
    }
}

