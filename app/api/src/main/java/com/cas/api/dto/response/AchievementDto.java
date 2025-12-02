package com.cas.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 업적 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDto {
    
    /**
     * 업적 ID
     */
    private Integer achievementId;
    
    /**
     * 업적 이름
     */
    private String name;
    
    /**
     * 업적 설명
     */
    private String description;
    
    /**
     * 달성 여부
     */
    private Boolean achieved;
    
    /**
     * 달성 일시 (ISO-8601 형식)
     */
    private String achievedAt;
    
    /**
     * 진행률 (0~100)
     */
    private Integer progress;
}

