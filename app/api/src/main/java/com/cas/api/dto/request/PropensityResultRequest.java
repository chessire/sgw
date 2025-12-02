package com.cas.api.dto.request;

import lombok.Data;

/**
 * 재무성향 결과 저장 Request
 */
@Data
public class PropensityResultRequest {
    
    /**
     * 재무성향 타입 (문자열)
     * 예: "AGGRESSIVE", "MODERATE", "CONSERVATIVE" 등
     */
    private String propensityType;
}

