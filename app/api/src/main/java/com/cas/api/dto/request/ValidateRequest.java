package com.cas.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 검증 Request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateRequest {
    
    /**
     * 사용자 고유 식별자
     */
    private String uid;
    
    /**
     * CDN 게임 데이터 MD5 Hash
     */
    private String dataFileMD5;
}

