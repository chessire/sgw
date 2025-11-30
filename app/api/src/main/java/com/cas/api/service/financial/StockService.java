package com.cas.api.service.financial;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 주식 계산 Service
 * - 주식 시가/종가 계산
 * - 배당금 계산 (분기/반기)
 * - 우대 매수가 계산
 */
@Slf4j
@Service
public class StockService {
    
    private static final BigDecimal PREFERENTIAL_RATE = new BigDecimal("0.95"); // 우대 매수 95%
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 0; // 원 단위
    
    /**
     * 주식 종가 계산
     * 
     * @param openPrice 시가
     * @param changeRate 등락률 (예: 0.056 = 5.6%)
     * @return 종가
     */
    public BigDecimal calculateClosePrice(BigDecimal openPrice, BigDecimal changeRate) {
        log.debug("Calculating stock close price: openPrice={}, changeRate={}", 
            openPrice, changeRate);
        
        // 종가 = 시가 × (1 + 등락률)
        BigDecimal result = openPrice
            .multiply(BigDecimal.ONE.add(changeRate))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Stock close price: {}", result);
        return result;
    }
    
    /**
     * 주식 평가 금액 계산
     * 
     * @param currentPrice 현재가
     * @param quantity 보유 수량
     * @return 평가 금액
     */
    public BigDecimal calculateStockValue(BigDecimal currentPrice, int quantity) {
        log.debug("Calculating stock value: currentPrice={}, quantity={}", 
            currentPrice, quantity);
        
        BigDecimal result = currentPrice
            .multiply(new BigDecimal(quantity))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Stock value: {}", result);
        return result;
    }
    
    /**
     * 배당금 계산 (분기)
     * 
     * @param closePrice 종가
     * @param quantity 보유 수량
     * @param quarterlyRate 분기 배당률 (예: 0.00875 = 연 3.5% ÷ 4)
     * @return 배당금
     */
    public BigDecimal calculateQuarterlyDividend(BigDecimal closePrice, int quantity, 
                                                   BigDecimal quarterlyRate) {
        log.debug("Calculating quarterly dividend: closePrice={}, quantity={}, rate={}", 
            closePrice, quantity, quarterlyRate);
        
        // 배당금 = 종가 × 보유수량 × 분기배당률
        BigDecimal result = closePrice
            .multiply(new BigDecimal(quantity))
            .multiply(quarterlyRate)
            .setScale(SCALE, ROUNDING);
        
        log.debug("Quarterly dividend: {}", result);
        return result;
    }
    
    /**
     * 배당금 계산 (반기)
     * 
     * @param closePrice 종가
     * @param quantity 보유 수량
     * @param semiannualRate 반기 배당률 (예: 0.005 = 연 1% ÷ 2)
     * @return 배당금
     */
    public BigDecimal calculateSemiannualDividend(BigDecimal closePrice, int quantity, 
                                                    BigDecimal semiannualRate) {
        log.debug("Calculating semiannual dividend: closePrice={}, quantity={}, rate={}", 
            closePrice, quantity, semiannualRate);
        
        // 배당금 = 종가 × 보유수량 × 반기배당률
        BigDecimal result = closePrice
            .multiply(new BigDecimal(quantity))
            .multiply(semiannualRate)
            .setScale(SCALE, ROUNDING);
        
        log.debug("Semiannual dividend: {}", result);
        return result;
    }
    
    /**
     * 우대 매수가 계산 (퀴즈 정답 시)
     * 
     * @param regularPrice 일반 매수가 (시가)
     * @return 우대 매수가 (95%)
     */
    public BigDecimal calculatePreferentialBuyPrice(BigDecimal regularPrice) {
        log.debug("Calculating preferential buy price: regularPrice={}", regularPrice);
        
        // 우대 매수가 = 시가 × 0.95
        BigDecimal result = regularPrice
            .multiply(PREFERENTIAL_RATE)
            .setScale(SCALE, ROUNDING);
        
        log.debug("Preferential buy price: {}", result);
        return result;
    }
    
    /**
     * 주식 매수 총액 계산
     * 
     * @param price 매수가
     * @param quantity 수량
     * @return 총 매수 금액
     */
    public BigDecimal calculateBuyTotal(BigDecimal price, int quantity) {
        log.debug("Calculating stock buy total: price={}, quantity={}", price, quantity);
        
        BigDecimal result = price
            .multiply(new BigDecimal(quantity))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Stock buy total: {}", result);
        return result;
    }
    
    /**
     * 주식 매도 총액 계산
     * 
     * @param currentPrice 현재가
     * @param quantity 수량
     * @return 총 매도 금액
     */
    public BigDecimal calculateSellTotal(BigDecimal currentPrice, int quantity) {
        log.debug("Calculating stock sell total: currentPrice={}, quantity={}", 
            currentPrice, quantity);
        
        BigDecimal result = currentPrice
            .multiply(new BigDecimal(quantity))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Stock sell total: {}", result);
        return result;
    }
    
    /**
     * 주식 수익률 계산
     * 
     * @param buyPrice 매수가
     * @param currentPrice 현재가
     * @return 수익률 (소수점, 예: 0.15 = 15%)
     */
    public BigDecimal calculateReturnRate(BigDecimal buyPrice, BigDecimal currentPrice) {
        log.debug("Calculating stock return rate: buyPrice={}, currentPrice={}", 
            buyPrice, currentPrice);
        
        // 수익률 = (현재가 - 매수가) / 매수가
        BigDecimal result = currentPrice
            .subtract(buyPrice)
            .divide(buyPrice, 6, ROUNDING);
        
        log.debug("Stock return rate: {}", result);
        return result;
    }
}

