package com.cas.api.dto.domain;

import com.cas.api.enums.GameMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게임 세션 DTO (Redis 저장용)
 * 전체 게임 진행 상태를 저장
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionDto {
    
    /**
     * 사용자 고유 식별자
     */
    private String uid;
    
    /**
     * 게임 모드 (TUTORIAL, COMPETITION)
     */
    private GameMode gameMode;
    
    /**
     * 현재 라운드 번호 (1~6 or 1~12)
     */
    private Integer currentRound;
    
    /**
     * 게임 시작 시간
     */
    private LocalDateTime startedAt;
    
    /**
     * 마지막 업데이트 시간
     */
    private LocalDateTime updatedAt;
    
    /**
     * 게임 완료 여부
     */
    private Boolean completed;
    
    /**
     * 재무 성향 유형 (재무성향검사 결과)
     */
    private String propensityType;
    
    /**
     * NPC 타입 (포용이/채우미)
     */
    private String npcType;
    
    /**
     * 월급 (튜토리얼은 가변, 경쟁모드는 고정)
     */
    private Long monthlySalary;
    
    /**
     * 생활비 (튜토리얼은 가변, 경쟁모드는 고정)
     */
    private Long monthlyLiving;
    
    /**
     * 초기 자본금 (점수 계산용)
     */
    private Long initialCash;
    
    /**
     * 월 보험료 (가입 시)
     */
    private Long monthlyInsurancePremium;
    
    /**
     * 현재 포트폴리오
     */
    private PortfolioDto portfolio;
    
    /**
     * 주식 패턴 (게임 시작 시 결정)
     * Map<stockId, "UP" or "DOWN">
     */
    private java.util.Map<String, String> stockPatterns;
    
    /**
     * 주식 시작 케이스 (경쟁모드 전용, 1~4)
     * Case 1: 2019.01~2020.01
     * Case 2: 2019.02~2020.02
     * Case 3: 2019.03~2020.03
     * Case 4: 2019.04~2020.04
     */
    private Integer stockStartCase;
    
    /**
     * 기준금리 시작 케이스 (경쟁모드 전용, 1~4)
     * 주식 시작 케이스와 동일하게 적용
     */
    private Integer baseRateCase;
    
    /**
     * 조언 사용 횟수 (최대 3회)
     */
    private Integer adviceUsedCount;
    
    /**
     * 보험 가입 여부
     */
    private Boolean insuranceSubscribed;
    
    /**
     * 대출 사용 여부 (최대 1회)
     */
    private Boolean loanUsed;
    
    /**
     * 대출 정보
     */
    private LoanDto loanInfo;
    
    /**
     * 불법사금융 사용 여부 (패널티 적용)
     */
    private Boolean illegalLoanUsed;
    
    /**
     * 보험 처리 가능 이벤트 발생 여부 (12라운드 중 1회만)
     */
    private Boolean insurableEventOccurred;
    
    /**
     * 오프닝 스토리 완료 여부
     */
    private Boolean openingStoryCompleted;
    
    /**
     * 재무성향검사 답안
     */
    private java.util.List<Integer> propensityTestAnswers;
    
    /**
     * 재무성향검사 완료 여부
     */
    private Boolean propensityTestCompleted;
    
    /**
     * 결과분석 완료 여부
     */
    private Boolean resultAnalysisCompleted;
    
    /**
     * NPC 배정 완료 여부 (튜토리얼)
     */
    private Boolean npcAssignmentCompleted;
    
    /**
     * NPC 선택 완료 여부 (경쟁모드)
     */
    private Boolean npcSelectionCompleted;
    
    // ============================================
    // 교육 영상 시청 완료 여부 (튜토리얼)
    // ============================================
    
    /**
     * 예적금 영상 시청 완료 여부
     */
    private Boolean depositVideoCompleted;
    
    /**
     * 주식 영상 시청 완료 여부
     */
    private Boolean stockVideoCompleted;
    
    /**
     * 채권 영상 시청 완료 여부
     */
    private Boolean bondVideoCompleted;
    
    /**
     * 연금 영상 시청 완료 여부
     */
    private Boolean pensionVideoCompleted;
    
    /**
     * 펀드 영상 시청 완료 여부
     */
    private Boolean fundVideoCompleted;
    
    /**
     * 보험 영상 시청 완료 여부
     */
    private Boolean insuranceVideoCompleted;
    
    // ============================================
    // 우대금리 퀴즈 정답 여부 (튜토리얼)
    // ============================================
    
    /**
     * 예적금 퀴즈 정답 (우대금리 적용)
     */
    private Boolean depositQuizPassed;
    
    /**
     * 주식 퀴즈 정답 (우대가격 적용)
     */
    private Boolean stockQuizPassed;
    
    /**
     * 채권 퀴즈 정답 (우대금리 적용)
     */
    private Boolean bondQuizPassed;
    
    /**
     * 연금 퀴즈 정답 (우대금리 적용)
     */
    private Boolean pensionQuizPassed;
    
    /**
     * 펀드 퀴즈 정답 (우대가격 적용)
     */
    private Boolean fundQuizPassed;
    
    /**
     * 보험 퀴즈 정답 (보험료 할인 적용)
     */
    private Boolean insuranceQuizPassed;
    
    // ============================================
    // 업적 시스템
    // ============================================
    
    /**
     * 달성한 업적 ID 목록 (1~20)
     */
    private java.util.Set<Integer> achievedAchievements;
    
    /**
     * 업적 진행 상황 (Key: 업적ID, Value: 진행 상황)
     * 예: "fundTypes" -> 펀드 종류 개수
     */
    private java.util.Map<String, Integer> achievementProgress;
}

