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
     * - FORCE_SELL: 투자상품 해지
     * - INSURANCE: 보험 적용
     * - LOAN: 대출 이용
     */
    private String resolutionType;
    
    /**
     * 대출 금액 (resolutionType이 LOAN인 경우)
     */
    private Long loanAmount;
    
    /**
     * 해지할 상품 정보 (resolutionType이 FORCE_SELL인 경우)
     * 프론트에서 선택한 상품 정보
     */
    private Object sellActions; // Map<String, Object> 형태
}

