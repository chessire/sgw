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
}

