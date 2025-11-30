package com.cas.api.service.game;

import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.dto.domain.PortfolioDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 랭킹 및 점수 계산 Service
 * - 재무관리능력 (40%)
 * - 리스크관리 (30%)
 * - 절대수익률 (30%)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {
    
    /**
     * 최종 점수 계산
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     * @param initialCash 초기 자본
     * @return 점수 정보 (0~100점)
     */
    public ScoreResult calculateScore(GameSessionDto session, PortfolioDto portfolio, long initialCash) {
        log.info("Calculating score: uid={}, mode={}", session.getUid(), session.getGameMode());
        
        // 1. 재무관리능력 (40점)
        double financialManagement = calculateFinancialManagement(session, portfolio);
        
        // 2. 리스크관리 (30점)
        double riskManagement = calculateRiskManagement(session, portfolio);
        
        // 3. 절대수익률 (30점)
        double returnRate = calculateReturnRate(portfolio, initialCash);
        
        // 총점
        double totalScore = financialManagement + riskManagement + returnRate;
        
        log.info("Score calculated: uid={}, total={}, financial={}, risk={}, return={}", 
            session.getUid(), totalScore, financialManagement, riskManagement, returnRate);
        
        return ScoreResult.builder()
            .totalScore(totalScore)
            .financialManagementScore(financialManagement)
            .riskManagementScore(riskManagement)
            .returnRateScore(returnRate)
            .build();
    }
    
    /**
     * 재무관리능력 계산 (40점)
     * - 자산 배분의 적절성
     * - NPC 조언 활용도
     * - 심화정보 구매 여부
     */
    private double calculateFinancialManagement(GameSessionDto session, PortfolioDto portfolio) {
        double score = 0.0;
        
        // 1. 자산 배분 점수 (25점)
        // 현금성 자산: 20~40% 적정, 투자 자산: 60~80% 적정
        if (portfolio.getAllocation() != null) {
            com.cas.api.dto.domain.AllocationDto allocation = portfolio.getAllocation();
            
            // 현금성 자산 비율 = 현금 + 예금 + 적금 + 채권
            double cashLikeRatio = 0.0;
            if (allocation.getCashRatio() != null) cashLikeRatio += allocation.getCashRatio().doubleValue();
            if (allocation.getDepositRatio() != null) cashLikeRatio += allocation.getDepositRatio().doubleValue();
            if (allocation.getSavingRatio() != null) cashLikeRatio += allocation.getSavingRatio().doubleValue();
            if (allocation.getBondRatio() != null) cashLikeRatio += allocation.getBondRatio().doubleValue();
            
            // 투자 자산 비율 = 주식 + 펀드 + 연금
            double investmentRatio = 0.0;
            if (allocation.getStockRatio() != null) investmentRatio += allocation.getStockRatio().doubleValue();
            if (allocation.getFundRatio() != null) investmentRatio += allocation.getFundRatio().doubleValue();
            if (allocation.getPensionRatio() != null) investmentRatio += allocation.getPensionRatio().doubleValue();
            
            // 적정 범위 내에 있으면 25점, 벗어날수록 감점
            if (cashLikeRatio >= 0.2 && cashLikeRatio <= 0.4 && investmentRatio >= 0.6 && investmentRatio <= 0.8) {
                score += 25.0;
            } else if (cashLikeRatio >= 0.1 && cashLikeRatio <= 0.5 && investmentRatio >= 0.5 && investmentRatio <= 0.9) {
                score += 20.0;
            } else {
                score += 15.0;
            }
        } else {
            score += 15.0; // 기본 점수
        }
        
        // 2. NPC 조언 활용 (10점)
        int adviceUsed = session.getAdviceUsedCount() != null ? session.getAdviceUsedCount() : 0;
        if (adviceUsed >= 2) {
            score += 10.0;
        } else if (adviceUsed == 1) {
            score += 5.0;
        }
        // 0회면 0점
        
        // 3. 심화정보 구매 (5점) - 추후 구현
        // TODO: 심화정보 구매 횟수 추적
        score += 2.5; // 임시 기본 점수
        
        return Math.min(score, 40.0);
    }
    
    /**
     * 리스크관리 계산 (30점)
     * - 보험 가입 여부
     * - 대출 사용 여부 (적절한 사용)
     * - 불법사금융 사용 여부 (감점)
     */
    private double calculateRiskManagement(GameSessionDto session, PortfolioDto portfolio) {
        double score = 0.0;
        
        // 1. 보험 가입 (15점)
        if (Boolean.TRUE.equals(session.getInsuranceSubscribed())) {
            score += 15.0;
        }
        
        // 2. 대출 사용 (10점)
        // - 대출 안 쓴 경우: 10점 (자력으로 해결)
        // - 일반 대출 사용: 7점 (적절한 금융 활용)
        // - 불법사금융 사용: 0점 (큰 감점)
        if (Boolean.TRUE.equals(session.getIllegalLoanUsed())) {
            score += 0.0; // 불법사금융 사용 시 0점
        } else if (Boolean.TRUE.equals(session.getLoanUsed())) {
            score += 7.0; // 일반 대출 사용
        } else {
            score += 10.0; // 대출 안 씀
        }
        
        // 3. 부채 비율 (5점)
        // 부채가 총자산의 20% 미만이면 5점, 그 이상이면 감점
        long totalAssets = portfolio.getTotalAssets() != null ? portfolio.getTotalAssets() : 0L;
        long totalLiabilities = portfolio.getTotalLiabilities() != null ? portfolio.getTotalLiabilities() : 0L;
        
        if (totalAssets > 0) {
            double debtRatio = (double) totalLiabilities / totalAssets;
            if (debtRatio < 0.2) {
                score += 5.0;
            } else if (debtRatio < 0.4) {
                score += 3.0;
            } else {
                score += 1.0;
            }
        } else {
            score += 2.5; // 기본 점수
        }
        
        return Math.min(score, 30.0);
    }
    
    /**
     * 절대수익률 계산 (30점)
     * - (최종 순자산 - 초기 자본) / 초기 자본 * 100
     * - 수익률 30% 이상: 30점
     * - 수익률 0~30%: 비례 배점
     * - 수익률 마이너스: 0점
     */
    private double calculateReturnRate(PortfolioDto portfolio, long initialCash) {
        long netWorth = portfolio.getNetWorth() != null ? portfolio.getNetWorth() : 0L;
        
        if (initialCash <= 0) {
            return 0.0;
        }
        
        // 수익률 계산 (%)
        double returnRate = ((double)(netWorth - initialCash) / initialCash) * 100.0;
        
        double score = 0.0;
        
        if (returnRate >= 30.0) {
            score = 30.0;
        } else if (returnRate > 0) {
            // 0~30% 사이는 비례 배점
            score = (returnRate / 30.0) * 30.0;
        } else {
            // 마이너스면 0점
            score = 0.0;
        }
        
        log.debug("Return rate: {}%, score: {}", returnRate, score);
        
        return score;
    }
    
    /**
     * 점수 결과 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ScoreResult {
        private Double totalScore;              // 총점 (0~100)
        private Double financialManagementScore; // 재무관리능력 (0~40)
        private Double riskManagementScore;      // 리스크관리 (0~30)
        private Double returnRateScore;          // 절대수익률 (0~30)
    }
}

