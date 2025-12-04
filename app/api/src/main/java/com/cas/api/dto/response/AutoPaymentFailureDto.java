package com.cas.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 자동 납입 실패 정보 DTO
 * - 프론트엔드에서 팝업 표시용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoPaymentFailureDto {
    
    /**
     * 상품 유형
     * - SAVING: 적금
     * - PENSION: 연금
     * - INSURANCE: 보험
     */
    private String type;
    
    /**
     * 상품 키 (예: SAVING_A, PENSION_1)
     */
    private String productKey;
    
    /**
     * 상품 이름 (예: "적금 A", "개인연금")
     */
    private String name;
    
    /**
     * 납입 실패 금액
     */
    private Long amount;
    
    /**
     * 팝업 메시지
     * 예: "현금이 부족하여 적금A 자동이체가 실행되지 않았습니다."
     */
    private String message;
    
    /**
     * 가능한 액션 목록
     * - CANCEL_PRODUCT: 해당 상품 해지
     * - CANCEL_OTHER: 다른 상품 해지
     */
    private List<String> actions;
}

