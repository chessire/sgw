package com.cas.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NPC 조언 사용 Request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UseAdviceRequest {
    
    /**
     * 라운드 번호
     */
    private Integer roundNo;
}

