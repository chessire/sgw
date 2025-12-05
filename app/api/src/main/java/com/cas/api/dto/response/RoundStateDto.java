package com.cas.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 라운드 상태 Response DTO
 * API 명세의 RoundState 구조
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundStateDto {
    
    /**
     * 현재 라운드 정보
     */
    private CurrentRoundDto currentRound;
    
    /**
     * 정산 보고서
     */
    private SettlementDto settlement;
    
    /**
     * 포트폴리오 전체 스냅샷
     */
    private PortfolioResponseDto portfolio;
    
    /**
     * 라운드 시작 정보 (뉴스, 시장 변동 등)
     */
    private RoundStartDto roundStart;
    
    /**
     * 게임 상태 정보 (NPC, 보험, 영상시청 등)
     */
    private GameStatusDto gameStatus;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentRoundDto {
        private Integer roundNo;
    }
}

