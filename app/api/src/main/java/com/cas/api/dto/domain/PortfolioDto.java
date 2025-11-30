package com.cas.api.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 포트폴리오 DTO
 * 플레이어의 전체 자산 현황
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDto {
    
    /**
     * 현금
     */
    private Long cash;
    
    /**
     * 예금 리스트
     */
    @Builder.Default
    private List<DepositDto> deposits = new ArrayList<>();
    
    /**
     * 적금 리스트
     */
    @Builder.Default
    private List<SavingDto> savings = new ArrayList<>();
    
    /**
     * 채권 리스트
     */
    @Builder.Default
    private List<BondDto> bonds = new ArrayList<>();
    
    /**
     * 주식 리스트
     */
    @Builder.Default
    private List<StockHoldingDto> stocks = new ArrayList<>();
    
    /**
     * 펀드 리스트
     */
    @Builder.Default
    private List<FundHoldingDto> funds = new ArrayList<>();
    
    /**
     * 연금 리스트
     */
    @Builder.Default
    private List<PensionDto> pensions = new ArrayList<>();
    
    /**
     * 대출 리스트
     */
    @Builder.Default
    private List<LoanDto> loans = new ArrayList<>();
    
    /**
     * 총 자산 (계산된 값)
     */
    private Long totalAssets;
    
    /**
     * 총 부채
     */
    private Long totalLiabilities;
    
    /**
     * 순자산 (총자산 - 부채)
     */
    private Long netWorth;
    
    /**
     * 자산 배분 비율
     */
    private AllocationDto allocation;
}

