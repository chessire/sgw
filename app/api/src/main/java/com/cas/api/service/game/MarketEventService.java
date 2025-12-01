package com.cas.api.service.game;

import com.cas.api.dto.domain.*;
import com.cas.api.enums.GameMode;
import com.cas.api.service.financial.FundService;
import com.cas.api.service.financial.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 시장 이벤트 Service
 * - 주식 시세 변동
 * - 펀드 기준가 변동
 * - 기준금리 변동
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketEventService {
    
    private final StockService stockService;
    private final FundService fundService;
    
    // 튜토리얼 모드 주식 등락률 (하드코딩)
    // 라운드별, 종목별 등락률
    private static final Map<String, BigDecimal[]> TUTORIAL_STOCK_CHANGE_RATES = new HashMap<>() {{
        // 에버반도체 (UP)
        put("STOCK_01", new BigDecimal[] {
            new BigDecimal("0.056"),  // 1라운드: +5.6%
            new BigDecimal("-0.034"), // 2라운드: -3.4%
            new BigDecimal("0.059"),  // 3라운드: +5.9%
            new BigDecimal("0.007"),  // 4라운드: +0.7%
            new BigDecimal("0.044"),  // 5라운드: +4.4%
            new BigDecimal("0.099")   // 6라운드: +9.9%
        });
        
        // 케어금융 (UP)
        put("STOCK_02", new BigDecimal[] {
            new BigDecimal("0.081"),  // 1라운드: +8.1%
            new BigDecimal("0.183"),  // 2라운드: +18.3%
            new BigDecimal("0.179"),  // 3라운드: +17.9%
            new BigDecimal("-0.088"), // 4라운드: -8.8%
            new BigDecimal("-0.013"), // 5라운드: -1.3%
            new BigDecimal("0.133")   // 6라운드: +13.3%
        });
        
        // 아톰에너지 (UP)
        put("STOCK_03", new BigDecimal[] {
            new BigDecimal("0.085"),  // 1라운드: +8.5%
            new BigDecimal("-0.030"), // 2라운드: -3.0%
            new BigDecimal("-0.021"), // 3라운드: -2.1%
            new BigDecimal("-0.070"), // 4라운드: -7.0%
            new BigDecimal("0.045"),  // 5라운드: +4.5%
            new BigDecimal("0.015")   // 6라운드: +1.5%
        });
        
        // 피크건설 (DOWN)
        put("STOCK_04", new BigDecimal[] {
            new BigDecimal("0.005"),  // 1라운드: +0.5%
            new BigDecimal("-0.160"), // 2라운드: -16.0%
            new BigDecimal("-0.044"), // 3라운드: -4.4%
            new BigDecimal("0.019"),  // 4라운드: +1.9%
            new BigDecimal("-0.064"), // 5라운드: -6.4%
            new BigDecimal("-0.029")  // 6라운드: -2.9%
        });
        
        // 제네틱바이오 (DOWN)
        put("STOCK_05", new BigDecimal[] {
            new BigDecimal("-0.118"), // 1라운드: -11.8%
            new BigDecimal("-0.157"), // 2라운드: -15.7%
            new BigDecimal("-0.027"), // 3라운드: -2.7%
            new BigDecimal("-0.638"), // 4라운드: -63.8%
            new BigDecimal("0.460"),  // 5라운드: +46.0%
            new BigDecimal("-0.061")  // 6라운드: -6.1%
        });
        
        // 비트온엔터 (DOWN)
        put("STOCK_06", new BigDecimal[] {
            new BigDecimal("-0.040"), // 1라운드: -4.0%
            new BigDecimal("-0.100"), // 2라운드: -10.0%
            new BigDecimal("-0.059"), // 3라운드: -5.9%
            new BigDecimal("0.125"),  // 4라운드: +12.5%
            new BigDecimal("-0.009"), // 5라운드: -0.9%
            new BigDecimal("0.039")   // 6라운드: +3.9%
        });
        
        // 웨이브조선 (UP)
        put("STOCK_07", new BigDecimal[] {
            new BigDecimal("0.140"),  // 1라운드: +14.0%
            new BigDecimal("-0.010"), // 2라운드: -1.0%
            new BigDecimal("0.240"),  // 3라운드: +24.0%
            new BigDecimal("0.100"),  // 4라운드: +10.0%
            new BigDecimal("-0.070"), // 5라운드: -7.0%
            new BigDecimal("0.040")   // 6라운드: +4.0%
        });
    }};
    
    // 튜토리얼 모드 주식 시작가
    private static final Map<String, Long> TUTORIAL_STOCK_START_PRICES = new HashMap<>() {{
        put("STOCK_01", 50000L);  // 에버반도체
        put("STOCK_02", 30000L);  // 케어금융
        put("STOCK_03", 15000L);  // 아톰에너지
        put("STOCK_04", 10000L);  // 피크건설
        put("STOCK_05", 5000L);   // 제네틱바이오
        put("STOCK_06", 25000L);  // 비트온엔터
        put("STOCK_07", 20000L);  // 웨이브조선
    }};
    
    // 튜토리얼 모드 기준금리
    private static final BigDecimal[] TUTORIAL_BASE_RATES = new BigDecimal[] {
        new BigDecimal("0.0175"), // 1라운드: 1.75%
        new BigDecimal("0.015"),  // 2라운드: 1.50%
        new BigDecimal("0.015"),  // 3라운드: 1.50%
        new BigDecimal("0.015"),  // 4라운드: 1.50%
        new BigDecimal("0.0125"), // 5라운드: 1.25%
        new BigDecimal("0.0125")  // 6라운드: 1.25%
    };
    
    // 경쟁모드 기준금리 (2019.01 ~ 2020.04, 16개월)
    private static final BigDecimal[] COMPETITION_BASE_RATES_ALL = new BigDecimal[] {
        new BigDecimal("0.0175"), // 2019.01
        new BigDecimal("0.0175"), // 2019.02
        new BigDecimal("0.0175"), // 2019.03
        new BigDecimal("0.0175"), // 2019.04
        new BigDecimal("0.0175"), // 2019.05
        new BigDecimal("0.0175"), // 2019.06
        new BigDecimal("0.015"),  // 2019.07
        new BigDecimal("0.015"),  // 2019.08
        new BigDecimal("0.015"),  // 2019.09
        new BigDecimal("0.0125"), // 2019.10
        new BigDecimal("0.0125"), // 2019.11
        new BigDecimal("0.0125"), // 2020.01
        new BigDecimal("0.0125"), // 2020.02
        new BigDecimal("0.0075"), // 2020.03
        new BigDecimal("0.0075"), // 2020.04
        new BigDecimal("0.0075")  // 추가 (13개월 선택을 위해)
    };
    
    // 경쟁모드 주식 시작가
    private static final Map<String, Long> COMPETITION_STOCK_START_PRICES = new HashMap<>() {{
        put("STOCK_01", 50000L);  // 에버반도체
        put("STOCK_02", 30000L);  // 케어금융
        put("STOCK_03", 15000L);  // 아톰에너지
        put("STOCK_04", 10000L);  // 피크건설
        put("STOCK_05", 5000L);   // 제네틱바이오
        put("STOCK_06", 25000L);  // 비트온엔터
        put("STOCK_07", 20000L);  // 웨이브조선
    }};
    
    // ==================== 경쟁모드 주식 등락률 ====================
    // Case 1: 2019.01~2020.01, UP 패턴
    private static final Map<String, BigDecimal[]> COMPETITION_STOCK_CHANGE_RATES_CASE1_UP = new HashMap<>() {{
        // 에버반도체 UP
        put("STOCK_01", new BigDecimal[] {
            new BigDecimal("-0.036"), // R1: 2019.01
            new BigDecimal("0.042"),  // R2: 2019.02
            new BigDecimal("0.000"),  // R3: 2019.03
            new BigDecimal("0.159"),  // R4: 2019.04
            new BigDecimal("0.038"),  // R5: 2019.05
            new BigDecimal("0.056"),  // R6: 2019.06
            new BigDecimal("-0.034"), // R7: 2019.07
            new BigDecimal("0.059"),  // R8: 2019.08
            new BigDecimal("0.007"),  // R9: 2019.09
            new BigDecimal("0.044"),  // R10: 2019.10
            new BigDecimal("0.099"),  // R11: 2019.11
            new BigDecimal("-0.013")  // R12: 2019.12
        });
        
        // 케어금융 UP
        put("STOCK_02", new BigDecimal[] {
            new BigDecimal("-0.087"), // R1
            new BigDecimal("-0.079"), // R2
            new BigDecimal("-0.169"), // R3
            new BigDecimal("0.065"),  // R4
            new BigDecimal("0.100"),  // R5
            new BigDecimal("0.081"),  // R6
            new BigDecimal("0.183"),  // R7
            new BigDecimal("0.179"),  // R8
            new BigDecimal("-0.088"), // R9
            new BigDecimal("-0.013"), // R10
            new BigDecimal("0.133"),  // R11
            new BigDecimal("-0.004")  // R12
        });
        
        // 아톰에너지 UP
        put("STOCK_03", new BigDecimal[] {
            new BigDecimal("0.061"),  // R1
            new BigDecimal("0.061"),  // R2
            new BigDecimal("-0.063"), // R3
            new BigDecimal("-0.014"), // R4
            new BigDecimal("-0.050"), // R5
            new BigDecimal("0.085"),  // R6
            new BigDecimal("-0.030"), // R7
            new BigDecimal("-0.021"), // R8
            new BigDecimal("-0.070"), // R9
            new BigDecimal("0.045"),  // R10
            new BigDecimal("0.015"),  // R11
            new BigDecimal("0.055")   // R12
        });
        
        // 피크건설 UP
        put("STOCK_04", new BigDecimal[] {
            new BigDecimal("0.002"),  // R1
            new BigDecimal("0.000"),  // R2
            new BigDecimal("0.048"),  // R3
            new BigDecimal("0.041"),  // R4
            new BigDecimal("0.044"),  // R5
            new BigDecimal("0.058"),  // R6
            new BigDecimal("0.074"),  // R7
            new BigDecimal("0.044"),  // R8
            new BigDecimal("0.053"),  // R9
            new BigDecimal("0.071"),  // R10
            new BigDecimal("-0.033"), // R11
            new BigDecimal("-0.082")  // R12
        });
        
        // 제네틱바이오 UP
        put("STOCK_05", new BigDecimal[] {
            new BigDecimal("0.181"),  // R1
            new BigDecimal("0.140"),  // R2
            new BigDecimal("0.016"),  // R3
            new BigDecimal("0.073"),  // R4
            new BigDecimal("0.245"),  // R5
            new BigDecimal("-0.097"), // R6
            new BigDecimal("0.171"),  // R7
            new BigDecimal("0.157"),  // R8
            new BigDecimal("-0.058"), // R9
            new BigDecimal("0.042"),  // R10
            new BigDecimal("0.325"),  // R11
            new BigDecimal("0.089")   // R12
        });
        
        // 비트온엔터 UP
        put("STOCK_06", new BigDecimal[] {
            new BigDecimal("0.017"),  // R1
            new BigDecimal("-0.012"), // R2
            new BigDecimal("-0.037"), // R3
            new BigDecimal("-0.023"), // R4
            new BigDecimal("-0.180"), // R5
            new BigDecimal("0.020"),  // R6
            new BigDecimal("-0.120"), // R7
            new BigDecimal("0.065"),  // R8
            new BigDecimal("0.025"),  // R9
            new BigDecimal("0.085"),  // R10
            new BigDecimal("0.122"),  // R11
            new BigDecimal("-0.021")  // R12
        });
        
        // 웨이브조선 UP
        put("STOCK_07", new BigDecimal[] {
            new BigDecimal("-0.034"), // R1
            new BigDecimal("0.049"),  // R2
            new BigDecimal("0.044"),  // R3
            new BigDecimal("-0.070"), // R4
            new BigDecimal("-0.010"), // R5
            new BigDecimal("0.140"),  // R6
            new BigDecimal("-0.010"), // R7
            new BigDecimal("0.240"),  // R8
            new BigDecimal("0.100"),  // R9
            new BigDecimal("-0.070"), // R10
            new BigDecimal("0.040"),  // R11
            new BigDecimal("-0.060")  // R12
        });
    }};
    
    // Case 1: 2019.01~2020.01, DOWN 패턴
    private static final Map<String, BigDecimal[]> COMPETITION_STOCK_CHANGE_RATES_CASE1_DOWN = new HashMap<>() {{
        // 에버반도체 DOWN
        put("STOCK_01", new BigDecimal[] {
            new BigDecimal("-0.043"), // R1
            new BigDecimal("0.010"),  // R2
            new BigDecimal("-0.035"), // R3
            new BigDecimal("-0.016"), // R4
            new BigDecimal("0.012"),  // R5
            new BigDecimal("0.034"),  // R6
            new BigDecimal("-0.041"), // R7
            new BigDecimal("-0.036"), // R8
            new BigDecimal("0.008"),  // R9
            new BigDecimal("-0.019"), // R10
            new BigDecimal("0.038"),  // R11
            new BigDecimal("-0.033")  // R12
        });
        
        // 케어금융 DOWN
        put("STOCK_02", new BigDecimal[] {
            new BigDecimal("-0.125"), // R1
            new BigDecimal("-0.053"), // R2
            new BigDecimal("-0.204"), // R3
            new BigDecimal("0.103"),  // R4
            new BigDecimal("0.082"),  // R5
            new BigDecimal("-0.035"), // R6
            new BigDecimal("-0.035"), // R7
            new BigDecimal("-0.005"), // R8
            new BigDecimal("0.014"),  // R9
            new BigDecimal("0.036"),  // R10
            new BigDecimal("0.106"),  // R11
            new BigDecimal("-0.010")  // R12
        });
        
        // 아톰에너지 DOWN
        put("STOCK_03", new BigDecimal[] {
            new BigDecimal("0.050"),  // R1
            new BigDecimal("-0.003"), // R2
            new BigDecimal("-0.045"), // R3
            new BigDecimal("0.017"),  // R4
            new BigDecimal("-0.096"), // R5
            new BigDecimal("-0.036"), // R6
            new BigDecimal("0.076"),  // R7
            new BigDecimal("-0.035"), // R8
            new BigDecimal("0.006"),  // R9
            new BigDecimal("-0.036"), // R10
            new BigDecimal("-0.084"), // R11
            new BigDecimal("0.024")   // R12
        });
        
        // 피크건설 DOWN
        put("STOCK_04", new BigDecimal[] {
            new BigDecimal("0.075"),  // R1
            new BigDecimal("-0.090"), // R2
            new BigDecimal("-0.005"), // R3
            new BigDecimal("-0.047"), // R4
            new BigDecimal("-0.011"), // R5
            new BigDecimal("0.005"),  // R6
            new BigDecimal("-0.160"), // R7
            new BigDecimal("-0.044"), // R8
            new BigDecimal("0.019"),  // R9
            new BigDecimal("-0.064"), // R10
            new BigDecimal("-0.029"), // R11
            new BigDecimal("0.035")   // R12
        });
        
        // 제네틱바이오 DOWN
        put("STOCK_05", new BigDecimal[] {
            new BigDecimal("0.087"),  // R1
            new BigDecimal("0.007"),  // R2
            new BigDecimal("0.007"),  // R3
            new BigDecimal("-0.070"), // R4
            new BigDecimal("-0.252"), // R5
            new BigDecimal("-0.118"), // R6
            new BigDecimal("-0.157"), // R7
            new BigDecimal("-0.027"), // R8
            new BigDecimal("-0.638"), // R9
            new BigDecimal("0.460"),  // R10
            new BigDecimal("-0.061"), // R11
            new BigDecimal("0.020")   // R12
        });
        
        // 비트온엔터 DOWN
        put("STOCK_06", new BigDecimal[] {
            new BigDecimal("-0.051"), // R1
            new BigDecimal("-0.007"), // R2
            new BigDecimal("0.081"),  // R3
            new BigDecimal("-0.033"), // R4
            new BigDecimal("-0.100"), // R5
            new BigDecimal("-0.040"), // R6
            new BigDecimal("-0.100"), // R7
            new BigDecimal("-0.059"), // R8
            new BigDecimal("0.125"),  // R9
            new BigDecimal("-0.009"), // R10
            new BigDecimal("0.039"),  // R11
            new BigDecimal("0.088")   // R12
        });
        
        // 웨이브조선 DOWN
        put("STOCK_07", new BigDecimal[] {
            new BigDecimal("0.166"),  // R1
            new BigDecimal("-0.271"), // R2
            new BigDecimal("-0.064"), // R3
            new BigDecimal("0.045"),  // R4
            new BigDecimal("-0.106"), // R5
            new BigDecimal("-0.008"), // R6
            new BigDecimal("-0.018"), // R7
            new BigDecimal("-0.018"), // R8
            new BigDecimal("0.137"),  // R9
            new BigDecimal("-0.097"), // R10
            new BigDecimal("-0.111"), // R11
            new BigDecimal("0.048")   // R12
        });
    }};
    
    // Case 2: 2019.02~2020.02, UP 패턴
    private static final Map<String, BigDecimal[]> COMPETITION_STOCK_CHANGE_RATES_CASE2_UP = new HashMap<>() {{
        put("STOCK_01", new BigDecimal[] {
            new BigDecimal("0.042"), new BigDecimal("0.000"), new BigDecimal("0.159"), new BigDecimal("0.038"),
            new BigDecimal("0.056"), new BigDecimal("-0.034"), new BigDecimal("0.059"), new BigDecimal("0.007"),
            new BigDecimal("0.044"), new BigDecimal("0.099"), new BigDecimal("-0.013"), new BigDecimal("0.036")
        });
        put("STOCK_02", new BigDecimal[] {
            new BigDecimal("-0.079"), new BigDecimal("-0.169"), new BigDecimal("0.065"), new BigDecimal("0.100"),
            new BigDecimal("0.081"), new BigDecimal("0.183"), new BigDecimal("0.179"), new BigDecimal("-0.088"),
            new BigDecimal("-0.013"), new BigDecimal("0.133"), new BigDecimal("-0.004"), new BigDecimal("0.006")
        });
        put("STOCK_03", new BigDecimal[] {
            new BigDecimal("0.061"), new BigDecimal("-0.063"), new BigDecimal("-0.014"), new BigDecimal("-0.050"),
            new BigDecimal("0.085"), new BigDecimal("-0.030"), new BigDecimal("-0.021"), new BigDecimal("-0.070"),
            new BigDecimal("0.045"), new BigDecimal("0.015"), new BigDecimal("0.055"), new BigDecimal("0.085")
        });
        put("STOCK_04", new BigDecimal[] {
            new BigDecimal("0.000"), new BigDecimal("0.048"), new BigDecimal("0.041"), new BigDecimal("0.044"),
            new BigDecimal("0.058"), new BigDecimal("0.074"), new BigDecimal("0.044"), new BigDecimal("0.053"),
            new BigDecimal("0.071"), new BigDecimal("-0.033"), new BigDecimal("-0.082"), new BigDecimal("-0.009")
        });
        put("STOCK_05", new BigDecimal[] {
            new BigDecimal("0.140"), new BigDecimal("0.016"), new BigDecimal("0.073"), new BigDecimal("0.245"),
            new BigDecimal("-0.097"), new BigDecimal("0.171"), new BigDecimal("0.157"), new BigDecimal("-0.058"),
            new BigDecimal("0.042"), new BigDecimal("0.325"), new BigDecimal("0.089"), new BigDecimal("-0.018")
        });
        put("STOCK_06", new BigDecimal[] {
            new BigDecimal("-0.012"), new BigDecimal("-0.037"), new BigDecimal("-0.023"), new BigDecimal("-0.180"),
            new BigDecimal("0.020"), new BigDecimal("-0.120"), new BigDecimal("0.065"), new BigDecimal("0.025"),
            new BigDecimal("0.085"), new BigDecimal("0.122"), new BigDecimal("-0.021"), new BigDecimal("0.105")
        });
        put("STOCK_07", new BigDecimal[] {
            new BigDecimal("0.049"), new BigDecimal("0.044"), new BigDecimal("-0.070"), new BigDecimal("-0.010"),
            new BigDecimal("0.140"), new BigDecimal("-0.010"), new BigDecimal("0.240"), new BigDecimal("0.100"),
            new BigDecimal("-0.070"), new BigDecimal("0.040"), new BigDecimal("-0.060"), new BigDecimal("-0.050")
        });
    }};
    
    // Case 2: 2019.02~2020.02, DOWN 패턴
    private static final Map<String, BigDecimal[]> COMPETITION_STOCK_CHANGE_RATES_CASE2_DOWN = new HashMap<>() {{
        put("STOCK_01", new BigDecimal[] {
            new BigDecimal("0.010"), new BigDecimal("-0.035"), new BigDecimal("-0.016"), new BigDecimal("0.012"),
            new BigDecimal("0.034"), new BigDecimal("-0.041"), new BigDecimal("-0.036"), new BigDecimal("0.008"),
            new BigDecimal("-0.019"), new BigDecimal("0.038"), new BigDecimal("-0.033"), new BigDecimal("-0.036")
        });
        put("STOCK_02", new BigDecimal[] {
            new BigDecimal("-0.053"), new BigDecimal("-0.204"), new BigDecimal("0.103"), new BigDecimal("0.082"),
            new BigDecimal("-0.035"), new BigDecimal("-0.035"), new BigDecimal("-0.005"), new BigDecimal("0.014"),
            new BigDecimal("0.036"), new BigDecimal("0.106"), new BigDecimal("-0.010"), new BigDecimal("-0.096")
        });
        put("STOCK_03", new BigDecimal[] {
            new BigDecimal("-0.003"), new BigDecimal("-0.045"), new BigDecimal("0.017"), new BigDecimal("-0.096"),
            new BigDecimal("-0.036"), new BigDecimal("0.076"), new BigDecimal("-0.035"), new BigDecimal("0.006"),
            new BigDecimal("-0.036"), new BigDecimal("-0.084"), new BigDecimal("0.024"), new BigDecimal("-0.133")
        });
        put("STOCK_04", new BigDecimal[] {
            new BigDecimal("-0.090"), new BigDecimal("-0.005"), new BigDecimal("-0.047"), new BigDecimal("-0.011"),
            new BigDecimal("0.005"), new BigDecimal("-0.160"), new BigDecimal("-0.044"), new BigDecimal("0.019"),
            new BigDecimal("-0.064"), new BigDecimal("-0.029"), new BigDecimal("0.035"), new BigDecimal("-0.098")
        });
        put("STOCK_05", new BigDecimal[] {
            new BigDecimal("0.007"), new BigDecimal("0.007"), new BigDecimal("-0.070"), new BigDecimal("-0.252"),
            new BigDecimal("-0.118"), new BigDecimal("-0.157"), new BigDecimal("-0.027"), new BigDecimal("-0.638"),
            new BigDecimal("0.460"), new BigDecimal("-0.061"), new BigDecimal("0.020"), new BigDecimal("-0.167")
        });
        put("STOCK_06", new BigDecimal[] {
            new BigDecimal("-0.007"), new BigDecimal("0.081"), new BigDecimal("-0.033"), new BigDecimal("-0.100"),
            new BigDecimal("-0.040"), new BigDecimal("-0.100"), new BigDecimal("-0.059"), new BigDecimal("0.125"),
            new BigDecimal("-0.009"), new BigDecimal("0.039"), new BigDecimal("0.088"), new BigDecimal("-0.020")
        });
        put("STOCK_07", new BigDecimal[] {
            new BigDecimal("-0.271"), new BigDecimal("-0.064"), new BigDecimal("0.045"), new BigDecimal("-0.106"),
            new BigDecimal("-0.008"), new BigDecimal("-0.018"), new BigDecimal("-0.018"), new BigDecimal("0.137"),
            new BigDecimal("-0.097"), new BigDecimal("-0.111"), new BigDecimal("0.048"), new BigDecimal("-0.044")
        });
    }};
    
    // Case 3: 2019.03~2020.03, UP 패턴
    private static final Map<String, BigDecimal[]> COMPETITION_STOCK_CHANGE_RATES_CASE3_UP = new HashMap<>() {{
        put("STOCK_01", new BigDecimal[] {
            new BigDecimal("0.000"), new BigDecimal("0.159"), new BigDecimal("0.038"), new BigDecimal("0.056"),
            new BigDecimal("-0.034"), new BigDecimal("0.059"), new BigDecimal("0.007"), new BigDecimal("0.044"),
            new BigDecimal("0.099"), new BigDecimal("-0.013"), new BigDecimal("0.036"), new BigDecimal("0.082")
        });
        put("STOCK_02", new BigDecimal[] {
            new BigDecimal("-0.169"), new BigDecimal("0.065"), new BigDecimal("0.100"), new BigDecimal("0.081"),
            new BigDecimal("0.183"), new BigDecimal("0.179"), new BigDecimal("-0.088"), new BigDecimal("-0.013"),
            new BigDecimal("0.133"), new BigDecimal("-0.004"), new BigDecimal("0.006"), new BigDecimal("-0.017")
        });
        put("STOCK_03", new BigDecimal[] {
            new BigDecimal("-0.063"), new BigDecimal("-0.014"), new BigDecimal("-0.050"), new BigDecimal("0.085"),
            new BigDecimal("-0.030"), new BigDecimal("-0.021"), new BigDecimal("-0.070"), new BigDecimal("0.045"),
            new BigDecimal("0.015"), new BigDecimal("0.055"), new BigDecimal("0.085"), new BigDecimal("0.115")
        });
        put("STOCK_04", new BigDecimal[] {
            new BigDecimal("0.048"), new BigDecimal("0.041"), new BigDecimal("0.044"), new BigDecimal("0.058"),
            new BigDecimal("0.074"), new BigDecimal("0.044"), new BigDecimal("0.053"), new BigDecimal("0.071"),
            new BigDecimal("-0.033"), new BigDecimal("-0.082"), new BigDecimal("-0.009"), new BigDecimal("-0.073")
        });
        put("STOCK_05", new BigDecimal[] {
            new BigDecimal("0.016"), new BigDecimal("0.073"), new BigDecimal("0.245"), new BigDecimal("-0.097"),
            new BigDecimal("0.171"), new BigDecimal("0.157"), new BigDecimal("-0.058"), new BigDecimal("0.042"),
            new BigDecimal("0.325"), new BigDecimal("0.089"), new BigDecimal("-0.018"), new BigDecimal("0.213")
        });
        put("STOCK_06", new BigDecimal[] {
            new BigDecimal("-0.037"), new BigDecimal("-0.023"), new BigDecimal("-0.180"), new BigDecimal("0.020"),
            new BigDecimal("-0.120"), new BigDecimal("0.065"), new BigDecimal("0.025"), new BigDecimal("0.085"),
            new BigDecimal("0.122"), new BigDecimal("-0.021"), new BigDecimal("0.105"), new BigDecimal("-0.090")
        });
        put("STOCK_07", new BigDecimal[] {
            new BigDecimal("0.044"), new BigDecimal("-0.070"), new BigDecimal("-0.010"), new BigDecimal("0.140"),
            new BigDecimal("-0.010"), new BigDecimal("0.240"), new BigDecimal("0.100"), new BigDecimal("-0.070"),
            new BigDecimal("0.040"), new BigDecimal("-0.060"), new BigDecimal("-0.050"), new BigDecimal("-0.120")
        });
    }};
    
    // Case 3: 2019.03~2020.03, DOWN 패턴
    private static final Map<String, BigDecimal[]> COMPETITION_STOCK_CHANGE_RATES_CASE3_DOWN = new HashMap<>() {{
        put("STOCK_01", new BigDecimal[] {
            new BigDecimal("-0.035"), new BigDecimal("-0.016"), new BigDecimal("0.012"), new BigDecimal("0.034"),
            new BigDecimal("-0.041"), new BigDecimal("-0.036"), new BigDecimal("0.008"), new BigDecimal("-0.019"),
            new BigDecimal("0.038"), new BigDecimal("-0.033"), new BigDecimal("-0.036"), new BigDecimal("-0.074")
        });
        put("STOCK_02", new BigDecimal[] {
            new BigDecimal("-0.204"), new BigDecimal("0.103"), new BigDecimal("0.082"), new BigDecimal("-0.035"),
            new BigDecimal("-0.035"), new BigDecimal("-0.005"), new BigDecimal("0.014"), new BigDecimal("0.036"),
            new BigDecimal("0.106"), new BigDecimal("-0.010"), new BigDecimal("-0.096"), new BigDecimal("0.089")
        });
        put("STOCK_03", new BigDecimal[] {
            new BigDecimal("-0.045"), new BigDecimal("0.017"), new BigDecimal("-0.096"), new BigDecimal("-0.036"),
            new BigDecimal("0.076"), new BigDecimal("-0.035"), new BigDecimal("0.006"), new BigDecimal("-0.036"),
            new BigDecimal("-0.084"), new BigDecimal("0.024"), new BigDecimal("-0.133"), new BigDecimal("-0.131")
        });
        put("STOCK_04", new BigDecimal[] {
            new BigDecimal("-0.005"), new BigDecimal("-0.047"), new BigDecimal("-0.011"), new BigDecimal("0.005"),
            new BigDecimal("-0.160"), new BigDecimal("-0.044"), new BigDecimal("0.019"), new BigDecimal("-0.064"),
            new BigDecimal("-0.029"), new BigDecimal("0.035"), new BigDecimal("-0.098"), new BigDecimal("-0.063")
        });
        put("STOCK_05", new BigDecimal[] {
            new BigDecimal("0.007"), new BigDecimal("-0.070"), new BigDecimal("-0.252"), new BigDecimal("-0.118"),
            new BigDecimal("-0.157"), new BigDecimal("-0.027"), new BigDecimal("-0.638"), new BigDecimal("0.460"),
            new BigDecimal("-0.061"), new BigDecimal("0.020"), new BigDecimal("-0.167"), new BigDecimal("-0.196")
        });
        put("STOCK_06", new BigDecimal[] {
            new BigDecimal("0.081"), new BigDecimal("-0.033"), new BigDecimal("-0.100"), new BigDecimal("-0.040"),
            new BigDecimal("-0.100"), new BigDecimal("-0.059"), new BigDecimal("0.125"), new BigDecimal("-0.009"),
            new BigDecimal("0.039"), new BigDecimal("0.088"), new BigDecimal("-0.020"), new BigDecimal("-0.120")
        });
        put("STOCK_07", new BigDecimal[] {
            new BigDecimal("-0.064"), new BigDecimal("0.045"), new BigDecimal("-0.106"), new BigDecimal("-0.008"),
            new BigDecimal("-0.018"), new BigDecimal("-0.018"), new BigDecimal("0.137"), new BigDecimal("-0.097"),
            new BigDecimal("-0.111"), new BigDecimal("0.048"), new BigDecimal("-0.044"), new BigDecimal("-0.084")
        });
    }};
    
    // Case 4: 2019.04~2020.04, UP 패턴
    private static final Map<String, BigDecimal[]> COMPETITION_STOCK_CHANGE_RATES_CASE4_UP = new HashMap<>() {{
        put("STOCK_01", new BigDecimal[] {
            new BigDecimal("0.159"), new BigDecimal("0.038"), new BigDecimal("0.056"), new BigDecimal("-0.034"),
            new BigDecimal("0.059"), new BigDecimal("0.007"), new BigDecimal("0.044"), new BigDecimal("0.099"),
            new BigDecimal("-0.013"), new BigDecimal("0.036"), new BigDecimal("0.082"), new BigDecimal("-0.096")
        });
        put("STOCK_02", new BigDecimal[] {
            new BigDecimal("0.065"), new BigDecimal("0.100"), new BigDecimal("0.081"), new BigDecimal("0.183"),
            new BigDecimal("0.179"), new BigDecimal("-0.088"), new BigDecimal("-0.013"), new BigDecimal("0.133"),
            new BigDecimal("-0.004"), new BigDecimal("0.006"), new BigDecimal("-0.017"), new BigDecimal("-0.169")
        });
        put("STOCK_03", new BigDecimal[] {
            new BigDecimal("-0.014"), new BigDecimal("-0.050"), new BigDecimal("0.085"), new BigDecimal("-0.030"),
            new BigDecimal("-0.021"), new BigDecimal("-0.070"), new BigDecimal("0.045"), new BigDecimal("0.015"),
            new BigDecimal("0.055"), new BigDecimal("0.085"), new BigDecimal("0.115"), new BigDecimal("-0.171")
        });
        put("STOCK_04", new BigDecimal[] {
            new BigDecimal("0.041"), new BigDecimal("0.044"), new BigDecimal("0.058"), new BigDecimal("0.074"),
            new BigDecimal("0.044"), new BigDecimal("0.053"), new BigDecimal("0.071"), new BigDecimal("-0.033"),
            new BigDecimal("-0.082"), new BigDecimal("-0.009"), new BigDecimal("-0.073"), new BigDecimal("-0.023")
        });
        put("STOCK_05", new BigDecimal[] {
            new BigDecimal("0.073"), new BigDecimal("0.245"), new BigDecimal("-0.097"), new BigDecimal("0.171"),
            new BigDecimal("0.157"), new BigDecimal("-0.058"), new BigDecimal("0.042"), new BigDecimal("0.325"),
            new BigDecimal("0.089"), new BigDecimal("-0.018"), new BigDecimal("0.213"), new BigDecimal("2.044")
        });
        put("STOCK_06", new BigDecimal[] {
            new BigDecimal("-0.023"), new BigDecimal("-0.180"), new BigDecimal("0.020"), new BigDecimal("-0.120"),
            new BigDecimal("0.065"), new BigDecimal("0.025"), new BigDecimal("0.085"), new BigDecimal("0.122"),
            new BigDecimal("-0.021"), new BigDecimal("0.105"), new BigDecimal("-0.090"), new BigDecimal("-0.247")
        });
        put("STOCK_07", new BigDecimal[] {
            new BigDecimal("-0.070"), new BigDecimal("-0.010"), new BigDecimal("0.140"), new BigDecimal("-0.010"),
            new BigDecimal("0.240"), new BigDecimal("0.100"), new BigDecimal("-0.070"), new BigDecimal("0.040"),
            new BigDecimal("-0.060"), new BigDecimal("-0.050"), new BigDecimal("-0.120"), new BigDecimal("-0.253")
        });
    }};
    
    // Case 4: 2019.04~2020.04, DOWN 패턴
    private static final Map<String, BigDecimal[]> COMPETITION_STOCK_CHANGE_RATES_CASE4_DOWN = new HashMap<>() {{
        put("STOCK_01", new BigDecimal[] {
            new BigDecimal("-0.016"), new BigDecimal("0.012"), new BigDecimal("0.034"), new BigDecimal("-0.041"),
            new BigDecimal("-0.036"), new BigDecimal("0.008"), new BigDecimal("-0.019"), new BigDecimal("0.038"),
            new BigDecimal("-0.033"), new BigDecimal("-0.036"), new BigDecimal("-0.074"), new BigDecimal("-0.167")
        });
        put("STOCK_02", new BigDecimal[] {
            new BigDecimal("0.103"), new BigDecimal("0.082"), new BigDecimal("-0.035"), new BigDecimal("-0.035"),
            new BigDecimal("-0.005"), new BigDecimal("0.014"), new BigDecimal("0.036"), new BigDecimal("0.106"),
            new BigDecimal("-0.010"), new BigDecimal("-0.096"), new BigDecimal("0.089"), new BigDecimal("-0.204")
        });
        put("STOCK_03", new BigDecimal[] {
            new BigDecimal("0.017"), new BigDecimal("-0.096"), new BigDecimal("-0.036"), new BigDecimal("0.076"),
            new BigDecimal("-0.035"), new BigDecimal("0.006"), new BigDecimal("-0.036"), new BigDecimal("-0.084"),
            new BigDecimal("0.024"), new BigDecimal("-0.133"), new BigDecimal("-0.131"), new BigDecimal("-0.230")
        });
        put("STOCK_04", new BigDecimal[] {
            new BigDecimal("-0.047"), new BigDecimal("-0.011"), new BigDecimal("0.005"), new BigDecimal("-0.160"),
            new BigDecimal("-0.044"), new BigDecimal("0.019"), new BigDecimal("-0.064"), new BigDecimal("-0.029"),
            new BigDecimal("0.035"), new BigDecimal("-0.098"), new BigDecimal("-0.063"), new BigDecimal("0.234")
        });
        put("STOCK_05", new BigDecimal[] {
            new BigDecimal("-0.070"), new BigDecimal("-0.252"), new BigDecimal("-0.118"), new BigDecimal("-0.157"),
            new BigDecimal("-0.027"), new BigDecimal("-0.638"), new BigDecimal("0.460"), new BigDecimal("-0.061"),
            new BigDecimal("0.020"), new BigDecimal("-0.167"), new BigDecimal("-0.196"), new BigDecimal("0.106")
        });
        put("STOCK_06", new BigDecimal[] {
            new BigDecimal("-0.033"), new BigDecimal("-0.100"), new BigDecimal("-0.040"), new BigDecimal("-0.100"),
            new BigDecimal("-0.059"), new BigDecimal("0.125"), new BigDecimal("-0.009"), new BigDecimal("0.039"),
            new BigDecimal("0.088"), new BigDecimal("-0.020"), new BigDecimal("-0.120"), new BigDecimal("0.200")
        });
        put("STOCK_07", new BigDecimal[] {
            new BigDecimal("0.045"), new BigDecimal("-0.106"), new BigDecimal("-0.008"), new BigDecimal("-0.018"),
            new BigDecimal("-0.018"), new BigDecimal("0.137"), new BigDecimal("-0.097"), new BigDecimal("-0.111"),
            new BigDecimal("0.048"), new BigDecimal("-0.044"), new BigDecimal("-0.084"), new BigDecimal("-0.318")
        });
    }};
    
    /**
     * 경쟁모드 주식 등락률 선택 헬퍼
     * 
     * @param stockId 주식 ID
     * @param pattern UP 또는 DOWN
     * @param startCase 시작 케이스 (1~4)
     * @return 등락률 배열
     */
    private BigDecimal[] getCompetitionStockChangeRates(String stockId, String pattern, Integer startCase) {
        if (startCase == null || startCase < 1 || startCase > 4) {
            log.warn("Invalid startCase, defaulting to 1: {}", startCase);
            startCase = 1;
        }
        
        boolean isUp = "UP".equals(pattern);
        
        switch (startCase) {
            case 1:
                return isUp ? COMPETITION_STOCK_CHANGE_RATES_CASE1_UP.get(stockId) 
                            : COMPETITION_STOCK_CHANGE_RATES_CASE1_DOWN.get(stockId);
            case 2:
                return isUp ? COMPETITION_STOCK_CHANGE_RATES_CASE2_UP.get(stockId) 
                            : COMPETITION_STOCK_CHANGE_RATES_CASE2_DOWN.get(stockId);
            case 3:
                return isUp ? COMPETITION_STOCK_CHANGE_RATES_CASE3_UP.get(stockId) 
                            : COMPETITION_STOCK_CHANGE_RATES_CASE3_DOWN.get(stockId);
            case 4:
                return isUp ? COMPETITION_STOCK_CHANGE_RATES_CASE4_UP.get(stockId) 
                            : COMPETITION_STOCK_CHANGE_RATES_CASE4_DOWN.get(stockId);
            default:
                log.warn("Unexpected startCase: {}, using case 1", startCase);
                return isUp ? COMPETITION_STOCK_CHANGE_RATES_CASE1_UP.get(stockId) 
                            : COMPETITION_STOCK_CHANGE_RATES_CASE1_DOWN.get(stockId);
        }
    }
    
    /**
     * 라운드 시작 시 주식 시세 업데이트
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     */
    public void updateStockPrices(GameSessionDto session, PortfolioDto portfolio) {
        GameMode gameMode = session.getGameMode();
        int currentRound = session.getCurrentRound();
        
        log.info("Updating stock prices: mode={}, round={}", gameMode, currentRound);
        
        if (portfolio.getStocks() == null || portfolio.getStocks().isEmpty()) {
            log.debug("No stocks to update");
            return;
        }
        
        for (StockHoldingDto stock : portfolio.getStocks()) {
            String stockId = stock.getStockId();
            
            // 이전 시가 (현재 currentPrice)
            Long previousPrice = stock.getCurrentPrice();
            
            // 등락률 가져오기 (모드별)
            BigDecimal[] changeRates;
            int maxRound;
            
            if (gameMode == GameMode.TUTORIAL) {
                changeRates = TUTORIAL_STOCK_CHANGE_RATES.get(stockId);
                maxRound = 6;
            } else if (gameMode == GameMode.COMPETITION) {
                // 세션의 패턴 정보 사용
                String pattern = session.getStockPatterns() != null 
                    ? session.getStockPatterns().get(stockId) 
                    : "UP";
                Integer startCase = session.getStockStartCase() != null 
                    ? session.getStockStartCase() 
                    : 1;
                
                changeRates = getCompetitionStockChangeRates(stockId, pattern, startCase);
                maxRound = 12;
                
                log.debug("Using competition pattern: stockId={}, pattern={}, case={}", 
                    stockId, pattern, startCase);
            } else {
                log.warn("Stock price update not implemented for mode: {}", gameMode);
                continue;
            }
            
            if (changeRates == null || currentRound < 1 || currentRound > maxRound) {
                log.warn("Change rate not found: stockId={}, round={}, mode={}", stockId, currentRound, gameMode);
                continue;
            }
            
            BigDecimal changeRate = changeRates[currentRound - 1];
            
            // 새로운 종가 계산 (StockService에서 이미 반올림 적용됨)
            Long newPrice = stockService.calculateClosePrice(
                BigDecimal.valueOf(previousPrice), 
                changeRate
            ).longValue();
            
            // 평가금액 및 손익 업데이트
            stock.setCurrentPrice(newPrice);
            stock.setEvaluationAmount(newPrice * stock.getQuantity());
            
            Long totalCost = stock.getAvgPrice() * stock.getQuantity();
            stock.setProfitLoss(stock.getEvaluationAmount() - totalCost);
            stock.setReturnRate((double) stock.getProfitLoss() / totalCost);
            
            log.debug("Stock updated: stockId={}, {} -> {}, changeRate={}%", 
                stockId, previousPrice, newPrice, changeRate.multiply(new BigDecimal("100")));
        }
    }
    
    // 펀드 구성 정보 (펀드ID -> 구성 종목 및 비율)
    private static final Map<String, Map<String, Double>> FUND_COMPOSITIONS = new HashMap<>() {{
        // FUND_01: 성장형 (UP 종목 중심)
        put("FUND_01", new HashMap<>() {{
            put("STOCK_01", 0.30); // 에버반도체 30%
            put("STOCK_02", 0.30); // 케어금융 30%
            put("STOCK_07", 0.40); // 웨이브조선 40%
        }});
        
        // FUND_02: 안정형 (혼합)
        put("FUND_02", new HashMap<>() {{
            put("STOCK_03", 0.40); // 아톰에너지 40%
            put("STOCK_04", 0.30); // 피크건설 30%
            put("STOCK_06", 0.30); // 비트온엔터 30%
        }});
        
        // FUND_03: 고위험 고수익형 (변동성 큰 종목)
        put("FUND_03", new HashMap<>() {{
            put("STOCK_02", 0.40); // 케어금융 40%
            put("STOCK_05", 0.30); // 제네틱바이오 30%
            put("STOCK_07", 0.30); // 웨이브조선 30%
        }});
    }};
    
    /**
     * 라운드 시작 시 펀드 기준가 업데이트
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     */
    public void updateFundNavs(GameSessionDto session, PortfolioDto portfolio) {
        GameMode gameMode = session.getGameMode();
        int currentRound = session.getCurrentRound();
        
        log.info("Updating fund NAVs: mode={}, round={}", gameMode, currentRound);
        
        if (portfolio.getFunds() == null || portfolio.getFunds().isEmpty()) {
            log.debug("No funds to update");
            return;
        }
        
        // 주식 등락률 조회
        Map<String, BigDecimal> stockChangeRates = getStockChangeRates(session);
        
        for (FundHoldingDto fund : portfolio.getFunds()) {
            String fundId = fund.getFundId();
            
            // 펀드 구성 정보 조회
            Map<String, Double> composition = FUND_COMPOSITIONS.get(fundId);
            if (composition == null) {
                log.warn("Fund composition not found: fundId={}", fundId);
                continue;
            }
            
            // 펀드 NAV 변동률 계산 (구성 종목의 가중평균)
            BigDecimal navChangeRate = BigDecimal.ZERO;
            for (Map.Entry<String, Double> entry : composition.entrySet()) {
                String stockId = entry.getKey();
                Double weight = entry.getValue();
                
                BigDecimal stockChangeRate = stockChangeRates.get(stockId);
                if (stockChangeRate != null) {
                    navChangeRate = navChangeRate.add(
                        stockChangeRate.multiply(BigDecimal.valueOf(weight))
                    );
                }
            }
            
            // 이전 NAV
            Long previousNav = fund.getCurrentNav();
            
            // 새로운 NAV 계산 (반올림)
            BigDecimal newNav = BigDecimal.valueOf(previousNav)
                .multiply(BigDecimal.ONE.add(navChangeRate))
                .setScale(0, java.math.RoundingMode.HALF_UP);
            Long newNavLong = newNav.longValue();
            
            // 평가금액 및 손익 업데이트
            fund.setCurrentNav(newNavLong);
            fund.setEvaluationAmount(newNavLong * fund.getShares());
            
            Long totalCost = fund.getAvgNav() * fund.getShares();
            fund.setProfitLoss(fund.getEvaluationAmount() - totalCost);
            fund.setReturnRate((double) fund.getProfitLoss() / totalCost);
            
            log.debug("Fund updated: fundId={}, NAV {} -> {}, changeRate={}%", 
                fundId, previousNav, newNavLong, navChangeRate.multiply(new BigDecimal("100")));
        }
    }
    
    /**
     * 펀드 시작 NAV 조회 (게임 시작 시)
     * 
     * @param fundId 펀드 ID
     * @param gameMode 게임 모드
     * @return 시작 NAV
     */
    public Long getFundStartNav(String fundId, GameMode gameMode) {
        // 펀드 초기 NAV는 10,000원으로 고정
        return 10000L;
    }
    
    /**
     * 현재 라운드의 펀드 NAV 조회 (매수/매도 시 사용)
     * 
     * @param fundId 펀드 ID
     * @param gameMode 게임 모드
     * @param currentRound 현재 라운드
     * @return 현재 NAV
     */
    public Long getCurrentFundNav(String fundId, GameMode gameMode, int currentRound) {
        log.debug("Getting current fund NAV: fundId={}, mode={}, round={}", 
            fundId, gameMode, currentRound);
        
        Long startNav = getFundStartNav(fundId, gameMode);
        
        // 펀드 구성 정보 조회
        Map<String, Double> composition = FUND_COMPOSITIONS.get(fundId);
        if (composition == null) {
            log.warn("Fund composition not found: fundId={}", fundId);
            return startNav;
        }
        
        // 1라운드부터 현재 라운드까지의 NAV 변동 누적 계산
        BigDecimal currentNav = BigDecimal.valueOf(startNav);
        
        for (int round = 1; round <= currentRound && round <= 6; round++) {
            Map<String, BigDecimal> stockChangeRates = getStockChangeRates(gameMode, round);
            
            // 펀드 NAV 변동률 계산
            BigDecimal navChangeRate = BigDecimal.ZERO;
            for (Map.Entry<String, Double> entry : composition.entrySet()) {
                String stockId = entry.getKey();
                Double weight = entry.getValue();
                
                BigDecimal stockChangeRate = stockChangeRates.get(stockId);
                if (stockChangeRate != null) {
                    navChangeRate = navChangeRate.add(
                        stockChangeRate.multiply(BigDecimal.valueOf(weight))
                    );
                }
            }
            
            currentNav = currentNav.multiply(BigDecimal.ONE.add(navChangeRate));
        }
        
        // 반올림 처리
        return currentNav.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
    }
    
    /**
     * 현재 라운드의 기준금리 조회 (세션 기반)
     * 
     * @param session 게임 세션
     * @return 기준금리
     */
    public BigDecimal getBaseRate(GameSessionDto session) {
        return getBaseRate(session.getGameMode(), session.getCurrentRound(), session.getBaseRateCase());
    }
    
    /**
     * 현재 라운드의 기준금리 조회
     * 
     * @param gameMode 게임 모드
     * @param currentRound 현재 라운드
     * @return 기준금리
     */
    public BigDecimal getBaseRate(GameMode gameMode, int currentRound) {
        return getBaseRate(gameMode, currentRound, null);
    }
    
    /**
     * 현재 라운드의 기준금리 조회 (케이스 정보 포함)
     * 
     * @param gameMode 게임 모드
     * @param currentRound 현재 라운드
     * @param baseRateCase 기준금리 케이스 (경쟁모드용, 1~4)
     * @return 기준금리
     */
    private BigDecimal getBaseRate(GameMode gameMode, int currentRound, Integer baseRateCase) {
        log.debug("Getting base rate: mode={}, round={}, case={}", gameMode, currentRound, baseRateCase);
        
        if (gameMode == GameMode.TUTORIAL) {
            if (currentRound < 1 || currentRound > 6) {
                log.warn("Invalid round for tutorial: {}", currentRound);
                return TUTORIAL_BASE_RATES[0];
            }
            return TUTORIAL_BASE_RATES[currentRound - 1];
        }
        
        // 경쟁모드
        if (gameMode == GameMode.COMPETITION) {
            if (baseRateCase == null || baseRateCase < 1 || baseRateCase > 4) {
                log.warn("Invalid or null baseRateCase, defaulting to 1: {}", baseRateCase);
                baseRateCase = 1;
            }
            
            // Case에 따라 시작 인덱스 결정
            // Case 1: index 0~12 (2019.01~2020.01)
            // Case 2: index 1~13 (2019.02~2020.02)
            // Case 3: index 2~14 (2019.03~2020.03)
            // Case 4: index 3~15 (2019.04~2020.04)
            int startIndex = baseRateCase - 1;
            int index = startIndex + (currentRound - 1);
            
            if (index < 0 || index >= COMPETITION_BASE_RATES_ALL.length) {
                log.warn("Invalid index for competition base rate: index={}, currentRound={}, baseRateCase={}", 
                    index, currentRound, baseRateCase);
                return COMPETITION_BASE_RATES_ALL[startIndex]; // 해당 케이스의 첫 번째 값
            }
            
            return COMPETITION_BASE_RATES_ALL[index];
        }
        
        log.warn("Base rate not implemented for mode: {}", gameMode);
        return new BigDecimal("0.015"); // 기본값
    }
    
    /**
     * 주식 시작가 조회 (게임 시작 시)
     * 
     * @param stockId 주식 ID
     * @param gameMode 게임 모드
     * @return 시작가
     */
    public Long getStockStartPrice(String stockId, GameMode gameMode) {
        log.debug("Getting stock start price: stockId={}, mode={}", stockId, gameMode);
        
        if (gameMode == GameMode.TUTORIAL) {
            Long price = TUTORIAL_STOCK_START_PRICES.get(stockId);
            if (price == null) {
                log.warn("Start price not found: stockId={}", stockId);
                return 10000L; // 기본값
            }
            return price;
        }
        
        // 경쟁모드
        if (gameMode == GameMode.COMPETITION) {
            Long price = COMPETITION_STOCK_START_PRICES.get(stockId);
            if (price == null) {
                log.warn("Start price not found for competition: stockId={}", stockId);
                return 10000L; // 기본값
            }
            return price;
        }
        
        log.warn("Stock start price not implemented for mode: {}", gameMode);
        return 10000L;
    }
    
    /**
     * 현재 라운드의 주식 시세 조회 (매수/매도 시 사용)
     * 
     * @param stockId 주식 ID
     * @param gameMode 게임 모드
     * @param currentRound 현재 라운드
     * @return 현재가
     */
    public Long getCurrentStockPrice(String stockId, GameMode gameMode, int currentRound) {
        log.debug("Getting current stock price: stockId={}, mode={}, round={}", 
            stockId, gameMode, currentRound);
        
        if (gameMode == GameMode.TUTORIAL) {
            Long startPrice = getStockStartPrice(stockId, gameMode);
            
            // 1라운드부터 현재 라운드까지의 등락률 누적 적용
            BigDecimal currentPrice = BigDecimal.valueOf(startPrice);
            BigDecimal[] changeRates = TUTORIAL_STOCK_CHANGE_RATES.get(stockId);
            
            if (changeRates == null) {
                log.warn("Change rates not found: stockId={}", stockId);
                return startPrice;
            }
            
            for (int round = 1; round <= currentRound && round <= 6; round++) {
                BigDecimal changeRate = changeRates[round - 1];
                currentPrice = stockService.calculateClosePrice(currentPrice, changeRate);
            }
            
            // StockService.calculateClosePrice()에서 이미 반올림 적용됨
            return currentPrice.longValue();
        }
        
        // 경쟁모드는 나중에 구현
        log.warn("Current stock price not implemented for mode: {}", gameMode);
        return 10000L;
    }
    
    /**
     * 라운드 시작 시 주식 등락률 조회 (세션 기반)
     * 
     * @param session 게임 세션
     * @return 주식ID -> 등락률 맵
     */
    public Map<String, BigDecimal> getStockChangeRates(GameSessionDto session) {
        return getStockChangeRates(session.getGameMode(), session.getCurrentRound(), 
            session.getStockPatterns(), session.getStockStartCase());
    }
    
    /**
     * 라운드 시작 시 주식 등락률 조회
     * 
     * @param gameMode 게임 모드
     * @param currentRound 현재 라운드
     * @return 주식ID -> 등락률 맵
     */
    public Map<String, BigDecimal> getStockChangeRates(GameMode gameMode, int currentRound) {
        return getStockChangeRates(gameMode, currentRound, null, null);
    }
    
    /**
     * 라운드 시작 시 주식 등락률 조회 (패턴 정보 포함)
     * 
     * @param gameMode 게임 모드
     * @param currentRound 현재 라운드
     * @param stockPatterns 주식 패턴 맵 (경쟁모드용)
     * @param stockStartCase 시작 케이스 (경쟁모드용)
     * @return 주식ID -> 등락률 맵
     */
    private Map<String, BigDecimal> getStockChangeRates(GameMode gameMode, int currentRound,
                                                         Map<String, String> stockPatterns, 
                                                         Integer stockStartCase) {
        log.debug("Getting stock change rates: mode={}, round={}", gameMode, currentRound);
        
        Map<String, BigDecimal> result = new HashMap<>();
        
        if (gameMode == GameMode.TUTORIAL) {
            if (currentRound < 1 || currentRound > 6) {
                log.warn("Invalid round for tutorial: {}", currentRound);
                return result;
            }
            
            for (Map.Entry<String, BigDecimal[]> entry : TUTORIAL_STOCK_CHANGE_RATES.entrySet()) {
                String stockId = entry.getKey();
                BigDecimal changeRate = entry.getValue()[currentRound - 1];
                result.put(stockId, changeRate);
            }
        } else if (gameMode == GameMode.COMPETITION) {
            if (currentRound < 1 || currentRound > 12) {
                log.warn("Invalid round for competition: {}", currentRound);
                return result;
            }
            
            // 7개 종목에 대해 패턴별 등락률 조회
            for (int i = 1; i <= 7; i++) {
                String stockId = "STOCK_0" + i;
                String pattern = (stockPatterns != null && stockPatterns.containsKey(stockId)) 
                    ? stockPatterns.get(stockId) 
                    : "UP";
                Integer startCase = (stockStartCase != null) ? stockStartCase : 1;
                
                BigDecimal[] changeRates = getCompetitionStockChangeRates(stockId, pattern, startCase);
                if (changeRates != null && currentRound - 1 < changeRates.length) {
                    result.put(stockId, changeRates[currentRound - 1]);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 이전 라운드와 비교하여 기준금리 변화량 조회
     * 
     * @param gameMode 게임 모드
     * @param currentRound 현재 라운드
     * @return 변화량
     */
    public BigDecimal getBaseRateChange(GameMode gameMode, int currentRound) {
        if (currentRound <= 1) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal current = getBaseRate(gameMode, currentRound);
        BigDecimal previous = getBaseRate(gameMode, currentRound - 1);
        
        return current.subtract(previous);
    }
    
    /**
     * 라운드 시작 시 펀드 NAV 변동률 조회
     * 
     * @param gameMode 게임 모드
     * @param currentRound 현재 라운드
     * @return 펀드ID -> NAV 변동률 맵
     */
    public Map<String, BigDecimal> getFundNavChangeRates(GameMode gameMode, int currentRound) {
        log.debug("Getting fund NAV change rates: mode={}, round={}", gameMode, currentRound);
        
        Map<String, BigDecimal> result = new HashMap<>();
        
        // 주식 등락률 조회
        Map<String, BigDecimal> stockChangeRates = getStockChangeRates(gameMode, currentRound);
        
        // 각 펀드의 NAV 변동률 계산
        for (Map.Entry<String, Map<String, Double>> entry : FUND_COMPOSITIONS.entrySet()) {
            String fundId = entry.getKey();
            Map<String, Double> composition = entry.getValue();
            
            // 가중평균 계산
            BigDecimal navChangeRate = BigDecimal.ZERO;
            for (Map.Entry<String, Double> comp : composition.entrySet()) {
                String stockId = comp.getKey();
                Double weight = comp.getValue();
                
                BigDecimal stockChangeRate = stockChangeRates.get(stockId);
                if (stockChangeRate != null) {
                    navChangeRate = navChangeRate.add(
                        stockChangeRate.multiply(BigDecimal.valueOf(weight))
                    );
                }
            }
            
            result.put(fundId, navChangeRate);
        }
        
        return result;
    }
}

