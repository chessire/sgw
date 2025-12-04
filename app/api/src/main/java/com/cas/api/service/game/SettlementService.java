package com.cas.api.service.game;

import com.cas.api.dto.domain.*;
import com.cas.api.dto.response.AutoPaymentFailureDto;
import com.cas.api.dto.response.AutoPaymentResultDto;
import com.cas.api.dto.response.StartSettlementResultDto;
import com.cas.api.service.financial.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 정산 계산 Service
 * - 라운드 시작 정산: 월급 지급, 생활비 차감, 이자/배당 지급
 * - 라운드 종료 정산: 포트폴리오 평가, 순자산 계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {
    
    private final DepositService depositService;
    private final StockService stockService;
    private final FundService fundService;
    private final BondService bondService;
    private final PensionService pensionService;
    private final PortfolioService portfolioService;
    
    /**
     * 라운드 시작 정산
     * 1. 월급 지급 (2라운드부터)
     * 2. 생활비 차감 (2라운드부터)
     * 3. 보험료 차감 (가입 시)
     * 4. 대출 이자 차감 (대출 시)
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     * @return 정산 후 현금
     */
    public Long processStartSettlement(GameSessionDto session, PortfolioDto portfolio) {
        log.info("Processing start settlement: uid={}, round={}", 
            session.getUid(), session.getCurrentRound());
        
        long cash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        int currentRound = session.getCurrentRound();
        
        // 1라운드는 초기 자금만 있고 월급/생활비 처리 없음
        if (currentRound == 1) {
            log.info("Round 1: No salary/expense settlement (initial cash only)");
            portfolio.setCash(cash);
            return cash;
        }
        
        // 1. 월급 지급 (2라운드부터)
        long salary = session.getMonthlySalary();
        cash += salary;
        log.debug("Salary added: +{}, cash={}", salary, cash);
        
        // 2. 생활비 차감 (2라운드부터)
        long livingExpense = session.getMonthlyLiving();
        cash -= livingExpense;
        log.debug("Living expense deducted: -{}, cash={}", livingExpense, cash);
        
        // 3. 보험료 차감 (가입 시)
        if (Boolean.TRUE.equals(session.getInsuranceSubscribed())) {
            long insurancePremium = session.getMonthlyInsurancePremium() != null 
                ? session.getMonthlyInsurancePremium() 
                : 5000L; // 기본값
            cash -= insurancePremium;
            log.debug("Insurance premium deducted: -{}, cash={}", insurancePremium, cash);
        }
        
        // 4. 대출 이자 차감 (대출 시)
        if (Boolean.TRUE.equals(session.getLoanUsed())) {
            // TODO: LoanDto 추가 후 구현
            // long loanInterest = pensionService.calculateLoanMonthlyInterest(loanPrincipal);
            // cash -= loanInterest;
            log.debug("Loan interest deduction skipped (not implemented yet)");
        }
        
        portfolio.setCash(cash);
        log.info("Start settlement completed: cash={}", cash);
        
        return cash;
    }
    
    /**
     * 라운드 시작 정산 (자동 납입 포함)
     * 1. 월급 지급 (2라운드부터)
     * 2. 생활비 차감 (2라운드부터)
     * 3. 대출 이자 차감 (소액, 반드시 성공)
     * 4. 연금 납입 (소액, 반드시 성공)
     * 5. 보험료 납입 (소액, 반드시 성공)
     * 6. 적금 납입 (잔액 부족 시 실패 가능)
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     * @return 정산 결과 (현금 + 자동 납입 결과)
     */
    public StartSettlementResultDto processStartSettlementWithAutoPayment(
            GameSessionDto session, PortfolioDto portfolio) {
        
        log.info("Processing start settlement with auto payments: uid={}, round={}", 
            session.getUid(), session.getCurrentRound());
        
        long cash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        int currentRound = session.getCurrentRound();
        
        AutoPaymentResultDto autoPayments = AutoPaymentResultDto.builder()
            .savings(new ArrayList<>())
            .pensions(new ArrayList<>())
            .build();
        List<AutoPaymentFailureDto> failures = new ArrayList<>();
        
        // 1라운드는 초기 자금만 있고 월급/생활비 처리 없음
        if (currentRound == 1) {
            log.info("Round 1: No salary/expense settlement (initial cash only)");
            portfolio.setCash(cash);
            return StartSettlementResultDto.builder()
                .cashAfterSettlement(cash)
                .autoPayments(autoPayments)
                .autoPaymentFailures(failures)
                .build();
        }
        
        // 1. 월급 지급 (2라운드부터)
        long salary = session.getMonthlySalary();
        cash += salary;
        log.debug("Salary added: +{}, cash={}", salary, cash);
        
        // 2. 생활비 차감 (2라운드부터)
        long livingExpense = session.getMonthlyLiving();
        cash -= livingExpense;
        log.debug("Living expense deducted: -{}, cash={}", livingExpense, cash);
        
        // 3. 대출 이자 차감 (소액이라 반드시 성공)
        if (Boolean.TRUE.equals(session.getLoanUsed()) && portfolio.getLoans() != null) {
            for (LoanDto loan : portfolio.getLoans()) {
                if (loan.getRemainingMonths() != null && loan.getRemainingMonths() > 0) {
                    // 월 이자 = 대출금 × 연 5% ÷ 12
                    long monthlyInterest = Math.round(loan.getPrincipal() * 0.05 / 12);
                    cash -= monthlyInterest;
                    
                    autoPayments.setLoanInterest(AutoPaymentResultDto.PaymentItemDto.builder()
                        .productKey(loan.getLoanId())
                        .name("대출 이자")
                        .amount(monthlyInterest)
                        .success(true)
                        .build());
                    
                    log.debug("Loan interest deducted: -{}, cash={}", monthlyInterest, cash);
                }
            }
        }
        
        // 4. 연금 납입 (소액이라 반드시 성공, 중도해지 불가)
        if (portfolio.getPensions() != null) {
            for (PensionDto pension : portfolio.getPensions()) {
                // 이미 가입된 연금만 처리 (subscriptionRound 이후)
                if (pension.getSubscriptionRound() != null && 
                    pension.getSubscriptionRound() < currentRound) {
                    
                    long monthlyAmount = pension.getMonthlyContribution() != null 
                        ? pension.getMonthlyContribution() : 50000L;
                    
                    cash -= monthlyAmount;
                    
                    // 납입 횟수 증가
                    int paymentCount = pension.getPaymentCount() != null ? pension.getPaymentCount() : 0;
                    pension.setPaymentCount(paymentCount + 1);
                    
                    // 총 납입액 업데이트
                    long totalContribution = pension.getTotalContribution() != null 
                        ? pension.getTotalContribution() : 0L;
                    pension.setTotalContribution(totalContribution + monthlyAmount);
                    
                    // 평가액 업데이트 (연 3.2% 월복리)
                    updatePensionEvaluation(pension);
                    
                    autoPayments.getPensions().add(AutoPaymentResultDto.PaymentItemDto.builder()
                        .productKey(pension.getProductKey())
                        .name(pension.getName() != null ? pension.getName() : "개인연금")
                        .amount(monthlyAmount)
                        .success(true)
                        .build());
                    
                    log.debug("Pension payment deducted: productKey={}, amount={}, cash={}", 
                        pension.getProductKey(), monthlyAmount, cash);
                }
            }
        }
        
        // 5. 보험료 납입 (소액이라 반드시 성공)
        if (Boolean.TRUE.equals(session.getInsuranceSubscribed())) {
            long insurancePremium = session.getMonthlyInsurancePremium() != null 
                ? session.getMonthlyInsurancePremium() 
                : 5000L;
            cash -= insurancePremium;
            
            autoPayments.setInsurance(AutoPaymentResultDto.PaymentItemDto.builder()
                .productKey("INSURANCE")
                .name("보험")
                .amount(insurancePremium)
                .success(true)
                .build());
            
            log.debug("Insurance premium deducted: -{}, cash={}", insurancePremium, cash);
        }
        
        // 6. 적금 납입 (잔액 부족 시 실패 가능)
        if (portfolio.getSavings() != null) {
            for (SavingDto saving : portfolio.getSavings()) {
                // 이미 가입된 적금만 처리 (subscriptionRound 이후)
                if (saving.getSubscriptionRound() != null && 
                    saving.getSubscriptionRound() < currentRound) {
                    
                    long monthlyAmount = saving.getMonthlyAmount() != null 
                        ? saving.getMonthlyAmount() : 0L;
                    
                    String savingName = getSavingName(saving.getProductKey());
                    
                    // 잔액 확인
                    if (cash >= monthlyAmount) {
                        // 납입 성공
                        cash -= monthlyAmount;
                        
                        // 납입 횟수 증가
                        int paymentCount = saving.getPaymentCount() != null ? saving.getPaymentCount() : 0;
                        saving.setPaymentCount(paymentCount + 1);
                        
                        // 잔액 업데이트
                        long balance = saving.getBalance() != null ? saving.getBalance() : 0L;
                        saving.setBalance(balance + monthlyAmount);
                        
                        autoPayments.getSavings().add(AutoPaymentResultDto.PaymentItemDto.builder()
                            .productKey(saving.getProductKey())
                            .name(savingName)
                            .amount(monthlyAmount)
                            .success(true)
                            .build());
                        
                        log.debug("Saving payment success: productKey={}, amount={}, cash={}", 
                            saving.getProductKey(), monthlyAmount, cash);
                    } else {
                        // 납입 실패 - 잔액 부족
                        autoPayments.getSavings().add(AutoPaymentResultDto.PaymentItemDto.builder()
                            .productKey(saving.getProductKey())
                            .name(savingName)
                            .amount(monthlyAmount)
                            .success(false)
                            .failReason("INSUFFICIENT_CASH")
                            .build());
                        
                        // 실패 팝업 정보 추가
                        failures.add(AutoPaymentFailureDto.builder()
                            .type("SAVING")
                            .productKey(saving.getProductKey())
                            .name(savingName)
                            .amount(monthlyAmount)
                            .message("현금이 부족하여 " + savingName + " 자동이체가 실행되지 않았습니다.")
                            .actions(Arrays.asList("CANCEL_PRODUCT", "CANCEL_OTHER"))
                            .build());
                        
                        log.warn("Saving payment failed: productKey={}, amount={}, cash={}", 
                            saving.getProductKey(), monthlyAmount, cash);
                    }
                }
            }
        }
        
        portfolio.setCash(cash);
        log.info("Start settlement with auto payments completed: cash={}, failures={}", 
            cash, failures.size());
        
        return StartSettlementResultDto.builder()
            .cashAfterSettlement(cash)
            .autoPayments(autoPayments)
            .autoPaymentFailures(failures)
            .build();
    }
    
    /**
     * 연금 평가액 업데이트 (연 3.2% 월복리)
     */
    private void updatePensionEvaluation(PensionDto pension) {
        // 간단한 계산: 총 납입액 + 이자
        // 실제로는 각 납입금별로 경과 개월수에 따른 복리 계산 필요
        long totalContribution = pension.getTotalContribution() != null ? pension.getTotalContribution() : 0L;
        int paymentCount = pension.getPaymentCount() != null ? pension.getPaymentCount() : 0;
        
        // 평균 경과 기간으로 간략화 (실제 구현 시 더 정밀하게)
        double avgMonths = paymentCount / 2.0;
        double monthlyRate = 0.032 / 12;
        double accumulatedReturn = totalContribution * monthlyRate * avgMonths;
        
        pension.setEvaluationAmount(totalContribution + Math.round(accumulatedReturn));
    }
    
    /**
     * 적금 상품명 조회
     */
    private String getSavingName(String productKey) {
        if (productKey == null) return "적금";
        
        switch (productKey) {
            case "SAVING_A":
                return "적금 A";
            case "SAVING_B":
                return "적금 B";
            case "SAVING_C":
                return "적금 C";
            default:
                return productKey.contains("_") 
                    ? productKey.replace("_", " ") 
                    : "적금";
        }
    }
    
    /**
     * 배당금 지급 정산
     * 
     * @param portfolio 포트폴리오
     * @param currentRound 현재 라운드
     * @return 총 배당금
     */
    public Long processDividendSettlement(PortfolioDto portfolio, int currentRound) {
        log.info("Processing dividend settlement: round={}", currentRound);
        
        long totalDividend = 0L;
        long cash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        
        // 주식 배당금 (3라운드, 6라운드)
        if (currentRound == 3 || currentRound == 6) {
            if (portfolio.getStocks() != null) {
                for (StockHoldingDto stock : portfolio.getStocks()) {
                    // TODO: 주식별 배당률 정보 필요 (CSV 또는 상수)
                    // BigDecimal dividend = stockService.calculateQuarterlyDividend(
                    //     BigDecimal.valueOf(stock.getCurrentPrice()), 
                    //     stock.getQuantity(), 
                    //     quarterlyRate
                    // );
                    // totalDividend += dividend.longValue();
                    log.debug("Stock dividend calculation skipped (rate info needed)");
                }
            }
        }
        
        // 펀드 배당금 (6라운드)
        if (currentRound == 6) {
            if (portfolio.getFunds() != null) {
                for (FundHoldingDto fund : portfolio.getFunds()) {
                    // TODO: 펀드별 배당률 정보 필요
                    log.debug("Fund dividend calculation skipped (rate info needed)");
                }
            }
        }
        
        // 채권 분기 이자 지급 (회사채, 3개월마다)
        if (portfolio.getBonds() != null) {
            for (BondDto bond : portfolio.getBonds()) {
                if (bond.getElapsedMonths() != null && bond.getElapsedMonths() % 3 == 0) {
                    // TODO: 채권별 이자 계산
                    log.debug("Bond interest calculation skipped");
                }
            }
        }
        
        cash += totalDividend;
        portfolio.setCash(cash);
        
        log.info("Dividend settlement completed: totalDividend={}, cash={}", 
            totalDividend, cash);
        
        return totalDividend;
    }
    
    /**
     * 라운드 종료 정산
     * 1. 만기 상품 정산
     * 2. 포트폴리오 평가
     * 
     * @param portfolio 포트폴리오
     * @param currentRound 현재 라운드
     */
    public void processEndSettlement(PortfolioDto portfolio, int currentRound) {
        log.info("Processing end settlement: round={}", currentRound);
        
        long cash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        long maturedTotal = 0L;
        
        // 1. 예금 만기 정산
        if (portfolio.getDeposits() != null) {
            final long[] maturedAmount = {0L};
            portfolio.getDeposits().removeIf(deposit -> {
                if (deposit.getMaturityRound() != null && 
                    deposit.getMaturityRound() == currentRound) {
                    
                    // 만기 금액 계산
                    long amount = deposit.getBalance();
                    maturedAmount[0] += amount;
                    
                    log.debug("Deposit matured: productKey={}, amount={}", 
                        deposit.getProductKey(), amount);
                    return true; // 리스트에서 제거
                }
                return false;
            });
            maturedTotal += maturedAmount[0];
        }
        
        // 2. 적금 만기 정산
        if (portfolio.getSavings() != null) {
            final long[] maturedAmount = {0L};
            portfolio.getSavings().removeIf(saving -> {
                if (saving.getMaturityRound() != null && 
                    saving.getMaturityRound() == currentRound) {
                    
                    long amount = saving.getBalance();
                    maturedAmount[0] += amount;
                    
                    log.debug("Saving matured: productKey={}, amount={}", 
                        saving.getProductKey(), amount);
                    return true;
                }
                return false;
            });
            maturedTotal += maturedAmount[0];
        }
        
        // 3. 채권 만기 정산
        if (portfolio.getBonds() != null) {
            final long[] maturedAmount = {0L};
            portfolio.getBonds().removeIf(bond -> {
                if (bond.getMaturityRound() != null && 
                    bond.getMaturityRound() == currentRound) {
                    
                    long amount = bond.getEvaluationAmount();
                    maturedAmount[0] += amount;
                    
                    log.debug("Bond matured: bondId={}, amount={}", 
                        bond.getBondId(), amount);
                    return true;
                }
                return false;
            });
            maturedTotal += maturedAmount[0];
        }
        
        cash += maturedTotal;
        portfolio.setCash(cash);
        
        // 4. 포트폴리오 평가 업데이트
        portfolioService.updatePortfolioSummary(portfolio);
        
        log.info("End settlement completed: matured={}, cash={}, totalAssets={}, netWorth={}", 
            maturedTotal, cash, portfolio.getTotalAssets(), portfolio.getNetWorth());
    }
    
    /**
     * 게임 종료 시 최종 정산
     * 
     * @param portfolio 포트폴리오
     * @return 최종 순자산
     */
    public Long processFinalSettlement(PortfolioDto portfolio) {
        log.info("Processing final settlement");
        
        long cash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        
        // 1. 모든 예적금 강제 정산
        if (portfolio.getDeposits() != null) {
            for (DepositDto deposit : portfolio.getDeposits()) {
                cash += deposit.getBalance();
            }
            portfolio.getDeposits().clear();
        }
        
        if (portfolio.getSavings() != null) {
            for (SavingDto saving : portfolio.getSavings()) {
                cash += saving.getBalance();
            }
            portfolio.getSavings().clear();
        }
        
        // 2. 모든 채권 강제 정산
        if (portfolio.getBonds() != null) {
            for (BondDto bond : portfolio.getBonds()) {
                cash += bond.getEvaluationAmount();
            }
            portfolio.getBonds().clear();
        }
        
        // 3. 모든 주식 매도
        if (portfolio.getStocks() != null) {
            for (StockHoldingDto stock : portfolio.getStocks()) {
                cash += stock.getEvaluationAmount();
            }
            portfolio.getStocks().clear();
        }
        
        // 4. 모든 펀드 환매
        if (portfolio.getFunds() != null) {
            for (FundHoldingDto fund : portfolio.getFunds()) {
                cash += fund.getEvaluationAmount();
            }
            portfolio.getFunds().clear();
        }
        
        // 5. 연금 수령
        if (portfolio.getPensions() != null) {
            for (PensionDto pension : portfolio.getPensions()) {
                cash += pension.getEvaluationAmount();
            }
            portfolio.getPensions().clear();
        }
        
        // 6. 대출 상환
        // TODO: LoanDto 추가 후 구현
        
        portfolio.setCash(cash);
        portfolioService.updatePortfolioSummary(portfolio);
        
        Long finalNetWorth = portfolio.getNetWorth();
        log.info("Final settlement completed: finalNetWorth={}", finalNetWorth);
        
        return finalNetWorth;
    }
    
    /**
     * 현금 부족 여부 확인
     * 
     * @param portfolio 포트폴리오
     * @param requiredAmount 필요 금액
     * @return 부족 여부
     */
    public boolean isInsufficientCash(PortfolioDto portfolio, long requiredAmount) {
        long cash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
        boolean insufficient = cash < requiredAmount;
        
        if (insufficient) {
            log.warn("Insufficient cash: required={}, available={}", requiredAmount, cash);
        }
        
        return insufficient;
    }
}

