package com.cas.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 자동 납입 결과 DTO
 * - 라운드 시작 시 자동으로 처리되는 납입 결과
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoPaymentResultDto {
    
    /**
     * 대출 이자 납입 결과
     */
    private PaymentItemDto loanInterest;
    
    /**
     * 연금 납입 결과 목록
     */
    @Builder.Default
    private List<PaymentItemDto> pensions = new ArrayList<>();
    
    /**
     * 보험료 납입 결과
     */
    private PaymentItemDto insurance;
    
    /**
     * 적금 납입 결과 목록
     */
    @Builder.Default
    private List<PaymentItemDto> savings = new ArrayList<>();
    
    /**
     * 개별 납입 항목 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentItemDto {
        /**
         * 상품 키 (예: SAVING_A, PENSION_1)
         */
        private String productKey;
        
        /**
         * 상품 이름 (예: "적금 A", "개인연금")
         */
        private String name;
        
        /**
         * 납입 금액
         */
        private Long amount;
        
        /**
         * 납입 성공 여부
         */
        private Boolean success;
        
        /**
         * 실패 사유 (실패 시에만)
         * - INSUFFICIENT_CASH: 현금 부족
         */
        private String failReason;
    }
}

