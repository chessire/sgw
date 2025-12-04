package com.cas.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 라운드 시작 정산 결과 DTO
 * - 정산 후 현금 및 자동 납입 결과 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSettlementResultDto {
    
    /**
     * 정산 후 현금
     */
    private Long cashAfterSettlement;
    
    /**
     * 자동 납입 결과
     */
    private AutoPaymentResultDto autoPayments;
    
    /**
     * 자동 납입 실패 목록 (팝업 표시용)
     */
    @Builder.Default
    private List<AutoPaymentFailureDto> autoPaymentFailures = new ArrayList<>();
    
    /**
     * 자동 납입 실패가 있는지 확인
     */
    public boolean hasFailures() {
        return autoPaymentFailures != null && !autoPaymentFailures.isEmpty();
    }
}

