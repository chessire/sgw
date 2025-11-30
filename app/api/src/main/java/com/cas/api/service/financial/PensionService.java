package com.cas.api.service.financial;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 연금/보험/대출 계산 Service
 * - 연금: 중도해지 불가, 게임 종료 시 수령, 월 복리
 * - 보험: 이자 없음, 납입 누적액만
 * - 대출: 연 5%, 3개월 고정, 월 이자 자동 차감
 */
@Slf4j
@Service
public class PensionService {
    
    private static final BigDecimal PENSION_RATE = new BigDecimal("0.032"); // 연 3.2%
    private static final BigDecimal LOAN_RATE = new BigDecimal("0.05"); // 연 5%
    private static final int LOAN_TERM_MONTHS = 3; // 대출 기간 3개월
    private static final int MONTHS_PER_YEAR = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 0; // 원 단위
    
    /**
     * 연금 게임종료 시 수령액 계산
     * 
     * @param monthlyPayment 월 납입액
     * @param paymentCount 납입 회차
     * @return 수령액
     */
    public BigDecimal calculatePensionPayout(BigDecimal monthlyPayment, int paymentCount) {
        log.debug("Calculating pension payout: monthlyPayment={}, paymentCount={}", 
            monthlyPayment, paymentCount);
        
        // 수령액 = 월납입액 × Σ(1 + 0.032×n/12), n=1~paymentCount
        BigDecimal total = BigDecimal.ZERO;
        
        for (int n = 1; n <= paymentCount; n++) {
            BigDecimal monthlyRate = PENSION_RATE
                .multiply(new BigDecimal(n))
                .divide(new BigDecimal(MONTHS_PER_YEAR), 10, ROUNDING);
            
            BigDecimal amount = monthlyPayment.multiply(BigDecimal.ONE.add(monthlyRate));
            total = total.add(amount);
        }
        
        BigDecimal result = total.setScale(SCALE, ROUNDING);
        log.debug("Pension payout: {}", result);
        return result;
    }
    
    /**
     * 보험 납입 누적액 계산
     * 
     * @param monthlyPremium 월 보험료
     * @param paymentCount 납입 회차
     * @return 납입 누적액
     */
    public BigDecimal calculateInsuranceAccumulation(BigDecimal monthlyPremium, int paymentCount) {
        log.debug("Calculating insurance accumulation: monthlyPremium={}, paymentCount={}", 
            monthlyPremium, paymentCount);
        
        // 납입 누적액 = 월보험료 × 납입회차 (이자 없음)
        BigDecimal result = monthlyPremium
            .multiply(new BigDecimal(paymentCount))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Insurance accumulation: {}", result);
        return result;
    }
    
    /**
     * 대출 월 이자 계산
     * 
     * @param loanAmount 대출금
     * @return 월 이자
     */
    public BigDecimal calculateLoanMonthlyInterest(BigDecimal loanAmount) {
        log.debug("Calculating loan monthly interest: loanAmount={}", loanAmount);
        
        // 월 이자 = 대출금 × 0.05 ÷ 12 = 대출금 × 0.00417
        BigDecimal monthlyRate = LOAN_RATE
            .divide(new BigDecimal(MONTHS_PER_YEAR), 10, ROUNDING);
        
        BigDecimal result = loanAmount
            .multiply(monthlyRate)
            .setScale(SCALE, ROUNDING);
        
        log.debug("Loan monthly interest: {}", result);
        return result;
    }
    
    /**
     * 대출 총 이자 비용 계산
     * 
     * @param loanAmount 대출금
     * @return 총 이자 비용 (3개월)
     */
    public BigDecimal calculateLoanTotalInterest(BigDecimal loanAmount) {
        log.debug("Calculating loan total interest: loanAmount={}", loanAmount);
        
        // 총 이자 = 대출금 × 0.00417 × 3 = 대출금 × 0.0125
        BigDecimal monthlyInterest = calculateLoanMonthlyInterest(loanAmount);
        BigDecimal result = monthlyInterest
            .multiply(new BigDecimal(LOAN_TERM_MONTHS))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Loan total interest: {}", result);
        return result;
    }
    
    /**
     * 대출 만기 상환액 계산
     * (이자는 매월 자동 차감되므로 만기 시에는 원금만 상환)
     * 
     * @param loanAmount 대출금
     * @return 만기 상환액 (원금)
     */
    public BigDecimal calculateLoanMaturityRepayment(BigDecimal loanAmount) {
        log.debug("Calculating loan maturity repayment: loanAmount={}", loanAmount);
        
        // 만기 상환액 = 대출금 (원금만)
        log.debug("Loan maturity repayment: {}", loanAmount);
        return loanAmount;
    }
    
    /**
     * 대출 조기 상환액 계산
     * 
     * @param loanAmount 대출금
     * @param elapsedMonths 경과 개월 수
     * @param paidInterest 기지급 이자
     * @return 조기 상환액 (원금 + 잔여 이자)
     */
    public BigDecimal calculateLoanEarlyRepayment(BigDecimal loanAmount, int elapsedMonths, 
                                                    BigDecimal paidInterest) {
        log.debug("Calculating loan early repayment: loanAmount={}, elapsedMonths={}, paidInterest={}", 
            loanAmount, elapsedMonths, paidInterest);
        
        // 조기 상환액 = 원금 + (총 이자 - 기지급 이자)
        BigDecimal totalInterest = calculateLoanTotalInterest(loanAmount);
        BigDecimal remainingInterest = totalInterest.subtract(paidInterest);
        
        BigDecimal result = loanAmount.add(remainingInterest);
        log.debug("Loan early repayment: {}", result);
        return result;
    }
}

