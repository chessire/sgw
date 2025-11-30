package com.cas.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 게임 시작 Request (튜토리얼)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartGameRequest {
    
    /**
     * 수입 정보
     */
    private IncomeDto income;
    
    /**
     * 지출 정보
     */
    private ExpenseDto expense;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncomeDto {
        private Long monthlyIncome;  // 고정 월급
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseDto {
        private Long monthlyFixedExpense;  // 고정 지출
    }
}

