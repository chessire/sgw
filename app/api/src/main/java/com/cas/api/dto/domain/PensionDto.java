package com.cas.api.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 연금 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PensionDto {
    
    /**
     * 연금 ID
     */
    private String pensionId;
    
    /**
     * 연금명
     */
    private String name;
    
    /**
     * 월 납입액
     */
    private Long monthlyAmount;
    
    /**
     * 평가금액 (복리 적용)
     */
    private Long evaluationAmount;
    
    /**
     * 이자율
     */
    private BigDecimal interestRate;
    
    /**
     * 가입 라운드
     */
    private Integer subscriptionRound;
    
    /**
     * 납입 회차
     */
    private Integer paymentCount;
    
    /**
     * 우대 금리 적용 여부
     */
    private Boolean preferential;
}

