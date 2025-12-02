package com.cas.api.dto.request;

import lombok.Data;

/**
 * 우대금리 퀴즈 제출 Request
 */
@Data
public class SubmitQuizRequest {
    
    /**
     * 상품 타입
     * DEPOSIT, STOCK, BOND, PENSION, FUND, INSURANCE
     */
    private String productType;
}

