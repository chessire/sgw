package com.cas.api.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 채권 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BondDto {
    
    /**
     * 채권 ID (BOND_01, BOND_02 등)
     */
    private String bondId;
    
    /**
     * 채권명 (국채, 회사채)
     */
    private String name;
    
    /**
     * 액면가 (투자 원금)
     */
    private Long faceValue;
    
    /**
     * 평가액 (시장가격 + 경과이자)
     */
    private Long evaluationAmount;
    
    /**
     * 채권 금리
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

