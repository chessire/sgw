package com.cas.api.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 펀드 보유 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundHoldingDto {
    
    /**
     * 펀드 ID (FUND_01 ~ FUND_05)
     */
    private String fundId;
    
    /**
     * 펀드명
     */
    private String name;
    
    /**
     * 보유 좌수
     */
    private Integer shares;
    
    /**
     * 평균 매수 기준가
     */
    private Long avgNav;
    
    /**
     * 현재 기준가 (NAV)
     */
    private Long currentNav;
    
    /**
     * 평가금액 (좌수 × 현재 기준가)
     */
    private Long evaluationAmount;
    
    /**
     * 평가손익
     */
    private Long profitLoss;
    
    /**
     * 수익률 (%)
     */
    private Double returnRate;
}

