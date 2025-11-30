package com.cas.api.constant;

import java.math.BigDecimal;

/**
 * 게임 상수 정의
 */
public class GameConstants {
    
    // ==================== Redis 키 패턴 ====================
    public static final String REDIS_KEY_GAME_SESSION = "game:session:%s:%s";  // uid:mode
    public static final String REDIS_KEY_GAME_ROUND = "game:round:%s:%s:%d";  // uid:mode:roundNo
    public static final String REDIS_KEY_GAME_TEMP = "game:temp:%s";          // uid
    
    // ==================== Redis TTL (초) ====================
    public static final int TTL_ACTIVE_SESSION = 24 * 60 * 60;    // 24시간
    public static final int TTL_COMPLETED_GAME = 7 * 24 * 60 * 60; // 7일
    public static final int TTL_TEMP_DATA = 10 * 60;               // 10분
    
    // ==================== 튜토리얼 기본값 ====================
    public static final long DEFAULT_INITIAL_CASH = 5_000_000L;           // 500만원 (기본값)
    public static final long DEFAULT_MONTHLY_LIVING_EXPENSE = 1_500_000L; // 150만원 (기본값)
    public static final long DEFAULT_MONTHLY_INSURANCE_PREMIUM = 5_000L;  // 5천원
    
    // ==================== 경쟁모드 고정 초기값 ====================
    public static final long COMPETITION_INITIAL_CAPITAL = 5_000_000L;  // 500만원 (1라운드 지원금)
    public static final long COMPETITION_MONTHLY_SALARY = 1_000_000L;   // 100만원
    public static final long COMPETITION_MONTHLY_LIVING = 500_000L;     // 50만원
    
    // ==================== 금융 상품 계산 상수 ====================
    // 예적금 - 고정 금리 (기준금리와 무관)
    public static final BigDecimal DEPOSIT_BASE_RATE = new BigDecimal("0.025");       // 2.5%
    public static final BigDecimal DEPOSIT_PREFERENTIAL_RATE = new BigDecimal("0.027"); // 2.7% (우대)
    public static final BigDecimal SAVING_A_BASE_RATE = new BigDecimal("0.026");      // 2.6%
    public static final BigDecimal SAVING_A_PREFERENTIAL_RATE = new BigDecimal("0.028"); // 2.8% (우대)
    public static final BigDecimal SAVING_B_BASE_RATE = new BigDecimal("0.032");      // 3.2%
    public static final BigDecimal SAVING_B_PREFERENTIAL_RATE = new BigDecimal("0.034"); // 3.4% (우대)
    public static final BigDecimal EARLY_WITHDRAWAL_PENALTY_RATE = new BigDecimal("0.005"); // 0.5%

    // 연금 - 고정 금리
    public static final BigDecimal PENSION_BASE_RATE = new BigDecimal("0.032");       // 3.2%

    // 채권 - 기준금리 연동 (스프레드 방식)
    public static final BigDecimal BOND_NATIONAL_SPREAD = new BigDecimal("0.0125"); // +1.25%p
    public static final BigDecimal BOND_CORPORATE_SPREAD = new BigDecimal("0.0175"); // +1.75%p
    
    // ==================== 금융 상품 만기 (라운드 = 개월) ====================
    // 튜토리얼 만기
    public static final int TUTORIAL_DEPOSIT_MATURITY_MONTHS = 3;        // 예금: 3개월
    public static final int TUTORIAL_SAVING_A_MATURITY_MONTHS = 3;       // 적금 A: 3개월
    public static final int TUTORIAL_SAVING_B_MATURITY_MONTHS = 6;       // 적금 B: 6개월
    public static final int TUTORIAL_BOND_NATIONAL_MATURITY_MONTHS = 3;  // 국채: 3개월
    public static final int TUTORIAL_BOND_CORPORATE_MATURITY_MONTHS = 6; // 회사채: 6개월
    
    // 경쟁모드 만기
    public static final int COMPETITION_DEPOSIT_MATURITY_MONTHS = 6;        // 예금: 6개월
    public static final int COMPETITION_SAVING_A_MATURITY_MONTHS = 6;       // 적금 A: 6개월
    public static final int COMPETITION_SAVING_B_MATURITY_MONTHS = 12;      // 적금 B: 12개월
    public static final int COMPETITION_BOND_NATIONAL_MATURITY_MONTHS = 9;  // 국채: 9개월
    public static final int COMPETITION_BOND_CORPORATE_MATURITY_MONTHS = 12; // 회사채: 12개월
    
    // 연금은 게임 종료 후 지급
    
    // 대출
    public static final BigDecimal LOAN_ANNUAL_RATE = new BigDecimal("0.05");  // 연 5%
    public static final int LOAN_PERIOD_MONTHS = 3;  // 3개월 고정
    public static final int LOAN_MAX_COUNT = 1;      // 게임당 1회
    
    // 보험
    public static final long TUTORIAL_INSURANCE_PREMIUM = 5_000L;     // 튜토리얼 월 5천원
    public static final long COMPETITION_INSURANCE_PREMIUM = 5_000L;  // 경쟁모드 월 5천원 (동일)
    
    // 주식 우대 할인율
    public static final BigDecimal STOCK_PREFERENTIAL_DISCOUNT = new BigDecimal("0.05"); // 5% 할인
    
    // 펀드 우대 할인율
    public static final BigDecimal FUND_PREFERENTIAL_DISCOUNT = new BigDecimal("0.05"); // 5% 할인
    
    // ==================== 인생이벤트 ====================
    public static final String EVENT_LEVEL_LOW = "EVENT_LOW";
    public static final String EVENT_LEVEL_MID = "EVENT_MID";
    public static final String EVENT_LEVEL_HIGH = "EVENT_HIGH";
    
    // 발생 가능 라운드
    public static final int EVENT_LOW_START_ROUND = 1;
    public static final int EVENT_MID_START_ROUND = 4;
    public static final int EVENT_HIGH_START_ROUND = 8;
    
    // ==================== NPC 조언 ====================
    public static final int MAX_ADVICE_COUNT = 3;  // 게임당 최대 3회
    
    // ==================== 심화 정보 ====================
    public static final int MAX_ADDITIONAL_INFO_PER_ROUND = 1;  // 라운드당 1개
    
    // ==================== 랭킹 계산 가중치 ====================
    public static final BigDecimal RANKING_FINANCIAL_WEIGHT = new BigDecimal("0.40");  // 재무관리능력 40%
    public static final BigDecimal RANKING_RISK_WEIGHT = new BigDecimal("0.30");       // 리스크관리 30%
    public static final BigDecimal RANKING_RETURN_WEIGHT = new BigDecimal("0.30");     // 절대수익률 30%
    
    // ==================== 계산 정밀도 ====================
    public static final int SCALE_MONEY = 0;        // 돈은 소수점 없음 (원 단위)
    public static final int SCALE_RATE = 4;         // 이자율은 소수점 4자리
    public static final int SCALE_PERCENTAGE = 2;   // 퍼센트는 소수점 2자리
    
    private GameConstants() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}

