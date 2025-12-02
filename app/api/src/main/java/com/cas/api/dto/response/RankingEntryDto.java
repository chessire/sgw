package com.cas.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 랭킹 항목 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingEntryDto {
    
    /**
     * 랭킹 순위
     */
    private Integer rank;
    
    /**
     * 사용자 닉네임
     */
    private String nickname;
    
    /**
     * 최종 총점
     */
    private Double totalScore;
    
    /**
     * 최종 순자산
     */
    private Long finalNetWorth;
    
    /**
     * 수익률 (%)
     */
    private String returnRate;
    
    /**
     * 본인 여부
     */
    private Boolean isMe;
}

