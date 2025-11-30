package com.cas.api.service.financial;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 펀드 계산 Service
 * - 기준가 계산 (포트폴리오 구성 종목의 가중평균)
 * - 배당금 계산 (반기)
 * - 우대 가입가 계산
 */
@Slf4j
@Service
public class FundService {
    
    private static final BigDecimal PREFERENTIAL_RATE = new BigDecimal("0.95"); // 우대 가입 95%
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int SCALE = 0; // 원 단위
    
    /**
     * 펀드 기준가 계산
     * 
     * @param stockPrices 종목별 종가 (종목명 -> 종가)
     * @param weights 종목별 비중 (종목명 -> 비중, 합계 = 1.0)
     * @return 기준가
     */
    public BigDecimal calculateNav(Map<String, BigDecimal> stockPrices, 
                                    Map<String, BigDecimal> weights) {
        log.debug("Calculating fund NAV: stockPrices={}, weights={}", 
            stockPrices, weights);
        
        // 기준가 = Σ(종목 종가 × 비중)
        BigDecimal nav = BigDecimal.ZERO;
        
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            String stockName = entry.getKey();
            BigDecimal weight = entry.getValue();
            BigDecimal price = stockPrices.get(stockName);
            
            if (price == null) {
                log.warn("Stock price not found: {}", stockName);
                continue;
            }
            
            BigDecimal weightedPrice = price.multiply(weight);
            nav = nav.add(weightedPrice);
        }
        
        BigDecimal result = nav.setScale(SCALE, ROUNDING);
        log.debug("Fund NAV: {}", result);
        return result;
    }
    
    /**
     * 펀드 평가 금액 계산
     * 
     * @param currentNav 현재 기준가
     * @param units 보유 좌수
     * @return 평가 금액
     */
    public BigDecimal calculateFundValue(BigDecimal currentNav, int units) {
        log.debug("Calculating fund value: currentNav={}, units={}", 
            currentNav, units);
        
        BigDecimal result = currentNav
            .multiply(new BigDecimal(units))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Fund value: {}", result);
        return result;
    }
    
    /**
     * 펀드 배당금 계산 (반기)
     * 
     * @param currentNav 현재 기준가
     * @param units 보유 좌수
     * @param semiannualRate 반기 배당률 (예: 0.01225 = 연 2.45% ÷ 2)
     * @return 배당금
     */
    public BigDecimal calculateSemiannualDividend(BigDecimal currentNav, int units, 
                                                    BigDecimal semiannualRate) {
        log.debug("Calculating fund semiannual dividend: nav={}, units={}, rate={}", 
            currentNav, units, semiannualRate);
        
        // 배당금 = 기준가 × 보유좌수 × 반기배당률
        BigDecimal result = currentNav
            .multiply(new BigDecimal(units))
            .multiply(semiannualRate)
            .setScale(SCALE, ROUNDING);
        
        log.debug("Fund semiannual dividend: {}", result);
        return result;
    }
    
    /**
     * 우대 가입가 계산 (퀴즈 정답 시)
     * 
     * @param regularNav 일반 기준가
     * @return 우대 가입가 (95%)
     */
    public BigDecimal calculatePreferentialNav(BigDecimal regularNav) {
        log.debug("Calculating preferential NAV: regularNav={}", regularNav);
        
        // 우대 가입가 = 기준가 × 0.95
        BigDecimal result = regularNav
            .multiply(PREFERENTIAL_RATE)
            .setScale(SCALE, ROUNDING);
        
        log.debug("Preferential NAV: {}", result);
        return result;
    }
    
    /**
     * 펀드 가입 총액 계산
     * 
     * @param nav 가입 기준가
     * @param units 좌수
     * @return 총 가입 금액
     */
    public BigDecimal calculateSubscriptionTotal(BigDecimal nav, int units) {
        log.debug("Calculating fund subscription total: nav={}, units={}", nav, units);
        
        BigDecimal result = nav
            .multiply(new BigDecimal(units))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Fund subscription total: {}", result);
        return result;
    }
    
    /**
     * 펀드 환매 총액 계산
     * 
     * @param currentNav 현재 기준가
     * @param units 좌수
     * @return 총 환매 금액
     */
    public BigDecimal calculateRedemptionTotal(BigDecimal currentNav, int units) {
        log.debug("Calculating fund redemption total: currentNav={}, units={}", 
            currentNav, units);
        
        BigDecimal result = currentNav
            .multiply(new BigDecimal(units))
            .setScale(SCALE, ROUNDING);
        
        log.debug("Fund redemption total: {}", result);
        return result;
    }
    
    /**
     * 펀드 수익률 계산
     * 
     * @param subscriptionNav 가입 기준가
     * @param currentNav 현재 기준가
     * @return 수익률 (소수점, 예: 0.15 = 15%)
     */
    public BigDecimal calculateReturnRate(BigDecimal subscriptionNav, BigDecimal currentNav) {
        log.debug("Calculating fund return rate: subscriptionNav={}, currentNav={}", 
            subscriptionNav, currentNav);
        
        // 수익률 = (현재 기준가 - 가입 기준가) / 가입 기준가
        BigDecimal result = currentNav
            .subtract(subscriptionNav)
            .divide(subscriptionNav, 6, ROUNDING);
        
        log.debug("Fund return rate: {}", result);
        return result;
    }
    
    /**
     * 펀드 등락률 계산
     * 
     * @param previousNav 이전 기준가
     * @param currentNav 현재 기준가
     * @return 등락률 (소수점, 예: 0.056 = 5.6%)
     */
    public BigDecimal calculateChangeRate(BigDecimal previousNav, BigDecimal currentNav) {
        log.debug("Calculating fund change rate: previousNav={}, currentNav={}", 
            previousNav, currentNav);
        
        // 등락률 = (현재 기준가 - 이전 기준가) / 이전 기준가
        BigDecimal result = currentNav
            .subtract(previousNav)
            .divide(previousNav, 6, ROUNDING);
        
        log.debug("Fund change rate: {}", result);
        return result;
    }
}

