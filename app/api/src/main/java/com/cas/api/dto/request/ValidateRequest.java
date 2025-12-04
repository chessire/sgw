package com.cas.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 검증 Request
 * uid는 Header로 전달됨
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateRequest {
    
    /**
     * CDN 게임 데이터 MD5 Hash
     */
    private String dataFileMD5;
}

