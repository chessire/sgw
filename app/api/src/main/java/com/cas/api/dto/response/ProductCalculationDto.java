package com.cas.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 상품 계산 미리보기 Response DTO
 * 구매 전 계산된 결과를 확인하는 용도
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCalculationDto {
    
    /**
     * 상품 유형 (STOCK, FUND, DEPOSIT, SAVING, BOND, PENSION)
     */
    private String productType;
    
    /**
     * 상품 키/ID
     */
    private String productKey;
    
    /**
     * 상품명
     */
    private String productName;
    
    /**
     * 액션 (BUY, SELL, SUBSCRIBE, CANCEL)
     */
    private String action;
    
    // ============================================
    // 입력값
    // ============================================
    
    /**
     * 요청 금액
     */
    private Long requestAmount;
    
    /**
     * 요청 수량 (주식)
     */
    private Integer requestQuantity;
    
    // ============================================
    // 계산 결과 - 공통
    // ============================================
    
    /**
     * 실제 거래 금액 (수수료 포함)
     */
    private Long totalCost;
    
    /**
     * 거래 후 예상 현금 잔액
     */
    private Long expectedCashAfter;
    
    /**
     * 현금 부족 여부
     */
    private Boolean insufficientCash;
    
    // ============================================
    // 계산 결과 - 주식
    // ============================================
    
    /**
     * 현재 주가
     */
    private Long currentPrice;
    
    /**
     * 매수/매도 수량
     */
    private Integer quantity;
    
    /**
     * 거래 후 예상 보유 수량
     */
    private Integer expectedQuantityAfter;
    
    /**
     * 거래 후 예상 평균 단가
     */
    private Long expectedAvgPrice;
    
    /**
     * 거래 후 예상 평가금액
     */
    private Long expectedEvaluationAmount;
    
    // ============================================
    // 계산 결과 - 펀드
    // ============================================
    
    /**
     * 현재 NAV (기준가)
     */
    private Long currentNav;
    
    /**
     * 매수/매도 좌수
     */
    private Integer shares;
    
    /**
     * 거래 후 예상 보유 좌수
     */
    private Integer expectedSharesAfter;
    
    /**
     * 거래 후 예상 평균 NAV
     */
    private Long expectedAvgNav;
    
    // ============================================
    // 계산 결과 - 예금/적금/채권
    // ============================================
    
    /**
     * 적용 금리 (연 %)
     */
    private BigDecimal interestRate;
    
    /**
     * 우대 금리 적용 여부
     */
    private Boolean preferentialApplied;
    
    /**
     * 만기 라운드
     */
    private Integer maturityRound;
    
    /**
     * 예상 만기 금액
     */
    private Long expectedMaturityAmount;
    
    /**
     * 예상 이자 수익
     */
    private Long expectedInterest;
}

