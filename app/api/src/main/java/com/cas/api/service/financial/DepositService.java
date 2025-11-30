package com.cas.api.service.financial;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 예적금 계산 Service
 * - 정기예금: 일시 예치, 만기 시 이자 지급
 * - 적금: 매월 분할 납입, 만기 시 이자 지급
 */
@Slf4j
@Service
public class DepositService {
    
    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.005"); // 중도해지 0.5%
    private static final int MONTHS_PER_YEAR = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 0; // 원 단위
    
    /**
     * 정기예금 중도해지 금액 계산
     * 
     * @param principal 예치금
     * @param elapsedMonths 경과 개월 수
     * @return 중도해지 금액
     */
    public BigDecimal calculateDepositEarlyWithdrawal(BigDecimal principal, int elapsedMonths) {
        log.debug("Calculating deposit early withdrawal: principal={}, elapsedMonths={}", 
            principal, elapsedMonths);
        
        // 중도해지 금액 = 예치금 × (1 + 0.005 × 경과개월/12)
        BigDecimal monthlyRate = PENALTY_RATE
            .multiply(new BigDecimal(elapsedMonths))
            .divide(new BigDecimal(MONTHS_PER_YEAR), 10, ROUNDING);
        
        BigDecimal result = principal.multiply(BigDecimal.ONE.add(monthlyRate))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Deposit early withdrawal: {}", result);
        return result;
    }
    
    /**
     * 정기예금 만기 금액 계산
     * 
     * @param principal 예치금
     * @param preferentialRate 우대 금리 (연이율, 예: 0.025)
     * @param termMonths 만기 개월 수
     * @return 만기 금액
     */
    public BigDecimal calculateDepositMaturity(BigDecimal principal, BigDecimal preferentialRate, 
                                                int termMonths) {
        log.debug("Calculating deposit maturity: principal={}, rate={}, termMonths={}", 
            principal, preferentialRate, termMonths);
        
        // 만기 금액 = 예치금 × (1 + 우대금리 × 만기개월/12)
        BigDecimal monthlyRate = preferentialRate
            .multiply(new BigDecimal(termMonths))
            .divide(new BigDecimal(MONTHS_PER_YEAR), 10, ROUNDING);
        
        BigDecimal result = principal.multiply(BigDecimal.ONE.add(monthlyRate))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Deposit maturity: {}", result);
        return result;
    }
    
    /**
     * 적금 중도해지 금액 계산
     * 
     * @param monthlyPayment 월 납입액
     * @param paymentCount 납입 회차
     * @return 중도해지 금액
     */
    public BigDecimal calculateSavingEarlyWithdrawal(BigDecimal monthlyPayment, int paymentCount) {
        log.debug("Calculating saving early withdrawal: monthlyPayment={}, paymentCount={}", 
            monthlyPayment, paymentCount);
        
        // 중도해지 금액 = 월납입액 × Σ(1 + 0.005×n/12), n=0~(paymentCount-1)
        BigDecimal total = BigDecimal.ZERO;
        
        for (int n = 0; n < paymentCount; n++) {
            BigDecimal monthlyRate = PENALTY_RATE
                .multiply(new BigDecimal(n))
                .divide(new BigDecimal(MONTHS_PER_YEAR), 10, ROUNDING);
            
            BigDecimal amount = monthlyPayment.multiply(BigDecimal.ONE.add(monthlyRate));
            total = total.add(amount);
        }
        
        BigDecimal result = total.setScale(SCALE, ROUNDING);
        log.debug("Saving early withdrawal: {}", result);
        return result;
    }
    
    /**
     * 적금 만기 금액 계산
     * 
     * @param monthlyPayment 월 납입액
     * @param preferentialRate 우대 금리 (연이율, 예: 0.026)
     * @param paymentCount 납입 회차
     * @return 만기 금액
     */
    public BigDecimal calculateSavingMaturity(BigDecimal monthlyPayment, BigDecimal preferentialRate, 
                                               int paymentCount) {
        log.debug("Calculating saving maturity: monthlyPayment={}, rate={}, paymentCount={}", 
            monthlyPayment, preferentialRate, paymentCount);
        
        // 만기 금액 = 월납입액 × Σ(1 + 우대금리×n/12), n=1~paymentCount
        BigDecimal total = BigDecimal.ZERO;
        
        for (int n = 1; n <= paymentCount; n++) {
            BigDecimal monthlyRate = preferentialRate
                .multiply(new BigDecimal(n))
                .divide(new BigDecimal(MONTHS_PER_YEAR), 10, ROUNDING);
            
            BigDecimal amount = monthlyPayment.multiply(BigDecimal.ONE.add(monthlyRate));
            total = total.add(amount);
        }
        
        BigDecimal result = total.setScale(SCALE, ROUNDING);
        log.debug("Saving maturity: {}", result);
        return result;
    }
    
    /**
     * 적금 게임종료 시 강제정산 금액 계산
     * (만기 미도달 시 우대금리 적용)
     * 
     * @param monthlyPayment 월 납입액
     * @param preferentialRate 우대 금리
     * @param paymentCount 납입 회차
     * @return 강제정산 금액
     */
    public BigDecimal calculateSavingForcedSettlement(BigDecimal monthlyPayment, 
                                                       BigDecimal preferentialRate, 
                                                       int paymentCount) {
        log.debug("Calculating saving forced settlement: monthlyPayment={}, rate={}, paymentCount={}", 
            monthlyPayment, preferentialRate, paymentCount);
        
        // 강제정산 = 월납입액 × Σ(1 + 우대금리×n/12), n=1~paymentCount
        return calculateSavingMaturity(monthlyPayment, preferentialRate, paymentCount);
    }
}

