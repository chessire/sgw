package com.cas.api.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 적금 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingDto {
    
    /**
     * 상품 키 (SAVING_A, SAVING_B 등)
     */
    private String productKey;
    
    /**
     * 상품명
     */
    private String name;
    
    /**
     * 월 납입액
     */
    private Long monthlyAmount;
    
    /**
     * 현재 잔액 (총 납입액 + 이자)
     */
    private Long balance;
    
    /**
     * 이자율
     */
    private BigDecimal interestRate;
    
    /**
     * 가입 라운드
     */
    private Integer subscriptionRound;
    
    /**
     * 만기 라운드
     */
    private Integer maturityRound;
    
    /**
     * 납입 회차
     */
    private Integer paymentCount;
    
    /**
     * 우대 금리 적용 여부
     */
    private Boolean preferential;
}

