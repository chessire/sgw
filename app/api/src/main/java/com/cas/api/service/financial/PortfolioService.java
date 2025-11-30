package com.cas.api.service.financial;

import com.cas.api.dto.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 포트폴리오 평가 Service
 * - 총 자산 계산
 * - 순자산 계산 (자산 - 부채)
 * - 자산 배분 비율 계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {
    
    private final StockService stockService;
    private final FundService fundService;
    
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 0; // 원 단위
    private static final int PERCENTAGE_SCALE = 2; // 비율 소수점 2자리
    
    /**
     * 총 자산 계산
     * 
     * @param portfolio 포트폴리오
     * @return 총 자산
     */
    public Long calculateTotalAssets(PortfolioDto portfolio) {
        log.debug("Calculating total assets");
        
        long total = 0L;
        
        // 현금
        if (portfolio.getCash() != null) {
            total += portfolio.getCash();
        }
        
        // 예금
        if (portfolio.getDeposits() != null) {
            for (DepositDto deposit : portfolio.getDeposits()) {
                if (deposit.getBalance() != null) {
                    total += deposit.getBalance();
                }
            }
        }
        
        // 적금
        if (portfolio.getSavings() != null) {
            for (SavingDto saving : portfolio.getSavings()) {
                if (saving.getBalance() != null) {
                    total += saving.getBalance();
                }
            }
        }
        
        // 채권
        if (portfolio.getBonds() != null) {
            for (BondDto bond : portfolio.getBonds()) {
                if (bond.getEvaluationAmount() != null) {
                    total += bond.getEvaluationAmount();
                }
            }
        }
        
        // 주식
        if (portfolio.getStocks() != null) {
            for (StockHoldingDto stock : portfolio.getStocks()) {
                if (stock.getEvaluationAmount() != null) {
                    total += stock.getEvaluationAmount();
                }
            }
        }
        
        // 펀드
        if (portfolio.getFunds() != null) {
            for (FundHoldingDto fund : portfolio.getFunds()) {
                if (fund.getEvaluationAmount() != null) {
                    total += fund.getEvaluationAmount();
                }
            }
        }
        
        // 연금
        if (portfolio.getPensions() != null) {
            for (PensionDto pension : portfolio.getPensions()) {
                if (pension.getEvaluationAmount() != null) {
                    total += pension.getEvaluationAmount();
                }
            }
        }
        
        log.debug("Total assets: {}", total);
        return total;
    }
    
    /**
     * 총 부채 계산
     * 
     * @param portfolio 포트폴리오
     * @return 총 부채
     */
    public Long calculateTotalLiabilities(PortfolioDto portfolio) {
        log.debug("Calculating total liabilities");
        
        long total = 0L;
        
        // TODO: 대출 필드 추가 필요 (현재 PortfolioDto에 없음)
        // if (portfolio.getLoans() != null) {
        //     for (LoanDto loan : portfolio.getLoans()) {
        //         if (loan.getPrincipal() != null) {
        //             total += loan.getPrincipal();
        //         }
        //     }
        // }
        
        log.debug("Total liabilities: {}", total);
        return total;
    }
    
    /**
     * 순자산 계산 (자산 - 부채)
     * 
     * @param portfolio 포트폴리오
     * @return 순자산
     */
    public Long calculateNetWorth(PortfolioDto portfolio) {
        log.debug("Calculating net worth");
        
        Long totalAssets = calculateTotalAssets(portfolio);
        Long totalLiabilities = calculateTotalLiabilities(portfolio);
        
        Long result = totalAssets - totalLiabilities;
        log.debug("Net worth: {}", result);
        return result;
    }
    
    /**
     * 자산 배분 비율 계산
     * 
     * @param portfolio 포트폴리오
     * @return 자산 배분 비율 DTO
     */
    public AllocationDto calculateAllocation(PortfolioDto portfolio) {
        log.debug("Calculating asset allocation");
        
        Long totalAssets = calculateTotalAssets(portfolio);
        
        if (totalAssets == 0) {
            log.warn("Total assets is zero, returning zero allocation");
            return AllocationDto.builder()
                .cashRatio(BigDecimal.ZERO)
                .depositRatio(BigDecimal.ZERO)
                .bondRatio(BigDecimal.ZERO)
                .stockRatio(BigDecimal.ZERO)
                .fundRatio(BigDecimal.ZERO)
                .pensionRatio(BigDecimal.ZERO)
                .build();
        }
        
        // 각 자산별 합계
        long cash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        
        long depositSum = portfolio.getDeposits() != null 
            ? portfolio.getDeposits().stream()
                .mapToLong(d -> d.getBalance() != null ? d.getBalance() : 0L)
                .sum()
            : 0L;
        
        long savingSum = portfolio.getSavings() != null
            ? portfolio.getSavings().stream()
                .mapToLong(s -> s.getBalance() != null ? s.getBalance() : 0L)
                .sum()
            : 0L;
        
        long bondSum = portfolio.getBonds() != null
            ? portfolio.getBonds().stream()
                .mapToLong(b -> b.getEvaluationAmount() != null ? b.getEvaluationAmount() : 0L)
                .sum()
            : 0L;
        
        long stockSum = portfolio.getStocks() != null
            ? portfolio.getStocks().stream()
                .mapToLong(s -> s.getEvaluationAmount() != null ? s.getEvaluationAmount() : 0L)
                .sum()
            : 0L;
        
        long fundSum = portfolio.getFunds() != null
            ? portfolio.getFunds().stream()
                .mapToLong(f -> f.getEvaluationAmount() != null ? f.getEvaluationAmount() : 0L)
                .sum()
            : 0L;
        
        long pensionSum = portfolio.getPensions() != null
            ? portfolio.getPensions().stream()
                .mapToLong(p -> p.getEvaluationAmount() != null ? p.getEvaluationAmount() : 0L)
                .sum()
            : 0L;
        
        // 비율 계산 (백분율, 소수점 4자리)
        BigDecimal cashRatio = calculateRatio(cash, totalAssets);
        BigDecimal depositRatio = calculateRatio(depositSum + savingSum, totalAssets);
        BigDecimal bondRatio = calculateRatio(bondSum, totalAssets);
        BigDecimal stockRatio = calculateRatio(stockSum, totalAssets);
        BigDecimal fundRatio = calculateRatio(fundSum, totalAssets);
        BigDecimal pensionRatio = calculateRatio(pensionSum, totalAssets);
        
        AllocationDto allocation = AllocationDto.builder()
            .cashRatio(cashRatio)
            .depositRatio(depositRatio)
            .bondRatio(bondRatio)
            .stockRatio(stockRatio)
            .fundRatio(fundRatio)
            .pensionRatio(pensionRatio)
            .build();
        
        log.debug("Asset allocation: {}", allocation);
        return allocation;
    }
    
    /**
     * 비율 계산 헬퍼 메서드
     * 
     * @param part 부분 금액
     * @param total 전체 금액
     * @return 비율 (0.0 ~ 1.0, 소수점 4자리)
     */
    private BigDecimal calculateRatio(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(part)
            .divide(BigDecimal.valueOf(total), 4, ROUNDING);
    }
    
    /**
     * 포트폴리오 요약 정보 업데이트
     * 
     * @param portfolio 포트폴리오
     */
    public void updatePortfolioSummary(PortfolioDto portfolio) {
        log.debug("Updating portfolio summary");
        
        Long totalAssets = calculateTotalAssets(portfolio);
        Long totalLiabilities = calculateTotalLiabilities(portfolio);
        Long netWorth = calculateNetWorth(portfolio);
        AllocationDto allocation = calculateAllocation(portfolio);
        
        portfolio.setTotalAssets(totalAssets);
        portfolio.setTotalLiabilities(totalLiabilities);
        portfolio.setNetWorth(netWorth);
        portfolio.setAllocation(allocation);
        
        log.debug("Portfolio summary updated: totalAssets={}, netWorth={}", 
            totalAssets, netWorth);
    }
}

