package com.cas.api.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주식 보유 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockHoldingDto {
    
    /**
     * 주식 ID (STOCK_01 ~ STOCK_07)
     */
    private String stockId;
    
    /**
     * 종목명
     */
    private String name;
    
    /**
     * 보유 수량
     */
    private Integer quantity;
    
    /**
     * 평균 매수가
     */
    private Long avgPrice;
    
    /**
     * 현재가
     */
    private Long currentPrice;
    
    /**
     * 평가금액 (수량 × 현재가)
     */
    private Long evaluationAmount;
    
    /**
     * 평가손익 (평가금액 - 총 매입가)
     */
    private Long profitLoss;
    
    /**
     * 수익률 (%)
     */
    private Double returnRate;
}

