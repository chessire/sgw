package com.cas.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 심화정보 구매 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyAdditionalInfoRequest {
    
    /**
     * 라운드 번호
     */
    private Integer roundNo;
    
    /**
     * 정보 키 (game_data.json의 키)
     */
    private String infoKey;
}
