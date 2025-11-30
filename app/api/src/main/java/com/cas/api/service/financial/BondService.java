package com.cas.api.service.financial;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 채권 계산 Service
 * - 국채: 3개월 만기, 만기 시 일시 이자 지급
 * - 회사채: 6개월 만기, 3개월마다 분기 이자 지급
 * - 시장가격 = 투자금 × [1 + (채권금리 - 기준금리) × 잔여개월/12]
 */
@Slf4j
@Service
public class BondService {
    
    private static final BigDecimal GOVERNMENT_BOND_RATE = new BigDecimal("0.03"); // 국채 3%
    private static final BigDecimal CORPORATE_BOND_RATE = new BigDecimal("0.035"); // 회사채 3.5%
    private static final int GOVERNMENT_BOND_TERM = 3; // 국채 3개월
    private static final int CORPORATE_BOND_TERM = 6; // 회사채 6개월
    private static final int MONTHS_PER_YEAR = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 0; // 원 단위
    
    /**
     * 채권 시장가격 계산
     * 
     * @param principal 투자금
     * @param bondRate 채권금리 (연이율, 예: 0.03)
     * @param baseRate 현재 기준금리 (예: 0.015)
     * @param remainingMonths 잔여 개월 수
     * @return 시장가격
     */
    public BigDecimal calculateMarketPrice(BigDecimal principal, BigDecimal bondRate, 
                                            BigDecimal baseRate, int remainingMonths) {
        log.debug("Calculating bond market price: principal={}, bondRate={}, baseRate={}, remainingMonths={}", 
            principal, bondRate, baseRate, remainingMonths);
        
        // 시장가격 = 투자금 × [1 + (채권금리 - 기준금리) × 잔여개월/12]
        BigDecimal rateDiff = bondRate.subtract(baseRate);
        BigDecimal timeFactor = new BigDecimal(remainingMonths)
            .divide(new BigDecimal(MONTHS_PER_YEAR), 10, ROUNDING);
        BigDecimal multiplier = BigDecimal.ONE.add(rateDiff.multiply(timeFactor));
        
        BigDecimal result = principal.multiply(multiplier).setScale(SCALE, ROUNDING);
        
        log.debug("Bond market price: {}", result);
        return result;
    }
    
    /**
     * 채권 경과이자 계산
     * 
     * @param principal 투자금
     * @param bondRate 채권금리
     * @param elapsedMonths 경과 개월 수
     * @return 경과이자
     */
    public BigDecimal calculateAccruedInterest(BigDecimal principal, BigDecimal bondRate, 
                                                 int elapsedMonths) {
        log.debug("Calculating bond accrued interest: principal={}, bondRate={}, elapsedMonths={}", 
            principal, bondRate, elapsedMonths);
        
        // 경과이자 = 투자금 × 채권금리 × 경과개월/12
        BigDecimal timeFactor = new BigDecimal(elapsedMonths)
            .divide(new BigDecimal(MONTHS_PER_YEAR), 10, ROUNDING);
        
        BigDecimal result = principal
            .multiply(bondRate)
            .multiply(timeFactor)
            .setScale(SCALE, ROUNDING);
        
        log.debug("Bond accrued interest: {}", result);
        return result;
    }
    
    /**
     * 국채 중도해지 금액 계산
     * 
     * @param principal 투자금
     * @param baseRate 현재 기준금리
     * @param elapsedMonths 경과 개월 수
     * @param remainingMonths 잔여 개월 수
     * @return 중도해지 금액
     */
    public BigDecimal calculateGovernmentBondEarlyWithdrawal(BigDecimal principal, 
                                                              BigDecimal baseRate, 
                                                              int elapsedMonths, 
                                                              int remainingMonths) {
        log.debug("Calculating government bond early withdrawal: principal={}, baseRate={}, elapsedMonths={}, remainingMonths={}", 
            principal, baseRate, elapsedMonths, remainingMonths);
        
        if (elapsedMonths == 0) {
            // 가입 라운드 내: 원금만 반환
            log.debug("Same round withdrawal: returning principal only");
            return principal;
        }
        
        // 중도해지 = 시장가격 + 경과이자
        BigDecimal marketPrice = calculateMarketPrice(principal, GOVERNMENT_BOND_RATE, 
            baseRate, remainingMonths);
        BigDecimal accruedInterest = calculateAccruedInterest(principal, GOVERNMENT_BOND_RATE, 
            elapsedMonths);
        
        BigDecimal result = marketPrice.add(accruedInterest);
        log.debug("Government bond early withdrawal: {}", result);
        return result;
    }
    
