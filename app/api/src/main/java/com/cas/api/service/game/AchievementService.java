package com.cas.api.service.game;

import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.enums.GameMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 업적 시스템 Service
 * 게임 플레이 중 업적 조건을 체크하고 달성 처리
 */
@Slf4j
@Service
public class AchievementService {
    
    /**
     * 업적 체크 및 달성 처리
     */
    public void checkAchievements(GameSessionDto session) {
        if (session.getAchievedAchievements() == null) {
            session.setAchievedAchievements(new HashSet<>());
        }
        if (session.getAchievementProgress() == null) {
            session.setAchievementProgress(new HashMap<>());
        }
        
        // 각 업적 체크
        checkTutorialComplete(session);          // 1. 튜토리얼 완주
        // checkBigProfit은 proceed-round에서만 호출
        checkStockMaster(session);               // 3. 주식 고수
        checkAdviceCollector(session);           // 8. 조언 수집가
        checkFinancialBeginner(session);         // 9. 금융 입문자
        checkCompoundInterest(session);          // 10. 복리의 마법
        checkNoDeptComplete(session);            // 11. 무차입 완주
        checkContinuousChallenge(session);       // 15. 연속 도전
    }
    
    /**
     * 1. 튜토리얼 완주
     */
    private void checkTutorialComplete(GameSessionDto session) {
        if (session.getGameMode() == GameMode.TUTORIAL && 
            Boolean.TRUE.equals(session.getCompleted())) {
            achieveAchievement(session, 1, "튜토리얼 완주");
        }
    }
    
    /**
     * 2. 대박 수익 (한 라운드에 500만원 이상 수익)
     * proceed-round에서 호출 필요
     */
    public void checkBigProfitInRound(GameSessionDto session, long netIncomeThisRound) {
        if (netIncomeThisRound >= 5000000) {
            achieveAchievement(session, 2, "대박 수익");
        }
    }
    
    /**
     * 3. 주식 고수 (주식으로 100% 이상 수익률)
     */
    private void checkStockMaster(GameSessionDto session) {
        if (session.getPortfolio() != null && 
            session.getPortfolio().getStocks() != null) {
            
            for (var stock : session.getPortfolio().getStocks()) {
                if (stock.getReturnRate() != null && stock.getReturnRate() >= 1.0) {
                    achieveAchievement(session, 3, "주식 고수");
                    break;
                }
            }
        }
    }
    
    /**
     * 4. 펀드 컬렉터 (5종류 펀드 투자)
     * proceed-round에서 호출
     */
    public void checkFundCollector(GameSessionDto session) {
        if (session.getPortfolio() != null && 
            session.getPortfolio().getFunds() != null) {
            
            Set<String> fundTypes = new HashSet<>();
            for (var fund : session.getPortfolio().getFunds()) {
                fundTypes.add(fund.getFundId());
            }
            
            int fundCount = fundTypes.size();
            session.getAchievementProgress().put("fundTypes", fundCount);
            
            if (fundCount >= 5) {
                achieveAchievement(session, 4, "펀드 컬렉터");
            }
        }
    }
    
    /**
     * 6. 하이 리스커 (주식/펀드 비중 80% 이상)
     * proceed-round에서 호출
     */
    public void checkHighRisker(GameSessionDto session) {
        if (session.getPortfolio() != null && 
            session.getPortfolio().getAllocation() != null) {
            
            var stockRatio = session.getPortfolio().getAllocation().getStockRatio();
            var fundRatio = session.getPortfolio().getAllocation().getFundRatio();
            
            if (stockRatio != null && fundRatio != null) {
                double riskRatio = stockRatio.doubleValue() + fundRatio.doubleValue();
                if (riskRatio >= 0.8) {
                    achieveAchievement(session, 6, "하이리스커");
                }
            }
        }
    }
    
    /**
     * 8. 조언 수집가 (NPC 조언 3회)
     */
    private void checkAdviceCollector(GameSessionDto session) {
        if (session.getAdviceUsedCount() != null && 
            session.getAdviceUsedCount() >= 3) {
            achieveAchievement(session, 8, "조언 수집가");
        }
    }
    
    /**
     * 9. 금융 입문자 (모든 금융 교육 영상 시청)
     */
    private void checkFinancialBeginner(GameSessionDto session) {
        if (Boolean.TRUE.equals(session.getDepositVideoCompleted()) &&
            Boolean.TRUE.equals(session.getStockVideoCompleted()) &&
            Boolean.TRUE.equals(session.getBondVideoCompleted()) &&
            Boolean.TRUE.equals(session.getPensionVideoCompleted()) &&
            Boolean.TRUE.equals(session.getFundVideoCompleted()) &&
            Boolean.TRUE.equals(session.getInsuranceVideoCompleted())) {
            
            achieveAchievement(session, 9, "금융 입문자");
        }
    }
    
