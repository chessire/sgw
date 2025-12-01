package com.cas.api.dto.request;

import lombok.Data;

/**
 * 인생이벤트 해결 요청 DTO
 */
@Data
public class ResolveLifeEventRequest {
    
    /**
     * 라운드 번호
     */
    private Integer roundNo;
    
    /**
     * 이벤트 키
     */
    private String eventKey;
    
    /**
     * 해결 방법
     * - CASH: 현금으로 지급
     * - SELL_ASSETS: 투자상품 해지
     * - MIXED: 현금 + 투자상품 복합
     * - INSURANCE: 보험 적용
     * - LOAN: 대출 이용
     */
    private String resolutionType;
    
    /**
     * 현금으로 지급할 금액 (resolutionType이 CASH 또는 MIXED인 경우)
     */
    private Long cashAmount;
    
    /**
     * 대출 금액 (resolutionType이 LOAN인 경우)
     */
    private Long loanAmount;
    
    /**
     * 해지할 상품 정보 (resolutionType이 SELL_ASSETS 또는 MIXED인 경우)
     * 프론트에서 선택한 상품 정보
     */
    private Object sellActions; // Map<String, Object> 형태
    
    /**
     * 해결 이유
     * - STRATEGIC: 전략적 선택 (현금 있지만 상품 매도 선택)
     * - INSUFFICIENT_CASH: 현금 부족으로 인한 강제 매도
     */
    private String reason;
    
    /**
     * 부족한 금액 (프론트에서 계산하여 전달, 검증용)
     */
    private Long shortfallAmount;
}

