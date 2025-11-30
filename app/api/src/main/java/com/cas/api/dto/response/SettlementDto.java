package com.cas.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 정산 보고서 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDto {
    
    /**
     * 기본 수입
     */
    private BaseIncomeDto baseIncome;
    
    /**
     * 기본 지출
     */
    private BaseExpensesDto baseExpenses;
    
    /**
     * 이자/배당금 (수동 수입)
     */
    private PassiveIncomeDto passiveIncome;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseIncomeDto {
        private Long salary;        // 월급
        private Long total;         // 합계
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseExpensesDto {
        private Long living;        // 생활비
        private Long otherExpenses; // 기타
        private Long total;         // 합계
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassiveIncomeDto {
        private Long depositInterest; // 예금 이자
        private Long savingInterest;  // 적금 이자
        private Long bondInterest;    // 채권 이자
        private Long stockDividend;   // 주식 배당금
        private Long total;           // 합계
    }
}

