package com.cas.api.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 자산 배분 비율 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationDto {
    
    /**
     * 현금 비율 (%)
     */
    private BigDecimal cashRatio;
    
    /**
     * 예금 비율 (%)
     */
    private BigDecimal depositRatio;
    
    /**
     * 적금 비율 (%)
     */
    private BigDecimal savingRatio;
    
    /**
     * 채권 비율 (%)
     */
    private BigDecimal bondRatio;
    
    /**
     * 주식 비율 (%)
     */
    private BigDecimal stockRatio;
    
    /**
     * 펀드 비율 (%)
     */
    private BigDecimal fundRatio;
    
    /**
     * 연금 비율 (%)
     */
    private BigDecimal pensionRatio;
}

