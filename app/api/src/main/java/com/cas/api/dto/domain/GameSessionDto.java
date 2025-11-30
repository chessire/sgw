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
}

