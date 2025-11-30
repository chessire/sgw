package com.cas.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 라운드 진행 Request
 * 해당 월의 유저의 모든 거래 내용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProceedRoundRequest {
    
    /**
     * 현재 라운드 정보
     */
    private CurrentRoundDto currentRound;
    
    /**
     * 보험 거래
     */
    private InsuranceDto insurance;
    
    /**
     * 펀드 거래 목록
     */
    private List<FundTransactionDto> fund;
    
    /**
     * 주식 거래 목록
     */
    private List<StockTransactionDto> stock;
    
    /**
     * 채권 거래 목록
     */
    private List<BondTransactionDto> bond;
    
    /**
     * 예적금 거래 목록
     */
    private List<DepositTransactionDto> deposit;
    
    /**
     * 연금 거래 목록
     */
    private List<PensionTransactionDto> pension;
    
    /**
     * 불법사금융 사용 여부
     */
    private IllegalPrivateLoanDto illegalPrivateLoan;
    
    /**
     * 인생이벤트 해결
     */
    private LifeEventResolutionDto lifeEvent;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentRoundDto {
        private Integer roundNo;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsuranceDto {
        private Boolean subscribe;  // 가입
        private Boolean cancel;     // 해지
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundTransactionDto {
        private String action;  // "BUY" | "SELL"
        private Long amount;    // 거래 금액
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockTransactionDto {
        private String action;  // "BUY" | "SELL"
        private Integer quantity; // 수량
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BondTransactionDto {
        private String action;  // "SUBSCRIBE" | "CANCEL"
        private Long amount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepositTransactionDto {
        private String action;  // "SUBSCRIBE" | "CANCEL"
        private Long amount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PensionTransactionDto {
        private String action;  // "SUBSCRIBE" | "CANCEL"
        private Long amount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IllegalPrivateLoanDto {
        private Boolean executed;  // 실행 여부
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LifeEventResolutionDto {
        private String eventKey;       // 이벤트 키
        private String resolutionType; // 해결 방법
        // "USE_CASH", "USE_SAVINGS", "USE_DEPOSIT", "REDEEM_FUND",
        // "SELL_STOCK", "REDEEM_BOND", "INSURANCE_COVERED", "TAKE_LOAN"
    }
}

