package com.cas.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 게임 상태 Response DTO
 * 게임 진행 상태 정보를 담는 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStatusDto {
    
    // ============================================
    // NPC 정보
    // ============================================
    
    /**
     * NPC 타입 (POYONGI/CHAEWUMI)
     */
    private String npcType;
    
    /**
     * NPC 배정 완료 여부 (튜토리얼)
     */
    private Boolean npcAssignmentCompleted;
    
    /**
     * NPC 선택 완료 여부 (경쟁모드)
     */
    private Boolean npcSelectionCompleted;
    
    // ============================================
    // 진행 상태
    // ============================================
    
    /**
     * 게임 완료 여부
     */
    private Boolean completed;
    
    /**
     * 오프닝 스토리 완료 여부
     */
    private Boolean openingStoryCompleted;
    
    /**
     * 재무성향검사 완료 여부
     */
    private Boolean propensityTestCompleted;
    
    /**
     * 재무 성향 유형 (검사 결과)
     */
    private String propensityType;
    
    /**
     * 결과분석 완료 여부
     */
    private Boolean resultAnalysisCompleted;
    
    // ============================================
    // 보험/대출 상태
    // ============================================
    
    /**
     * 보험 가입 여부
     */
    private Boolean insuranceSubscribed;
    
    /**
     * 월 보험료
     */
    private Long monthlyInsurancePremium;
    
    /**
     * 대출 사용 여부
     */
    private Boolean loanUsed;
    
    /**
     * 불법사금융 사용 여부
     */
    private Boolean illegalLoanUsed;
    
    /**
     * 보험 처리 가능 이벤트 발생 여부
     */
    private Boolean insurableEventOccurred;
    
    // ============================================
    // 조언 사용
    // ============================================
    
    /**
     * 조언 사용 횟수 (최대 3회)
     */
    private Integer adviceUsedCount;
    
    /**
     * 남은 조언 횟수
     */
    private Integer adviceRemaining;
    
    // ============================================
    // 교육 영상 시청 상태 (튜토리얼)
    // ============================================
    
    /**
     * 영상 시청 상태
     */
    private VideoStatusDto videoStatus;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoStatusDto {
        private Boolean depositVideoCompleted;
        private Boolean stockVideoCompleted;
        private Boolean bondVideoCompleted;
        private Boolean pensionVideoCompleted;
        private Boolean fundVideoCompleted;
        private Boolean insuranceVideoCompleted;
    }
    
    // ============================================
    // 우대금리 퀴즈 상태 (튜토리얼)
    // ============================================
    
    /**
     * 퀴즈 정답 상태
     */
    private QuizStatusDto quizStatus;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizStatusDto {
        private Boolean depositQuizPassed;
        private Boolean stockQuizPassed;
        private Boolean bondQuizPassed;
        private Boolean pensionQuizPassed;
        private Boolean fundQuizPassed;
        private Boolean insuranceQuizPassed;
    }
}