    /**
     * 국채 만기 금액 계산
     * 
     * @param principal 투자금
     * @return 만기 금액
     */
    public BigDecimal calculateGovernmentBondMaturity(BigDecimal principal) {
        log.debug("Calculating government bond maturity: principal={}", principal);
        
        // 만기 금액 = 투자금 × (1 + 0.03 × 3/12) = 투자금 × 1.0075
        BigDecimal interestRate = GOVERNMENT_BOND_RATE
            .multiply(new BigDecimal(GOVERNMENT_BOND_TERM))
            .divide(new BigDecimal(MONTHS_PER_YEAR), 10, ROUNDING);
        
        BigDecimal result = principal
            .multiply(BigDecimal.ONE.add(interestRate))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Government bond maturity: {}", result);
        return result;
    }
    
    /**
     * 회사채 분기 이자 계산
     * 
     * @param principal 투자금
     * @return 분기 이자
     */
    public BigDecimal calculateCorporateBondQuarterlyInterest(BigDecimal principal) {
        log.debug("Calculating corporate bond quarterly interest: principal={}", principal);
        
        // 분기 이자 = 투자금 × 0.00875 (3.5% ÷ 4)
        BigDecimal quarterlyRate = CORPORATE_BOND_RATE
            .divide(new BigDecimal(4), 10, ROUNDING);
        
        BigDecimal result = principal
            .multiply(quarterlyRate)
            .setScale(SCALE, ROUNDING);
        
        log.debug("Corporate bond quarterly interest: {}", result);
        return result;
    }
    
    /**
     * 회사채 중도해지 금액 계산
     * 
     * @param principal 투자금
     * @param baseRate 현재 기준금리
     * @param elapsedMonths 경과 개월 수
     * @param remainingMonths 잔여 개월 수
     * @param receivedQuarterlyInterest 기수령 분기이자
     * @return 중도해지 금액
     */
    public BigDecimal calculateCorporateBondEarlyWithdrawal(BigDecimal principal, 
                                                             BigDecimal baseRate, 
                                                             int elapsedMonths, 
                                                             int remainingMonths,
                                                             BigDecimal receivedQuarterlyInterest) {
        log.debug("Calculating corporate bond early withdrawal: principal={}, baseRate={}, elapsedMonths={}, remainingMonths={}, received={}", 
            principal, baseRate, elapsedMonths, remainingMonths, receivedQuarterlyInterest);
        
        if (elapsedMonths == 0) {
            // 가입 라운드 내: 원금만 반환
            log.debug("Same round withdrawal: returning principal only");
            return principal;
        }
        
        // 중도해지 = 시장가격 + 경과이자 + 기수령_분기이자
        BigDecimal marketPrice = calculateMarketPrice(principal, CORPORATE_BOND_RATE, 
            baseRate, remainingMonths);
        
        // 잔여 경과개월 (분기 지급 후 잔여분)
        int remainingElapsedMonths = elapsedMonths % 3;
        BigDecimal accruedInterest = calculateAccruedInterest(principal, CORPORATE_BOND_RATE, 
            remainingElapsedMonths);
        
        BigDecimal result = marketPrice.add(accruedInterest).add(receivedQuarterlyInterest);
        log.debug("Corporate bond early withdrawal: {}", result);
        return result;
    }
    
    /**
     * 회사채 만기 금액 계산
     * 
     * @param principal 투자금
     * @return 만기 금액
     */
    public BigDecimal calculateCorporateBondMaturity(BigDecimal principal) {
        log.debug("Calculating corporate bond maturity: principal={}", principal);
        
        // 만기 금액 = 투자금 × (1 + 0.035 × 6/12) = 투자금 × 1.0175
        BigDecimal interestRate = CORPORATE_BOND_RATE
            .multiply(new BigDecimal(CORPORATE_BOND_TERM))
            .divide(new BigDecimal(MONTHS_PER_YEAR), 10, ROUNDING);
        
        BigDecimal result = principal
            .multiply(BigDecimal.ONE.add(interestRate))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Corporate bond maturity: {}", result);
        return result;
    }
    
    /**
     * 게임종료 시 강제정산 금액 계산
     * 
     * @param principal 투자금
     * @param bondRate 채권금리
     * @param elapsedMonths 경과 개월 수
     * @param receivedInterest 기수령 이자 (분기이자 등)
     * @return 강제정산 금액
     */
    public BigDecimal calculateForcedSettlement(BigDecimal principal, BigDecimal bondRate, 
                                                  int elapsedMonths, BigDecimal receivedInterest) {
        log.debug("Calculating bond forced settlement: principal={}, bondRate={}, elapsedMonths={}, received={}", 
            principal, bondRate, elapsedMonths, receivedInterest);
        
        // 강제정산 = 원금 + 경과이자 + 기수령이자
        int remainingElapsedMonths = elapsedMonths % 3; // 분기 지급 후 잔여분
        BigDecimal accruedInterest = calculateAccruedInterest(principal, bondRate, 
            remainingElapsedMonths);
        
        BigDecimal result = principal.add(accruedInterest).add(receivedInterest);
        log.debug("Bond forced settlement: {}", result);
        return result;
    }
}

