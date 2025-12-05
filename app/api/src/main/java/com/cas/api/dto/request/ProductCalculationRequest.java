package com.cas.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상품 계산 미리보기 Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCalculationRequest {
    
    /**
     * 상품 유형 (STOCK, FUND, DEPOSIT, SAVING, BOND, PENSION)
     */
    private String productType;
    
    /**
     * 상품 키/ID (예: STOCK_01, FUND_01, DEPOSIT, SAVING_A, BOND_NATIONAL)
     */
    private String productKey;
    
    /**
     * 액션 (BUY, SELL, SUBSCRIBE, CANCEL)
     */
    private String action;
    
    /**
     * 금액 (펀드, 예금, 적금, 채권용)
     */
    private Long amount;
    
    /**
     * 수량 (주식용)
     */
    private Integer quantity;
}

