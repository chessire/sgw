package com.cas.api.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 예금 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositDto {
    
    /**
     * 상품 키 (DEPOSIT_01, DEPOSIT_02 등)
     */
    private String productKey;
    
    /**
     * 상품명
     */
    private String name;
    
    /**
     * 예치금 (원금)
     */
    private Long principal;
    
    /**
     * 현재 잔액 (원금 + 이자)
     */
    private Long balance;
    
    /**
     * 예상 만기금액 (만기 시 수령 예상액)
     */
    private Long expectedMaturityAmount;
    
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
     * 경과 개월 수
     */
    private Integer elapsedMonths;
    
    /**
     * 우대 금리 적용 여부
     */
    private Boolean preferential;
}

