package com.cas.api.dto.response;

import com.cas.api.dto.domain.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 포트폴리오 Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponseDto {
    
    /**
     * 요약 정보 (총자산, 부채, 순자산, 배분 비율)
     */
    private SummaryDto summary;
    
    /**
     * 현금성 자산
     */
    private CashLikeAssetsDto cashLikeAssets;
    
    /**
     * 투자 자산
     */
    private InvestmentAssetsDto investmentAssets;
    
    /**
     * 실제 보유 리스트
     */
    private HoldingsDto holdings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryDto {
        private Long totalAssets;      // 총자산
        private Long totalLiabilities; // 부채
        private Long netWorth;         // 순자산
        private AllocationDto allocation; // 배분 비율
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashLikeAssetsDto {
        private Long cash;            // 현금
        private Long depositBalance;  // 예금 잔액
        private Long savingBalance;   // 적금 잔액
        private Long bondBalance;     // 채권 평가액
        private Long total;           // 합계
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestmentAssetsDto {
        private Long stockEvaluation;   // 주식 평가액
        private Long fundEvaluation;    // 펀드 평가액
        private Long pensionEvaluation; // 연금 평가액
        private Long total;             // 합계
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldingsDto {
        private Long cash;
        private List<DepositDto> deposits;
        private List<SavingDto> savings;
        private List<BondDto> bonds;
        private List<StockHoldingDto> stocks;
        private List<FundHoldingDto> funds;
        private List<PensionDto> pensions;
        private List<LoanDto> loans;
    }
}

