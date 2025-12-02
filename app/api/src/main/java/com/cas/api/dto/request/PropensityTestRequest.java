package com.cas.api.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 재무성향검사 제출 Request
 */
@Data
public class PropensityTestRequest {
    
    /**
     * 재무성향검사 답안
     * 각 문항의 답안 (0~4)
     */
    private List<Integer> answers;
}

