package com.cas.api.service.game;

import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.dto.domain.PortfolioDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * 인생이벤트 Service
 * - 이벤트 발생 확률 계산
 * - 이벤트 처리 (현금 증감, 보험 처리)
 * - 대출 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifeEventService {
    
    private final GameSessionService gameSessionService;
    private final Random random = new Random();
    
    /**
     * 인생이벤트 발생 여부 판정
     * 
     * @param session 게임 세션
     * @param currentRound 현재 라운드
     * @return 이벤트 발생 여부
     */
    public boolean shouldEventOccur(GameSessionDto session, int currentRound) {
        // TODO: CSV 규칙에 따라 라운드별 이벤트 발생 확률 적용
        // 현재는 간단히 50% 확률로 구현
        
        boolean occur = random.nextBoolean();
        
        log.debug("Life event occurrence check: uid={}, round={}, occur={}", 
            session.getUid(), currentRound, occur);
        
        return occur;
    }
    
    /**
     * 이벤트 처리 (금액 증감)
     * 
     * @param portfolio 포트폴리오
     * @param eventAmount 이벤트 금액 (양수: 수입, 음수: 지출)
     * @param eventType 이벤트 유형 (INCOME, EXPENSE)
     * @return 처리 후 현금
     */
    public Long processEvent(PortfolioDto portfolio, long eventAmount, String eventType) {
        log.info("Processing life event: type={}, amount={}", eventType, eventAmount);
        
        long cash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        cash += eventAmount;
        
        portfolio.setCash(cash);
        
        log.info("Event processed: newCash={}", cash);
        return cash;
    }
    
    /**
     * 보험 처리 가능 이벤트 여부 확인
     * 
     * @param eventKey 이벤트 키
     * @return 보험 처리 가능 여부
     */
    public boolean isInsurableEvent(String eventKey) {
        // TODO: CSV에서 보험 처리 가능 이벤트 목록 확인
        // 현재는 임시로 특정 키워드 포함 여부로 판단
        
        boolean insurable = eventKey != null && 
            (eventKey.contains("ACCIDENT") || eventKey.contains("HOSPITAL"));
        
        log.debug("Insurable event check: eventKey={}, insurable={}", eventKey, insurable);
        
        return insurable;
    }
    
    /**
     * 보험 적용 처리
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     * @param originalAmount 원래 이벤트 금액 (음수)
     * @return 보험 적용 후 금액
     */
    public Long applyInsurance(GameSessionDto session, PortfolioDto portfolio, long originalAmount) {
        log.info("Applying insurance: uid={}, originalAmount={}", 
            session.getUid(), originalAmount);
        
        if (!Boolean.TRUE.equals(session.getInsuranceSubscribed())) {
            log.warn("Insurance not subscribed: uid={}", session.getUid());
            return originalAmount;
        }
        
        // 보험 가입 시 금액의 50% 감소 (예시)
        long reducedAmount = originalAmount / 2;
        
        // 보험 처리 가능 이벤트 발생 표시 (12라운드 중 1회만)
        gameSessionService.markInsurableEventOccurred(session);
        
        log.info("Insurance applied: originalAmount={}, reducedAmount={}", 
            originalAmount, reducedAmount);
        
        return reducedAmount;
    }
    
    /**
     * 대출 가능 여부 확인
     * 
     * @param session 게임 세션
     * @return 대출 가능 여부
     */
    public boolean canTakeLoan(GameSessionDto session) {
        boolean available = !Boolean.TRUE.equals(session.getLoanUsed());
        
        if (!available) {
            log.warn("Loan not available: uid={}, alreadyUsed={}", 
                session.getUid(), session.getLoanUsed());
        }
        
        return available;
    }
    
    /**
     * 대출 실행
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     * @param loanAmount 대출 금액
     * @return 성공 여부
     */
    public boolean takeLoan(GameSessionDto session, PortfolioDto portfolio, long loanAmount) {
        log.info("Taking loan: uid={}, amount={}", session.getUid(), loanAmount);
        
        if (!canTakeLoan(session)) {
            return false;
        }
        
        // 현금 증가
        long cash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        cash += loanAmount;
        portfolio.setCash(cash);
        
        // 대출 사용 표시
        boolean success = gameSessionService.useLoan(session);
        
        if (success) {
            // LoanDto 생성
            int currentRound = session.getCurrentRound();
            int maturityRound = currentRound + 3; // 3개월 고정
            
            // 월 이자 = 대출금 × 5% ÷ 12
            long monthlyInterest = Math.round(loanAmount * 0.05 / 12);
            
            com.cas.api.dto.domain.LoanDto loan = com.cas.api.dto.domain.LoanDto.builder()
                .loanId("LOAN_" + currentRound)
                .productKey("LOAN_NORMAL")
                .name("일반 대출")
                .principal(loanAmount)
                .remainingBalance(loanAmount)
                .interestRate(java.math.BigDecimal.valueOf(5.0))
                .executionRound(currentRound)
                .maturityRound(maturityRound)
                .elapsedMonths(0)
                .monthlyInterest(monthlyInterest)
                .totalInterestPaid(0L)
                .build();
            
            // 포트폴리오에 추가
            if (portfolio.getLoans() == null) {
                portfolio.setLoans(new java.util.ArrayList<com.cas.api.dto.domain.LoanDto>());
            }
            portfolio.getLoans().add(loan);
            
            log.info("Loan taken successfully: uid={}, loanAmount={}, monthlyInterest={}, maturity=R{}, newCash={}", 
                session.getUid(), loanAmount, monthlyInterest, maturityRound, cash);
        }
        
        return success;
    }
    
    /**
     * 불법사금융 사용 (패널티)
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     * @param loanAmount 대출 금액
     */
    public void useIllegalLoan(GameSessionDto session, PortfolioDto portfolio, long loanAmount) {
        log.warn("Using illegal loan: uid={}, amount={}", session.getUid(), loanAmount);
        
        // 현금 증가 (높은 이자율 적용 예정)
        long cash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        cash += loanAmount;
        portfolio.setCash(cash);
        
        // 불법사금융 사용 표시 (패널티 적용)
        gameSessionService.useIllegalLoan(session);
        
        log.warn("Illegal loan used: uid={}, newCash={}", session.getUid(), cash);
    }
    
    /**
     * 현금 부족 시 강제 이벤트 처리
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     * @param requiredAmount 필요 금액
     * @return 처리 방법 ("LOAN", "ILLEGAL_LOAN", "FORCE_SELL")
     */
    public String handleInsufficientCash(GameSessionDto session, PortfolioDto portfolio, 
                                          long requiredAmount) {
        log.warn("Insufficient cash: uid={}, required={}, current={}", 
            session.getUid(), requiredAmount, portfolio.getCash());
        
        // 대출 가능 여부 확인
        if (canTakeLoan(session)) {
            log.info("Suggesting regular loan");
            return "LOAN_AVAILABLE";
        }
        
        // 대출 불가능 시 불법사금융 또는 자산 강제 매도
        log.warn("Regular loan not available, suggesting alternatives");
        return "ILLEGAL_LOAN_OR_FORCE_SELL";
    }
    
    /**
     * 현금 지급 가능 여부 확인
     * 
     * @param portfolio 포트폴리오
     * @param requiredAmount 필요 금액
     * @return 지급 가능 여부와 부족 금액
     */
    public CashPaymentResult checkCashPayment(PortfolioDto portfolio, long requiredAmount) {
        long currentCash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        long shortfall = requiredAmount - currentCash;
        
        boolean sufficient = shortfall <= 0;
        
        log.debug("Cash payment check: current={}, required={}, shortfall={}, sufficient={}", 
            currentCash, requiredAmount, shortfall, sufficient);
        
        return new CashPaymentResult(sufficient, currentCash, shortfall);
    }
    
    /**
     * 현금 지급 처리
     * 
     * @param portfolio 포트폴리오
     * @param amount 지급 금액
     * @return 성공 여부
     */
    public boolean processCashPayment(PortfolioDto portfolio, long amount) {
        long currentCash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        
        if (currentCash < amount) {
            log.warn("Insufficient cash for payment: current={}, required={}", currentCash, amount);
            return false;
        }
        
        portfolio.setCash(currentCash - amount);
        log.info("Cash payment processed: amount={}, remaining={}", amount, portfolio.getCash());
        
        return true;
    }
    
    /**
     * 해지 가능한 자산이 있는지 확인
     * 
     * @param portfolio 포트폴리오
     * @return 해지 가능 여부
     */
    public boolean hasRedeemableAssets(PortfolioDto portfolio) {
        if (portfolio == null) {
            return false;
        }
        
        boolean hasStocks = portfolio.getStocks() != null && !portfolio.getStocks().isEmpty();
        boolean hasFunds = portfolio.getFunds() != null && !portfolio.getFunds().isEmpty();
        boolean hasBonds = portfolio.getBonds() != null && !portfolio.getBonds().isEmpty();
        boolean hasDeposits = portfolio.getDeposits() != null && !portfolio.getDeposits().isEmpty();
        boolean hasSavings = portfolio.getSavings() != null && !portfolio.getSavings().isEmpty();
        
        boolean redeemable = hasStocks || hasFunds || hasBonds || hasDeposits || hasSavings;
        
        log.debug("Redeemable assets check: stocks={}, funds={}, bonds={}, deposits={}, savings={}, result={}", 
            hasStocks, hasFunds, hasBonds, hasDeposits, hasSavings, redeemable);
        
        return redeemable;
    }
    
    /**
     * 복합 해결 처리 (현금 + 투자상품 매도)
     * 
     * @param portfolio 포트폴리오
     * @param cashAmount 현금 지급액
     * @param assetsValue 매도한 자산 가치
     * @param requiredAmount 필요 금액
     * @param reason 해결 이유
     * @return 해결 결과
     */
    public MixedResolutionResult processMixedResolution(PortfolioDto portfolio, 
                                                         Long cashAmount, 
                                                         long assetsValue,
                                                         long requiredAmount,
                                                         String reason) {
        long totalPayment = (cashAmount != null ? cashAmount : 0L) + assetsValue;
        long shortfall = requiredAmount - totalPayment;
        boolean resolved = shortfall <= 0;
        
        log.info("Mixed resolution: cash={}, assets={}, total={}, required={}, shortfall={}, reason={}", 
            cashAmount, assetsValue, totalPayment, requiredAmount, shortfall, reason);
        
        // 현금 차감
        if (cashAmount != null && cashAmount > 0) {
            long currentCash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
            portfolio.setCash(currentCash - cashAmount);
        }
        
        // 전략적 선택인지 강제 매도인지 로그
        if ("STRATEGIC".equals(reason)) {
            log.info("Strategic asset liquidation: user chose to sell assets despite having cash");
        } else if ("INSUFFICIENT_CASH".equals(reason)) {
            log.warn("Forced asset liquidation due to insufficient cash");
        }
        
        return new MixedResolutionResult(resolved, totalPayment, shortfall, reason);
    }
    
    /**
     * 현금 지급 결과 DTO
     */
    public static class CashPaymentResult {
        private final boolean sufficient;
        private final long currentCash;
        private final long shortfall;
        
        public CashPaymentResult(boolean sufficient, long currentCash, long shortfall) {
            this.sufficient = sufficient;
            this.currentCash = currentCash;
            this.shortfall = shortfall;
        }
        
        public boolean isSufficient() { return sufficient; }
        public long getCurrentCash() { return currentCash; }
        public long getShortfall() { return shortfall; }
    }
    
    /**
     * 복합 해결 결과 DTO
     */
    public static class MixedResolutionResult {
        private final boolean resolved;
        private final long totalPayment;
        private final long shortfall;
        private final String reason;
        
        public MixedResolutionResult(boolean resolved, long totalPayment, long shortfall, String reason) {
            this.resolved = resolved;
            this.totalPayment = totalPayment;
            this.shortfall = shortfall;
            this.reason = reason;
        }
        
        public boolean isResolved() { return resolved; }
        public long getTotalPayment() { return totalPayment; }
        public long getShortfall() { return shortfall; }
        public String getReason() { return reason; }
    }
}

