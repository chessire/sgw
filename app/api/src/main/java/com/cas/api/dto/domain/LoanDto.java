package com.cas.api.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 대출 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDto {
    
    /**
     * 대출 ID
     */
    private String loanId;
    
    /**
     * 대출명
     */
    private String name;
    
    /**
     * 대출 원금
     */
    private Long principal;
    
    /**
     * 잔여 원금
     */
    private Long remainingBalance;
    
    /**
     * 이자율 (연 5%)
     */
    private BigDecimal interestRate;
    
    /**
     * 대출 실행 라운드
     */
    private Integer executionRound;
    
    /**
     * 만기 라운드 (실행 + 3개월)
     */
    private Integer maturityRound;
    
    /**
     * 경과 개월 수
     */
    private Integer elapsedMonths;
    
    /**
     * 월 이자 (대출금 × 5% ÷ 12)
     */
    private Long monthlyInterest;
    
    /**
     * 누적 지급 이자
     */
    private Long totalInterestPaid;
}