    /**
     * 10. 복리의 마법 (예금/적금 만기 수령)
     * proceed-round에서 만기 발생 시 호출
     */
    public void checkCompoundInterestOnMaturity(GameSessionDto session) {
        if (session.getPortfolio() != null && session.getCurrentRound() != null) {
            boolean hasMaturedDeposit = false;
            boolean hasMaturedSaving = false;
            
            int currentRound = session.getCurrentRound();
            
            // 만기 도래한 예금이 있는지 확인 (현재 라운드 >= 만기 라운드)
            if (session.getPortfolio().getDeposits() != null) {
                hasMaturedDeposit = session.getPortfolio().getDeposits().stream()
                    .anyMatch(d -> d.getMaturityRound() != null && currentRound >= d.getMaturityRound());
            }
            
            // 만기 도래한 적금이 있는지 확인
            if (session.getPortfolio().getSavings() != null) {
                hasMaturedSaving = session.getPortfolio().getSavings().stream()
                    .anyMatch(s -> s.getMaturityRound() != null && currentRound >= s.getMaturityRound());
            }
            
            if (hasMaturedDeposit || hasMaturedSaving) {
                achieveAchievement(session, 10, "복리의 마법");
            }
        }
    }
    
    /**
     * 복리의 마법 체크 (게임 종료 시)
     */
    private void checkCompoundInterest(GameSessionDto session) {
        checkCompoundInterestOnMaturity(session);
    }
    
    /**
     * 11. 무차입 완주 (대출 없이 12개월 완주)
     */
    private void checkNoDeptComplete(GameSessionDto session) {
        if (Boolean.TRUE.equals(session.getCompleted()) &&
            session.getCurrentRound() != null && session.getCurrentRound() >= 12 &&
            Boolean.FALSE.equals(session.getLoanUsed())) {
            
            achieveAchievement(session, 11, "무차입 완주");
        }
    }
    
    /**
     * 15. 연속 도전 (튜토리얼 & 경쟁모드 연속 완료)
     * 이 업적은 여러 게임에 걸쳐 있으므로 DB 필요 (현재는 스킵)
     */
    private void checkContinuousChallenge(GameSessionDto session) {
        // TODO: DB 연동 시 구현
        // 현재 세션만으로는 판단 불가
    }
    
    /**
     * 16. 금융 종합 (모든 금융 상품 최소 1회 투자)
     * result 호출 시 체크
     */
    public void checkFinancialComprehensive(GameSessionDto session) {
        if (session.getPortfolio() == null) return;
        
        boolean hasDeposit = session.getPortfolio().getDeposits() != null && 
                            !session.getPortfolio().getDeposits().isEmpty();
        boolean hasSaving = session.getPortfolio().getSavings() != null && 
                           !session.getPortfolio().getSavings().isEmpty();
        boolean hasStock = session.getPortfolio().getStocks() != null && 
                          !session.getPortfolio().getStocks().isEmpty();
        boolean hasFund = session.getPortfolio().getFunds() != null && 
                         !session.getPortfolio().getFunds().isEmpty();
        boolean hasPension = session.getPortfolio().getPensions() != null && 
                            !session.getPortfolio().getPensions().isEmpty();
        boolean hasBond = session.getPortfolio().getBonds() != null && 
                         !session.getPortfolio().getBonds().isEmpty();
        
        if (hasDeposit && hasSaving && hasStock && hasFund && hasPension && hasBond) {
            achieveAchievement(session, 16, "금융 종합");
        }
    }
    
    /**
     * 20. 순자산의 힘 (모든 인생이벤트 현금 대처)
     * resolve-life-event에서 호출
     */
    public void trackLifeEventResolution(GameSessionDto session, String resolutionType) {
        Map<String, Integer> progress = session.getAchievementProgress();
        
        // 총 인생이벤트 횟수
        int totalEvents = progress.getOrDefault("totalLifeEvents", 0) + 1;
        progress.put("totalLifeEvents", totalEvents);
        
        // 현금으로 해결한 횟수
        if ("CASH".equals(resolutionType)) {
            int cashResolutions = progress.getOrDefault("cashResolutions", 0) + 1;
            progress.put("cashResolutions", cashResolutions);
        }
        
        // 모든 이벤트를 현금으로만 해결했는지 체크
        if (totalEvents > 0 && 
            totalEvents == progress.getOrDefault("cashResolutions", 0)) {
            achieveAchievement(session, 20, "순자산의 힘");
        }
    }
    
    /**
     * 업적 달성 처리
     */
    private void achieveAchievement(GameSessionDto session, int achievementId, String achievementName) {
        if (!session.getAchievedAchievements().contains(achievementId)) {
            session.getAchievedAchievements().add(achievementId);
            log.info("🏆 업적 달성! uid={}, achievement={} ({})", 
                session.getUid(), achievementId, achievementName);
        }
    }
    
    /**
     * 달성한 업적 목록 반환
     */
    public Set<Integer> getAchievedAchievements(GameSessionDto session) {
        if (session.getAchievedAchievements() == null) {
            return new HashSet<>();
        }
        return session.getAchievedAchievements();
    }
}

