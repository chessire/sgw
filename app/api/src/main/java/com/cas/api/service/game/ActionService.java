package com.cas.api.service.game;

import com.cas.api.constant.GameConstants;
import com.cas.api.dto.domain.*;
import com.cas.api.enums.GameMode;
import com.cas.api.service.financial.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 플레이어 액션 처리 Service
 * - 금융상품 매수/매도
 * - 대출/보험 가입
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionService {
    
    private final MarketEventService marketEventService;
    private final DepositService depositService;
    private final StockService stockService;
    private final FundService fundService;
    private final BondService bondService;
    private final PensionService pensionService;
    
    /**
     * 플레이어 액션 처리
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     * @param actions 액션 맵
     */
    public void processActions(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> actions) {
        log.info("Processing player actions: uid={}, round={}", 
            session.getUid(), session.getCurrentRound());
        
        if (actions == null || actions.isEmpty()) {
            log.debug("No actions to process");
            return;
        }
        
        // 1. 예금 가입
        processDepositActions(session, portfolio, actions);
        
        // 2. 적금 가입
        processSavingActions(session, portfolio, actions);
        
        // 3. 채권 매수
        processBondActions(session, portfolio, actions);
        
        // 4. 주식 매수/매도
        processStockActions(session, portfolio, actions);
        
        // 5. 펀드 매수/매도
        processFundActions(session, portfolio, actions);
        
        // 6. 연금 가입
        processPensionActions(session, portfolio, actions);
        
        // 7. 대출 실행
        processLoanActions(session, portfolio, actions);
        
        // 8. 보험 가입
        processInsuranceActions(session, actions);
        
        log.info("Actions processed successfully");
    }
    
    /**
     * 예금 가입 처리
     */
    private void processDepositActions(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> actions) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> deposits = (List<Map<String, Object>>) actions.get("deposits");
        
        if (deposits == null || deposits.isEmpty()) {
            return;
        }
        
        for (Map<String, Object> deposit : deposits) {
            String productKey = (String) deposit.get("productKey");
            Long amount = getLongValue(deposit.get("amount"));
            
            if (amount == null || amount <= 0) {
                log.warn("Invalid deposit amount: {}", amount);
                continue;
            }
            
            // 현금 확인
            if (portfolio.getCash() < amount) {
                log.warn("Insufficient cash for deposit: cash={}, amount={}", 
                    portfolio.getCash(), amount);
                continue;
            }
            
            // 예금 생성 (고정 금리, 모드별 만기)
            int maturityMonths = (session.getGameMode() == GameMode.TUTORIAL) 
                ? GameConstants.TUTORIAL_DEPOSIT_MATURITY_MONTHS 
                : GameConstants.COMPETITION_DEPOSIT_MATURITY_MONTHS;
            
            // 만기는 최대 라운드를 초과할 수 없음
            int calculatedMaturity = session.getCurrentRound() + maturityMonths;
            int maxRound = session.getGameMode().getMaxRounds();
            int maturityRound = Math.min(calculatedMaturity, maxRound);
            
            // 예상 만기금액 계산 (우대금리 적용)
            BigDecimal expectedMaturity = depositService.calculateDepositMaturity(
                BigDecimal.valueOf(amount),
                GameConstants.DEPOSIT_BASE_RATE,
                maturityMonths
            );
            
            DepositDto newDeposit = DepositDto.builder()
                .productKey(productKey)
                .name(getDepositName(productKey))
                .principal(amount)
                .balance(amount)
                .expectedMaturityAmount(expectedMaturity.longValue())
                .interestRate(GameConstants.DEPOSIT_BASE_RATE)
                .subscriptionRound(session.getCurrentRound())
                .maturityRound(maturityRound)
                .build();
            
            portfolio.getDeposits().add(newDeposit);
            portfolio.setCash(portfolio.getCash() - amount);
            
            log.info("Deposit created: productKey={}, amount={}", productKey, amount);
        }
    }
    
    /**
     * 적금 가입 처리
     */
    private void processSavingActions(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> actions) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> savings = (List<Map<String, Object>>) actions.get("savings");
        
        if (savings == null || savings.isEmpty()) {
            return;
        }
        
        for (Map<String, Object> saving : savings) {
            String productKey = (String) saving.get("productKey");
            Long monthlyAmount = getLongValue(saving.get("monthlyAmount"));
            
            if (monthlyAmount == null || monthlyAmount <= 0) {
                log.warn("Invalid saving amount: {}", monthlyAmount);
                continue;
            }
            
            // 현금 확인 (첫 달 납입)
            if (portfolio.getCash() < monthlyAmount) {
                log.warn("Insufficient cash for saving: cash={}, amount={}", 
                    portfolio.getCash(), monthlyAmount);
                continue;
            }
            
            // 적금 생성 (고정 금리, 모드별 만기)
            BigDecimal interestRate;
            int maturityMonths;
            
            if (productKey.equals("SAVING_A")) {
                interestRate = GameConstants.SAVING_A_BASE_RATE;
                maturityMonths = (session.getGameMode() == GameMode.TUTORIAL) 
                    ? GameConstants.TUTORIAL_SAVING_A_MATURITY_MONTHS 
                    : GameConstants.COMPETITION_SAVING_A_MATURITY_MONTHS;
            } else { // SAVING_B
                interestRate = GameConstants.SAVING_B_BASE_RATE;
                maturityMonths = (session.getGameMode() == GameMode.TUTORIAL) 
                    ? GameConstants.TUTORIAL_SAVING_B_MATURITY_MONTHS 
                    : GameConstants.COMPETITION_SAVING_B_MATURITY_MONTHS;
            }
            
            // 만기는 최대 라운드를 초과할 수 없음
            int calculatedMaturity = session.getCurrentRound() + maturityMonths;
            int maxRound = session.getGameMode().getMaxRounds();
            int maturityRound = Math.min(calculatedMaturity, maxRound);
            
            // 예상 만기금액 계산 (우대금리 적용)
            BigDecimal expectedMaturity = depositService.calculateSavingMaturity(
                BigDecimal.valueOf(monthlyAmount),
                interestRate,
                maturityMonths
            );
            
            SavingDto newSaving = SavingDto.builder()
                .productKey(productKey)
                .name(getSavingName(productKey))
                .monthlyAmount(monthlyAmount)
                .balance(monthlyAmount)
                .expectedMaturityAmount(expectedMaturity.longValue())
                .interestRate(interestRate)
                .subscriptionRound(session.getCurrentRound())
                .maturityRound(maturityRound)
                .paymentCount(1)
                .build();
            
            portfolio.getSavings().add(newSaving);
            portfolio.setCash(portfolio.getCash() - monthlyAmount);
            
            log.info("Saving created: productKey={}, monthlyAmount={}", productKey, monthlyAmount);
        }
    }
    
    /**
     * 채권 가입/해지 처리
     */
    private void processBondActions(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> actions) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bonds = (List<Map<String, Object>>) actions.get("bonds");
        
        if (bonds == null || bonds.isEmpty()) {
            return;
        }
        
        for (Map<String, Object> bond : bonds) {
            String bondId = (String) bond.get("bondId");
            String action = (String) bond.get("action");
            Long amount = getLongValue(bond.get("amount"));
            
            if (amount == null || amount <= 0) {
                log.warn("Invalid bond amount: {}", amount);
                continue;
            }
            
            // 액션 처리
            if ("SUBSCRIBE".equalsIgnoreCase(action)) {
                // 채권 가입
                processBondSubscribe(session, portfolio, bondId, amount);
            } else if ("CANCEL".equalsIgnoreCase(action)) {
                // 채권 해지 (FIFO)
                processBondCancel(session, portfolio, bondId, amount);
            } else {
                log.warn("Unknown bond action: {}", action);
            }
        }
    }
    
    /**
     * 채권 가입 처리
     */
    private void processBondSubscribe(GameSessionDto session, PortfolioDto portfolio, 
                                       String bondId, Long amount) {
        // 현금 확인
        if (portfolio.getCash() < amount) {
            log.warn("Insufficient cash for bond: cash={}, amount={}", 
                portfolio.getCash(), amount);
            return;
        }
        
        // 현재 기준금리 조회 (경쟁모드는 baseRateCase 반영)
        BigDecimal baseRate = marketEventService.getBaseRate(session);
        
        // 채권 금리 = 기준금리 + 스프레드 (모드별 만기)
        BigDecimal interestRate;
        int maturityMonths;
        
        if (bondId.equals("BOND_NATIONAL")) {
            interestRate = baseRate.add(GameConstants.BOND_NATIONAL_SPREAD);
            maturityMonths = (session.getGameMode() == GameMode.TUTORIAL) 
                ? GameConstants.TUTORIAL_BOND_NATIONAL_MATURITY_MONTHS 
                : GameConstants.COMPETITION_BOND_NATIONAL_MATURITY_MONTHS;
        } else { // BOND_CORPORATE
            interestRate = baseRate.add(GameConstants.BOND_CORPORATE_SPREAD);
            maturityMonths = (session.getGameMode() == GameMode.TUTORIAL) 
                ? GameConstants.TUTORIAL_BOND_CORPORATE_MATURITY_MONTHS 
                : GameConstants.COMPETITION_BOND_CORPORATE_MATURITY_MONTHS;
        }
        
        BondDto newBond = BondDto.builder()
            .bondId(bondId)
            .name(getBondName(bondId))
            .faceValue(amount)
            .evaluationAmount(amount)
            .interestRate(interestRate)
            .subscriptionRound(session.getCurrentRound())
            .maturityRound(session.getCurrentRound() + maturityMonths)
            .elapsedMonths(0)
            .build();
        
        portfolio.getBonds().add(newBond);
        portfolio.setCash(portfolio.getCash() - amount);
        
        log.info("Bond subscribed: bondId={}, amount={}", bondId, amount);
    }
    
    /**
     * 채권 해지 처리 (FIFO - 먼저 가입한 것부터 해지)
     */
    private void processBondCancel(GameSessionDto session, PortfolioDto portfolio, 
                                     String bondId, Long cancelAmount) {
        if (portfolio.getBonds() == null || portfolio.getBonds().isEmpty()) {
            log.warn("No bonds to cancel: bondId={}", bondId);
            return;
        }
        
        // 현재 기준금리 조회
        BigDecimal baseRate = marketEventService.getBaseRate(session);
        
        // 해당 bondId의 채권들을 가입 순서대로 정렬 (subscriptionRound 오름차순)
        List<BondDto> targetBonds = portfolio.getBonds().stream()
            .filter(b -> bondId.equals(b.getBondId()))
            .sorted((b1, b2) -> Integer.compare(
                b1.getSubscriptionRound() != null ? b1.getSubscriptionRound() : 0,
                b2.getSubscriptionRound() != null ? b2.getSubscriptionRound() : 0
            ))
            .collect(java.util.stream.Collectors.toList());
        
        if (targetBonds.isEmpty()) {
            log.warn("No matching bonds to cancel: bondId={}", bondId);
            return;
        }
        
        long remainingCancelAmount = cancelAmount;
        long totalReceivedAmount = 0L;
        List<BondDto> bondsToRemove = new ArrayList<>();
        
        // FIFO 방식으로 해지
        for (BondDto bond : targetBonds) {
            if (remainingCancelAmount <= 0) {
                break;
            }
            
            Long faceValue = bond.getFaceValue() != null ? bond.getFaceValue() : 0L;
            
            if (faceValue <= 0) {
                continue;
            }
            
            // 해지 금액 계산
            int currentRound = session.getCurrentRound();
            int subscriptionRound = bond.getSubscriptionRound() != null ? bond.getSubscriptionRound() : currentRound;
            int elapsedMonths = currentRound - subscriptionRound;
            int maturityRound = bond.getMaturityRound() != null ? bond.getMaturityRound() : currentRound;
            int remainingMonths = Math.max(0, maturityRound - currentRound);
            
            // 중도 해지 금액 계산
            BigDecimal redemptionAmount;
            if (bondId.equals("BOND_NATIONAL")) {
                // 국채 중도 해지
                redemptionAmount = bondService.calculateGovernmentBondEarlyWithdrawal(
                    BigDecimal.valueOf(faceValue),
                    baseRate,
                    elapsedMonths,
                    remainingMonths
                );
            } else {
                // 회사채 중도 해지 (분기 이자 고려)
                int quarterCount = elapsedMonths / 3;
                BigDecimal receivedQuarterlyInterest = bondService.calculateCorporateBondQuarterlyInterest(
                    BigDecimal.valueOf(faceValue)
                ).multiply(BigDecimal.valueOf(quarterCount));
                
                redemptionAmount = bondService.calculateCorporateBondEarlyWithdrawal(
                    BigDecimal.valueOf(faceValue),
                    baseRate,
                    elapsedMonths,
                    remainingMonths,
                    receivedQuarterlyInterest
                );
            }
            
            // 반올림 처리 (BondService에서 이미 반올림되어 있지만 명시적으로 처리)
            long redemptionAmountLong = redemptionAmount.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
            
            // 이 채권을 전부 또는 일부 해지
            if (faceValue <= remainingCancelAmount) {
                // 전부 해지
                totalReceivedAmount += redemptionAmountLong;
                remainingCancelAmount -= faceValue;
                bondsToRemove.add(bond);
                
                log.info("Bond fully cancelled: bondId={}, faceValue={}, received={}", 
                    bondId, faceValue, redemptionAmountLong);
            } else {
                // 일부 해지 (비율 계산)
                double ratio = (double) remainingCancelAmount / faceValue;
                long partialRedemption = (long) (redemptionAmountLong * ratio);
                
                // 채권을 분할: 해지된 부분은 제거, 남은 부분은 업데이트
                totalReceivedAmount += partialRedemption;
                
                // 남은 채권 금액 업데이트
                long remainingFaceValue = faceValue - remainingCancelAmount;
                bond.setFaceValue(remainingFaceValue);
                bond.setEvaluationAmount(remainingFaceValue);
                
                log.info("Bond partially cancelled: bondId={}, cancelled={}, remaining={}, received={}", 
                    bondId, remainingCancelAmount, remainingFaceValue, partialRedemption);
                
                remainingCancelAmount = 0;
                break;
            }
        }
        
        // 제거할 채권 삭제
        portfolio.getBonds().removeAll(bondsToRemove);
        
        // 현금 추가
        portfolio.setCash(portfolio.getCash() + totalReceivedAmount);
        
        log.info("Bond cancellation completed: bondId={}, totalReceived={}, remainingCancel={}", 
            bondId, totalReceivedAmount, remainingCancelAmount);
        
        if (remainingCancelAmount > 0) {
            log.warn("Could not cancel full amount: bondId={}, remainingCancel={}", 
                bondId, remainingCancelAmount);
        }
    }
    
    /**
     * 주식 매수/매도 처리
     */
    private void processStockActions(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> actions) {
        // 매수
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stockBuys = (List<Map<String, Object>>) actions.get("stockBuys");
        
        if (stockBuys != null && !stockBuys.isEmpty()) {
            for (Map<String, Object> buy : stockBuys) {
                processStockBuy(session, portfolio, buy);
            }
        }
        
        // 매도
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stockSells = (List<Map<String, Object>>) actions.get("stockSells");
        
        if (stockSells != null && !stockSells.isEmpty()) {
            for (Map<String, Object> sell : stockSells) {
                processStockSell(session, portfolio, sell);
            }
        }
    }
    
    /**
     * 주식 매수
     */
    private void processStockBuy(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> buy) {
        String stockId = (String) buy.get("stockId");
        Integer quantity = getIntValue(buy.get("quantity"));
        
        if (quantity == null || quantity <= 0) {
            log.warn("Invalid stock quantity: {}", quantity);
            return;
        }
        
        // 현재가 조회
        Long currentPrice = marketEventService.getCurrentStockPrice(
            stockId, session.getGameMode(), session.getCurrentRound());
        
        Long totalCost = currentPrice * quantity;
        
        // 현금 확인
        if (portfolio.getCash() < totalCost) {
            log.warn("Insufficient cash for stock: cash={}, cost={}", 
                portfolio.getCash(), totalCost);
            return;
        }
        
        // 기존 보유 주식 찾기
        StockHoldingDto existingStock = portfolio.getStocks().stream()
            .filter(s -> s.getStockId().equals(stockId))
            .findFirst()
            .orElse(null);
        
        if (existingStock != null) {
            // 기존 보유 주식에 추가
            long totalQuantity = existingStock.getQuantity() + quantity;
            long totalPurchaseAmount = (existingStock.getAvgPrice() * existingStock.getQuantity()) 
                + (currentPrice * quantity);
            long newAvgPrice = totalPurchaseAmount / totalQuantity;
            
            existingStock.setQuantity((int) totalQuantity);
            existingStock.setAvgPrice(newAvgPrice);
            existingStock.setEvaluationAmount(currentPrice * totalQuantity);
            existingStock.setProfitLoss(existingStock.getEvaluationAmount() - totalPurchaseAmount);
            existingStock.setReturnRate((double) existingStock.getProfitLoss() / totalPurchaseAmount);
        } else {
            // 신규 매수
            StockHoldingDto newStock = StockHoldingDto.builder()
                .stockId(stockId)
                .name(getStockName(stockId))
                .quantity(quantity)
                .avgPrice(currentPrice)
                .currentPrice(currentPrice)
                .evaluationAmount(totalCost)
                .profitLoss(0L)
                .returnRate(0.0)
                .build();
            
            portfolio.getStocks().add(newStock);
        }
        
        portfolio.setCash(portfolio.getCash() - totalCost);
        
        log.info("Stock purchased: stockId={}, quantity={}, price={}", 
            stockId, quantity, currentPrice);
    }
    
    /**
     * 주식 매도
     */
    private void processStockSell(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> sell) {
        String stockId = (String) sell.get("stockId");
        Integer quantity = getIntValue(sell.get("quantity"));
        
        if (quantity == null || quantity <= 0) {
            log.warn("Invalid stock quantity: {}", quantity);
            return;
        }
        
        // 보유 주식 찾기
        StockHoldingDto stock = portfolio.getStocks().stream()
            .filter(s -> s.getStockId().equals(stockId))
            .findFirst()
            .orElse(null);
        
        if (stock == null) {
            log.warn("Stock not found: stockId={}", stockId);
            return;
        }
        
        if (stock.getQuantity() < quantity) {
            log.warn("Insufficient stock quantity: have={}, sell={}", 
                stock.getQuantity(), quantity);
            return;
        }
        
        // 현재가 조회
        Long currentPrice = marketEventService.getCurrentStockPrice(
            stockId, session.getGameMode(), session.getCurrentRound());
        
        Long sellAmount = currentPrice * quantity;
        
        // 매도 처리
        if (stock.getQuantity() == quantity) {
            // 전량 매도
            portfolio.getStocks().remove(stock);
        } else {
            // 일부 매도
            stock.setQuantity(stock.getQuantity() - quantity);
            long totalCost = stock.getAvgPrice() * stock.getQuantity();
            stock.setEvaluationAmount(currentPrice * stock.getQuantity());
            stock.setProfitLoss(stock.getEvaluationAmount() - totalCost);
            stock.setReturnRate((double) stock.getProfitLoss() / totalCost);
        }
        
        portfolio.setCash(portfolio.getCash() + sellAmount);
        
        log.info("Stock sold: stockId={}, quantity={}, price={}", 
            stockId, quantity, currentPrice);
    }
    
    /**
     * 펀드 매수/매도 처리
     */
    private void processFundActions(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> actions) {
        // 매수
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fundBuys = (List<Map<String, Object>>) actions.get("fundBuys");
        
        if (fundBuys != null && !fundBuys.isEmpty()) {
            for (Map<String, Object> buy : fundBuys) {
                processFundBuy(session, portfolio, buy);
            }
        }
        
        // 매도
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fundSells = (List<Map<String, Object>>) actions.get("fundSells");
        
        if (fundSells != null && !fundSells.isEmpty()) {
            for (Map<String, Object> sell : fundSells) {
                processFundSell(session, portfolio, sell);
            }
        }
    }
    
    /**
     * 펀드 매수
     */
    private void processFundBuy(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> buy) {
        String fundId = (String) buy.get("fundId");
        Long amount = getLongValue(buy.get("amount"));
        
        if (amount == null || amount <= 0) {
            log.warn("Invalid fund amount: {}", amount);
            return;
        }
        
        // 현재 NAV 조회
        Long currentNav = marketEventService.getCurrentFundNav(
            fundId, session.getGameMode(), session.getCurrentRound());
        
        // 좌수 계산 (금액 / NAV, 반올림)
        int quantity = (int) Math.round((double) amount / currentNav);
        if (quantity <= 0) {
            log.warn("Insufficient amount for fund: amount={}, nav={}", amount, currentNav);
            return;
        }
        
        Long totalCost = currentNav * quantity;
        
        // 현금 확인
        if (portfolio.getCash() < totalCost) {
            log.warn("Insufficient cash for fund: cash={}, cost={}", 
                portfolio.getCash(), totalCost);
            return;
        }
        
        // 기존 보유 펀드 찾기
        FundHoldingDto existingFund = portfolio.getFunds().stream()
            .filter(f -> f.getFundId().equals(fundId))
            .findFirst()
            .orElse(null);
        
        if (existingFund != null) {
            // 기존 보유 펀드에 추가
            long totalShares = existingFund.getShares() + quantity;
            long totalPurchaseAmount = (existingFund.getAvgNav() * existingFund.getShares()) 
                + (currentNav * quantity);
            long newAvgNav = totalPurchaseAmount / totalShares;
            
            existingFund.setShares((int) totalShares);
            existingFund.setAvgNav(newAvgNav);
            existingFund.setEvaluationAmount(currentNav * totalShares);
            existingFund.setProfitLoss(existingFund.getEvaluationAmount() - totalPurchaseAmount);
            existingFund.setReturnRate((double) existingFund.getProfitLoss() / totalPurchaseAmount);
        } else {
            // 신규 매수
            FundHoldingDto newFund = FundHoldingDto.builder()
                .fundId(fundId)
                .name(getFundName(fundId))
                .shares(quantity)
                .avgNav(currentNav)
                .currentNav(currentNav)
                .evaluationAmount(totalCost)
                .profitLoss(0L)
                .returnRate(0.0)
                .build();
            
            portfolio.getFunds().add(newFund);
        }
        
        portfolio.setCash(portfolio.getCash() - totalCost);
        
        log.info("Fund purchased: fundId={}, quantity={}, nav={}", 
            fundId, quantity, currentNav);
    }
    
    /**
     * 펀드 매도 (금액 단위)
     */
    private void processFundSell(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> sell) {
        String fundId = (String) sell.get("fundId");
        Long amount = getLongValue(sell.get("amount"));
        
        if (amount == null || amount <= 0) {
            log.warn("Invalid fund amount: {}", amount);
            return;
        }
        
        // 보유 펀드 찾기
        FundHoldingDto fund = portfolio.getFunds().stream()
            .filter(f -> f.getFundId().equals(fundId))
            .findFirst()
            .orElse(null);
        
        if (fund == null) {
            log.warn("Fund not found: fundId={}", fundId);
            return;
        }
        
        // 현재 NAV 조회
        Long currentNav = marketEventService.getCurrentFundNav(
            fundId, session.getGameMode(), session.getCurrentRound());
        
        // 좌수 계산 (금액 / NAV, 반올림)
        int quantity = (int) Math.round((double) amount / currentNav);
        if (quantity <= 0) {
            log.warn("Insufficient amount for fund sell: amount={}, nav={}", amount, currentNav);
            return;
        }
        
        if (fund.getShares() < quantity) {
            log.warn("Insufficient fund shares: have={}, need={}", 
                fund.getShares(), quantity);
            return;
        }
        
        // 실제 매도 금액 (좌수 * NAV)
        Long sellAmount = currentNav * quantity;
        
        // 매도 처리
        if (fund.getShares() == quantity) {
            // 전량 매도
            portfolio.getFunds().remove(fund);
        } else {
            // 일부 매도
            fund.setShares(fund.getShares() - quantity);
            long totalCost = fund.getAvgNav() * fund.getShares();
            fund.setEvaluationAmount(currentNav * fund.getShares());
            fund.setProfitLoss(fund.getEvaluationAmount() - totalCost);
            fund.setReturnRate((double) fund.getProfitLoss() / totalCost);
        }
        
        portfolio.setCash(portfolio.getCash() + sellAmount);
        
        log.info("Fund sold: fundId={}, requestAmount={}, quantity={}, sellAmount={}, nav={}", 
            fundId, amount, quantity, sellAmount, currentNav);
    }
    
    /**
     * 연금 가입 처리
     */
    private void processPensionActions(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> actions) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pensions = (List<Map<String, Object>>) actions.get("pensions");
        
        if (pensions == null || pensions.isEmpty()) {
            return;
        }
        
        for (Map<String, Object> pension : pensions) {
            String pensionId = (String) pension.get("pensionId");
            Long monthlyAmount = getLongValue(pension.get("monthlyAmount"));
            
            if (monthlyAmount == null || monthlyAmount <= 0) {
                log.warn("Invalid pension amount: {}", monthlyAmount);
                continue;
            }
            
            // 현금 확인 (첫 달 납입)
            if (portfolio.getCash() < monthlyAmount) {
                log.warn("Insufficient cash for pension: cash={}, amount={}", 
                    portfolio.getCash(), monthlyAmount);
                continue;
            }
            
            // 연금 생성
            PensionDto newPension = PensionDto.builder()
                .pensionId(pensionId)
                .name("연금저축")
                .monthlyAmount(monthlyAmount)
                .evaluationAmount(monthlyAmount)
                .interestRate(GameConstants.PENSION_BASE_RATE)
                .subscriptionRound(session.getCurrentRound())
                .build();
            
            portfolio.getPensions().add(newPension);
            portfolio.setCash(portfolio.getCash() - monthlyAmount);
            
            log.info("Pension created: pensionId={}, monthlyAmount={}", pensionId, monthlyAmount);
        }
    }
    
    /**
     * 대출 실행 처리
     */
    private void processLoanActions(GameSessionDto session, PortfolioDto portfolio, Map<String, Object> actions) {
        @SuppressWarnings("unchecked")
        Map<String, Object> loan = (Map<String, Object>) actions.get("loan");
        
        if (loan == null || loan.isEmpty()) {
            return;
        }
        
        // 이미 대출이 있는지 확인
        if (session.getLoanUsed() != null && session.getLoanUsed()) {
            log.warn("Loan already used");
            return;
        }
        
        Long amount = getLongValue(loan.get("amount"));
        
        if (amount == null || amount <= 0) {
            log.warn("Invalid loan amount: {}", amount);
            return;
        }
        
        // 대출 생성
        LoanDto newLoan = LoanDto.builder()
            .loanId("LOAN_01")
            .productKey("LOAN_NORMAL")
            .name("단기대출")
            .principal(amount)
            .remainingBalance(amount)
            .interestRate(GameConstants.LOAN_ANNUAL_RATE)
            .executionRound(session.getCurrentRound())
            .maturityRound(session.getCurrentRound() + GameConstants.LOAN_PERIOD_MONTHS)
            .build();
        
        session.setLoanInfo(newLoan);
        session.setLoanUsed(true);
        portfolio.setCash(portfolio.getCash() + amount);
        
        log.info("Loan executed: amount={}", amount);
    }
    
    /**
     * 보험 가입 처리
     */
    private void processInsuranceActions(GameSessionDto session, Map<String, Object> actions) {
        // 배열 형식으로 받기
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> insuranceSubscribes = (List<Map<String, Object>>) actions.get("insuranceSubscribes");
        
        if (insuranceSubscribes != null && !insuranceSubscribes.isEmpty()) {
            // 이미 가입되어 있지 않은 경우에만 가입
            if (session.getInsuranceSubscribed() == null || !session.getInsuranceSubscribed()) {
                session.setInsuranceSubscribed(true);
                session.setMonthlyInsurancePremium(
                    session.getGameMode() == GameMode.TUTORIAL 
                        ? GameConstants.TUTORIAL_INSURANCE_PREMIUM 
                        : GameConstants.COMPETITION_INSURANCE_PREMIUM
                );
                
                log.info("Insurance subscribed: premium={}", session.getMonthlyInsurancePremium());
            }
        }
        
        // 하위 호환성: 기존 방식도 지원
        Boolean subscribeInsurance = (Boolean) actions.get("subscribeInsurance");
        if (subscribeInsurance != null && subscribeInsurance && 
            (session.getInsuranceSubscribed() == null || !session.getInsuranceSubscribed())) {
            
            session.setInsuranceSubscribed(true);
            session.setMonthlyInsurancePremium(
                session.getGameMode() == GameMode.TUTORIAL 
                    ? GameConstants.TUTORIAL_INSURANCE_PREMIUM 
                    : GameConstants.COMPETITION_INSURANCE_PREMIUM
            );
            
            log.info("Insurance subscribed (legacy): premium={}", session.getMonthlyInsurancePremium());
        }
    }
    
    // Helper methods
    
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Double) {
            return ((Double) value).longValue();
        }
        return null;
    }
    
    private Integer getIntValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        return null;
    }
    
    private String getDepositName(String productKey) {
        return productKey.equals("DEPOSIT_01") ? "예금" : "예금";
    }
    
    private String getSavingName(String productKey) {
        return productKey.equals("SAVING_A") ? "적금 A" : "적금 B";
    }
    
    private String getBondName(String bondId) {
        return bondId.equals("BOND_01") ? "국채" : "회사채";
    }
    
    private String getStockName(String stockId) {
        // TODO: 실제 주식명 매핑
        return stockId;
    }
    
    private String getFundName(String fundId) {
        switch (fundId) {
            case "FUND_01": return "성장형 펀드";
            case "FUND_02": return "안정형 펀드";
            case "FUND_03": return "고위험 고수익 펀드";
            default: return fundId;
        }
    }
}

