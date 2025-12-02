package com.cas.api.dto.request;

import lombok.Data;

/**
 * 교육 영상 시청 완료 Request
 */
@Data
public class CompleteVideoRequest {
    
    /**
     * 영상 타입
     * DEPOSIT, STOCK, BOND, PENSION, FUND, INSURANCE
     */
    private String videoType;
}

