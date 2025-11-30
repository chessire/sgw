package com.cas.api.controller.v1;

import com.cas.api.constant.GameConstants;
import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.dto.domain.LoanDto;
import com.cas.api.dto.domain.PortfolioDto;
import com.cas.api.dto.request.BuyAdditionalInfoRequest;
import com.cas.api.dto.request.ResolveLifeEventRequest;
import com.cas.api.dto.request.UseAdviceRequest;
import com.cas.api.dto.response.PortfolioResponseDto;
import com.cas.api.dto.response.RoundStartDto;
import com.cas.api.dto.response.RoundStateDto;
import com.cas.api.dto.response.SettlementDto;
import com.cas.api.enums.GameMode;
import com.cas.api.service.financial.PortfolioService;
import com.cas.api.service.game.*;
import com.cas.common.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 경쟁 모드 Controller
 * TutorialController와 거의 동일하지만 GameMode가 COMPETITION
 */
@Slf4j
@RestController
@RequestMapping("/v1/competition")
@RequiredArgsConstructor
public class CompetitionController {
    
    private final GameSessionService gameSessionService;
    private final RoundService roundService;
    private final PortfolioService portfolioService;
    private final MarketEventService marketEventService;
    private final AdviceService adviceService;
    private final ClueService clueService;
    private final ActionService actionService;
    private final LifeEventService lifeEventService;
    private final RankingService rankingService;
    private final java.util.Random random = new java.util.Random();
    
    /**
     * 게임 시작 (경쟁모드는 고정 초기값)
     * POST /api/v1/competition/start
     */
    @PostMapping("/start")
    public ApiResponse<RoundStateDto> startGame(@RequestHeader("uid") String uid) {
        
        log.info("Starting competition game: uid={}", uid);
        
        try {
            // 기존 세션 확인
            GameSessionDto existingSession = gameSessionService.getSession(uid, GameMode.COMPETITION);
            if (existingSession != null && !existingSession.getCompleted()) {
                log.warn("Competition game already in progress: uid={}", uid);
                return ApiResponse.error("GAME_IN_PROGRESS", "이미 진행 중인 게임이 있습니다.");
            }
            
            // 경쟁모드 고정 초기값
            Long initialCash = 5000000L;      // 500만원
            Long monthlyIncome = 1000000L;    // 100만원
            Long monthlyExpense = 500000L;    // 50만원
            
            // 초기 포트폴리오 생성
            PortfolioDto portfolio = PortfolioDto.builder()
                .cash(initialCash)
                .deposits(new ArrayList<>())
                .savings(new ArrayList<>())
                .bonds(new ArrayList<>())
                .stocks(new ArrayList<>())
                .funds(new ArrayList<>())
                .pensions(new ArrayList<>())
                .build();
            
            // 경쟁모드 랜덤 설정
            Random random = new Random();
            
            // 1. 주식 패턴 랜덤 결정 (UP/DOWN, 7개 종목)
            Map<String, String> stockPatterns = new HashMap<>();
            for (int i = 1; i <= 7; i++) {
                String stockId = "STOCK_0" + i;
                String pattern = random.nextBoolean() ? "UP" : "DOWN";
                stockPatterns.put(stockId, pattern);
                log.debug("Stock pattern set: {}={}", stockId, pattern);
            }
            
            // 2. 시작 케이스 랜덤 선택 (1~4)
            // Case 1: 2019.01~2020.01, Case 2: 2019.02~2020.02
            // Case 3: 2019.03~2020.03, Case 4: 2019.04~2020.04
            int startCase = random.nextInt(4) + 1;
            log.info("Competition start case selected: {}", startCase);
            
            // 게임 세션 생성
            GameSessionDto session = GameSessionDto.builder()
                .uid(uid)
                .gameMode(GameMode.COMPETITION)
                .currentRound(1)
                .completed(false)
                .monthlySalary(monthlyIncome)
                .monthlyLiving(monthlyExpense)
                .initialCash(initialCash) // 점수 계산용
                .monthlyInsurancePremium(GameConstants.DEFAULT_MONTHLY_INSURANCE_PREMIUM)
                .adviceUsedCount(0)
                .insuranceSubscribed(false)
                .loanUsed(false)
                .illegalLoanUsed(false)
                .insurableEventOccurred(false)
                .portfolio(portfolio)
                .stockPatterns(stockPatterns)
                .stockStartCase(startCase)
                .baseRateCase(startCase)  // 기준금리도 동일한 케이스 사용
                .build();
            
            gameSessionService.createSession(uid, GameMode.COMPETITION, session);
            
            // 라운드 시작 처리
            roundService.startRound(session, portfolio);
            
            // 포트폴리오 평가 (총자산, 순자산 등 계산)
            portfolioService.updatePortfolioSummary(portfolio);
            
            // 응답 생성
            RoundStateDto response = buildRoundState(session, portfolio);
            
            log.info("Competition game started: uid={}, round={}", uid, session.getCurrentRound());
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("Failed to start competition game: uid={}", uid, e);
            return ApiResponse.error("START_FAILED", "게임 시작 실패: " + e.getMessage());
        }
    }
    
    /**
     * 라운드 진행
     * POST /api/v1/competition/proceed-round
     */
    @PostMapping("/proceed-round")
    public ApiResponse<RoundStateDto> proceedRound(
            @RequestHeader("uid") String uid,
            @RequestBody Map<String, Object> actions) {
        
        log.info("Proceeding competition round: uid={}", uid);
        
        try {
            // 세션 조회
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.COMPETITION);
            if (session == null) {
                log.warn("Competition session not found: uid={}", uid);
                return ApiResponse.error("SESSION_NOT_FOUND", "게임 세션을 찾을 수 없습니다.");
            }
            
            PortfolioDto portfolio = session.getPortfolio();
            
            // 플레이어 액션 처리 (매수/매도, 예적금 가입 등)
            actionService.processActions(session, portfolio, actions);
            
            // 라운드 종료 처리
            roundService.endRound(session, portfolio);
            
            // 게임이 완료되지 않았으면 다음 라운드 시작
            if (!session.getCompleted()) {
                roundService.startRound(session, portfolio);
                portfolioService.updatePortfolioSummary(portfolio);
            }
            
            // 세션 업데이트
            gameSessionService.updateSession(uid, GameMode.COMPETITION, session);
            
            // 응답 생성
            RoundStateDto response = buildRoundState(session, portfolio);
            
            log.info("Competition round proceeded: uid={}, round={}, completed={}", 
                uid, session.getCurrentRound(), session.getCompleted());
            
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("Failed to proceed competition round: uid={}", uid, e);
            return ApiResponse.error("PROCEED_FAILED", "라운드 진행 실패: " + e.getMessage());
        }
    }
    
    /**
     * NPC 조언 사용
     * POST /api/v1/competition/use-advice
     */
    @PostMapping("/use-advice")
    public ApiResponse<Map<String, Object>> useAdvice(
            @RequestHeader("uid") String uid,
            @RequestBody UseAdviceRequest request) {
        
        log.info("Using advice: uid={}, roundNo={}", uid, request.getRoundNo());
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.COMPETITION);
            if (session == null) {
                return ApiResponse.error("SESSION_NOT_FOUND", "게임 세션을 찾을 수 없습니다.");
            }
            
            if (!adviceService.canUseAdvice(session)) {
                return ApiResponse.error("ADVICE_LIMIT_EXCEEDED", "조언 횟수를 모두 사용했습니다.");
            }
            
            boolean success = adviceService.useAdvice(session);
            
            if (!success) {
                return ApiResponse.error("ADVICE_USE_FAILED", "조언 사용에 실패했습니다.");
            }
            
            gameSessionService.updateSession(uid, GameMode.COMPETITION, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("remainingAdviceCount", adviceService.getRemainingAdviceCount(session));
            data.put("adviceUsed", true);
            
            log.info("Advice used successfully: uid={}, remaining={}", 
                uid, data.get("remainingAdviceCount"));
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to use advice: uid={}", uid, e);
            return ApiResponse.error("ADVICE_FAILED", "조언 사용 실패: " + e.getMessage());
        }
    }
    
    /**
     * 심화정보 구매
     * POST /api/v1/competition/buy-additional-info
     */
    @PostMapping("/buy-additional-info")
    public ApiResponse<Map<String, Object>> buyAdditionalInfo(
            @RequestHeader("uid") String uid,
            @RequestBody BuyAdditionalInfoRequest request) {
        
        log.info("Buying additional info: uid={}, infoKey={}", uid, request.getInfoKey());
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.COMPETITION);
            if (session == null) {
                return ApiResponse.error("SESSION_NOT_FOUND", "게임 세션을 찾을 수 없습니다.");
            }
            
            PortfolioDto portfolio = session.getPortfolio();
            
            boolean success = clueService.buyAdditionalInfo(session, portfolio, request.getInfoKey());
            
            if (!success) {
                return ApiResponse.error("PURCHASE_FAILED", "현금이 부족하거나 구매에 실패했습니다.");
            }
            
            // 세션 업데이트
            gameSessionService.updateSession(uid, GameMode.COMPETITION, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("purchased", true);
            data.put("infoKey", request.getInfoKey());
            data.put("remainingCash", portfolio.getCash());
            
            log.info("Additional info purchased: uid={}, infoKey={}, remainingCash={}", 
                uid, request.getInfoKey(), portfolio.getCash());
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to buy additional info: uid={}", uid, e);
            return ApiResponse.error("PURCHASE_FAILED", "심화정보 구매 실패: " + e.getMessage());
        }
    }
    
    /**
     * 인생이벤트 해결
     * POST /api/v1/competition/resolve-life-event
     */
    @PostMapping("/resolve-life-event")
    public ApiResponse<Map<String, Object>> resolveLifeEvent(
            @RequestHeader("uid") String uid,
            @RequestBody ResolveLifeEventRequest request) {
        
        log.info("Resolving life event: uid={}, eventKey={}, resolutionType={}", 
            uid, request.getEventKey(), request.getResolutionType());
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.COMPETITION);
            if (session == null) {
                return ApiResponse.error("SESSION_NOT_FOUND", "게임 세션을 찾을 수 없습니다.");
            }
            
            PortfolioDto portfolio = session.getPortfolio();
            String resolutionType = request.getResolutionType();
            
            Map<String, Object> result = new HashMap<>();
            
            // 해결 방법에 따라 처리
            switch (resolutionType) {
                case "CASH":
                    // 현금으로 지급 (프론트에서 금액 확인 완료)
                    // 실제 처리는 프론트가 처리할 금액을 백엔드에 전달
                    result.put("resolved", true);
                    result.put("method", "CASH");
                    break;
                    
                case "FORCE_SELL":
                    // 투자상품 해지 (프론트에서 선택한 상품)
                    // sellActions 처리
                    if (request.getSellActions() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> sellActions = (Map<String, Object>) request.getSellActions();
                        actionService.processActions(session, portfolio, sellActions);
                    }
                    result.put("resolved", true);
                    result.put("method", "FORCE_SELL");
                    break;
                    
                case "INSURANCE":
                    // 보험 적용
                    if (!Boolean.TRUE.equals(session.getInsuranceSubscribed())) {
                        return ApiResponse.error("INSURANCE_NOT_AVAILABLE", "보험에 가입되어 있지 않습니다.");
                    }
                    gameSessionService.markInsurableEventOccurred(session);
                    result.put("resolved", true);
                    result.put("method", "INSURANCE");
                    break;
                    
                case "LOAN":
                    // 대출 이용
                    Long loanAmount = request.getLoanAmount();
                    if (loanAmount == null || loanAmount <= 0) {
                        return ApiResponse.error("INVALID_LOAN_AMOUNT", "대출 금액이 올바르지 않습니다.");
                    }
                    
                    boolean loanSuccess = lifeEventService.takeLoan(session, portfolio, loanAmount);
                    if (!loanSuccess) {
                        return ApiResponse.error("LOAN_FAILED", "대출이 불가능합니다. (이미 사용했거나 조건 미달)");
                    }
                    
                    result.put("resolved", true);
                    result.put("method", "LOAN");
                    result.put("loanAmount", loanAmount);
                    break;
                    
                default:
                    return ApiResponse.error("INVALID_RESOLUTION_TYPE", "올바르지 않은 해결 방법입니다.");
            }
            
            // 포트폴리오 업데이트
            portfolioService.updatePortfolioSummary(portfolio);
            
            // 세션 업데이트
            gameSessionService.updateSession(uid, GameMode.COMPETITION, session);
            
            result.put("remainingCash", portfolio.getCash());
            result.put("portfolioSummary", buildPortfolioSummaryResponse(portfolio));
            
            log.info("Life event resolved: uid={}, method={}, remainingCash={}", 
                uid, resolutionType, portfolio.getCash());
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("Failed to resolve life event: uid={}", uid, e);
            return ApiResponse.error("RESOLVE_FAILED", "인생이벤트 해결 실패: " + e.getMessage());
        }
    }
    
    /**
     * 게임 결과 조회 (랭킹 포함)
     * GET /api/v1/competition/result
     */
    @GetMapping("/result")
    public ApiResponse<Map<String, Object>> getResult(@RequestHeader("uid") String uid) {
        log.info("Getting competition result: uid={}", uid);
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.COMPETITION);
            if (session == null) {
                return ApiResponse.error("SESSION_NOT_FOUND", "게임 세션을 찾을 수 없습니다.");
            }
            
            if (!session.getCompleted()) {
                return ApiResponse.error("GAME_NOT_COMPLETED", "게임이 아직 완료되지 않았습니다.");
            }
            
            PortfolioDto portfolio = session.getPortfolio();
            
            // 초기 자본 (세션에 저장된 값 사용)
            long initialCash = session.getInitialCash() != null ? session.getInitialCash() : 5000000L;
            
            // 점수 계산
            RankingService.ScoreResult scoreResult = rankingService.calculateScore(session, portfolio, initialCash);
            
            // 결과 구성
            Map<String, Object> result = new HashMap<>();
            
            // 재무 정보
            result.put("finalNetWorth", portfolio.getNetWorth());
            result.put("totalAssets", portfolio.getTotalAssets());
            result.put("totalLiabilities", portfolio.getTotalLiabilities());
            result.put("initialCash", initialCash);
            
            // 수익률 계산
            long profit = portfolio.getNetWorth() - initialCash;
            double returnRate = ((double)profit / initialCash) * 100.0;
            result.put("profit", profit);
            result.put("returnRate", String.format("%.2f", returnRate) + "%");
            
            // 점수
            Map<String, Object> score = new HashMap<>();
            score.put("totalScore", String.format("%.1f", scoreResult.getTotalScore()));
            score.put("financialManagement", String.format("%.1f", scoreResult.getFinancialManagementScore()));
            score.put("riskManagement", String.format("%.1f", scoreResult.getRiskManagementScore()));
            score.put("returnRate", String.format("%.1f", scoreResult.getReturnRateScore()));
            result.put("score", score);
            
            // 게임 정보
            result.put("adviceUsedCount", session.getAdviceUsedCount());
            result.put("insuranceSubscribed", session.getInsuranceSubscribed());
            result.put("loanUsed", session.getLoanUsed());
            result.put("illegalLoanUsed", session.getIllegalLoanUsed());
            
            // TODO: 랭킹 (Redis Sorted Set)
            // result.put("ranking", rankingService.getRanking(uid, scoreResult.getTotalScore()));
            
            log.info("Competition result retrieved: uid={}, netWorth={}, score={}", 
                uid, portfolio.getNetWorth(), scoreResult.getTotalScore());
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("Failed to get competition result: uid={}", uid, e);
            return ApiResponse.error("RESULT_FAILED", "결과 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * RoundState 응답 생성
     */
    private RoundStateDto buildRoundState(GameSessionDto session, PortfolioDto portfolio) {
        // CurrentRound
        RoundStateDto.CurrentRoundDto currentRound = RoundStateDto.CurrentRoundDto.builder()
            .roundNo(session.getCurrentRound())
            .build();
        
        // Portfolio Summary
        PortfolioResponseDto.SummaryDto summary = PortfolioResponseDto.SummaryDto.builder()
            .totalAssets(portfolio.getTotalAssets())
            .totalLiabilities(portfolio.getTotalLiabilities())
            .netWorth(portfolio.getNetWorth())
            .allocation(portfolio.getAllocation())
            .build();
        
        // Cash-like Assets
        long depositBalance = portfolio.getDeposits().stream()
            .mapToLong(d -> d.getBalance() != null ? d.getBalance() : 0L)
            .sum();
        long savingBalance = portfolio.getSavings().stream()
            .mapToLong(s -> s.getBalance() != null ? s.getBalance() : 0L)
            .sum();
        long bondBalance = portfolio.getBonds().stream()
            .mapToLong(b -> b.getEvaluationAmount() != null ? b.getEvaluationAmount() : 0L)
            .sum();
        
        PortfolioResponseDto.CashLikeAssetsDto cashLikeAssets = PortfolioResponseDto.CashLikeAssetsDto.builder()
            .cash(portfolio.getCash())
            .depositBalance(depositBalance)
            .savingBalance(savingBalance)
            .bondBalance(bondBalance)
            .total(portfolio.getCash() + depositBalance + savingBalance + bondBalance)
            .build();
        
        // Investment Assets
        long stockEvaluation = portfolio.getStocks().stream()
            .mapToLong(s -> s.getEvaluationAmount() != null ? s.getEvaluationAmount() : 0L)
            .sum();
        long fundEvaluation = portfolio.getFunds().stream()
            .mapToLong(f -> f.getEvaluationAmount() != null ? f.getEvaluationAmount() : 0L)
            .sum();
        long pensionEvaluation = portfolio.getPensions().stream()
            .mapToLong(p -> p.getEvaluationAmount() != null ? p.getEvaluationAmount() : 0L)
            .sum();
        
        PortfolioResponseDto.InvestmentAssetsDto investmentAssets = PortfolioResponseDto.InvestmentAssetsDto.builder()
            .stockEvaluation(stockEvaluation)
            .fundEvaluation(fundEvaluation)
            .pensionEvaluation(pensionEvaluation)
            .total(stockEvaluation + fundEvaluation + pensionEvaluation)
            .build();
        
        // Holdings (대출 정보는 GameSessionDto.loanInfo에 있으므로 변환)
        List<LoanDto> loansList = new ArrayList<>();
        if (session.getLoanInfo() != null) {
            loansList.add(session.getLoanInfo());
        }
        
        PortfolioResponseDto.HoldingsDto holdings = PortfolioResponseDto.HoldingsDto.builder()
            .cash(portfolio.getCash())
            .deposits(portfolio.getDeposits())
            .savings(portfolio.getSavings())
            .bonds(portfolio.getBonds())
            .stocks(portfolio.getStocks())
            .funds(portfolio.getFunds())
            .pensions(portfolio.getPensions())
            .loans(loansList)
            .build();
        
        PortfolioResponseDto portfolioResponse = PortfolioResponseDto.builder()
            .summary(summary)
            .cashLikeAssets(cashLikeAssets)
            .investmentAssets(investmentAssets)
            .holdings(holdings)
            .build();
        
        // Settlement (정산 내역)
        SettlementDto settlement = buildSettlementInfo(session, portfolio);
        
        // RoundStart - 시장 변동 및 게임 정보
        RoundStartDto roundStart = buildRoundStartInfo(session);
        
        return RoundStateDto.builder()
            .currentRound(currentRound)
            .settlement(settlement)
            .portfolio(portfolioResponse)
            .roundStart(roundStart)
            .build();
    }
    
    /**
     * Settlement 정보 구성
     */
    private SettlementDto buildSettlementInfo(GameSessionDto session, PortfolioDto portfolio) {
        // 기본 수입
        Long salary = session.getMonthlySalary() != null ? session.getMonthlySalary() : 0L;
        SettlementDto.BaseIncomeDto baseIncome = SettlementDto.BaseIncomeDto.builder()
            .salary(salary)
            .total(salary)
            .build();
        
        // 기본 지출
        Long living = session.getMonthlyLiving() != null ? session.getMonthlyLiving() : 0L;
        Long insurance = (session.getInsuranceSubscribed() != null && session.getInsuranceSubscribed()) 
            ? (session.getMonthlyInsurancePremium() != null ? session.getMonthlyInsurancePremium() : 0L)
            : 0L;
        Long loanInterest = 0L;
        if (session.getLoanInfo() != null && session.getLoanInfo().getRemainingBalance() != null 
            && session.getLoanInfo().getRemainingBalance() > 0) {
            // TODO: 대출 이자는 SettlementService에서 계산됨
            loanInterest = 0L;
        }
        
        SettlementDto.BaseExpensesDto baseExpenses = SettlementDto.BaseExpensesDto.builder()
            .living(living)
            .otherExpenses(insurance + loanInterest)
            .total(living + insurance + loanInterest)
            .build();
        
        // 이자/배당금 (수동 수입) - TODO: SettlementService에서 계산된 값 사용
        long depositInterest = 0L; // 라운드 시작 시 이자 지급
        long savingInterest = 0L; // 적금은 만기 시에만 이자 지급
        long bondInterest = 0L; // 라운드 시작 시 이자 지급
        long stockDividend = 0L; // 배당은 3, 6라운드에만
        
        SettlementDto.PassiveIncomeDto passiveIncome = SettlementDto.PassiveIncomeDto.builder()
            .depositInterest(depositInterest)
            .savingInterest(savingInterest)
            .bondInterest(bondInterest)
            .stockDividend(stockDividend)
            .total(depositInterest + savingInterest + bondInterest + stockDividend)
            .build();
        
        return SettlementDto.builder()
            .baseIncome(baseIncome)
            .baseExpenses(baseExpenses)
            .passiveIncome(passiveIncome)
            .build();
    }
    
    /**
     * RoundStart 정보 구성
     */
    private RoundStartDto buildRoundStartInfo(GameSessionDto session) {
        int currentRound = session.getCurrentRound();
        GameMode gameMode = session.getGameMode();
        
        // 시장 변동 정보
        BigDecimal baseRate = marketEventService.getBaseRate(gameMode, currentRound);
        BigDecimal baseRateChange = marketEventService.getBaseRateChange(gameMode, currentRound);
        
        // 주식 가격 변동률
        Map<String, BigDecimal> stockChangeRates = marketEventService.getStockChangeRates(gameMode, currentRound);
        List<RoundStartDto.StockPriceChangeDto> stockPriceChanges = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : stockChangeRates.entrySet()) {
            stockPriceChanges.add(RoundStartDto.StockPriceChangeDto.builder()
                .stockId(entry.getKey())
                .changeRate(entry.getValue())
                .build());
        }
        
        // 펀드 NAV 변동률
        Map<String, BigDecimal> fundNavChangeRates = marketEventService.getFundNavChangeRates(gameMode, currentRound);
        List<RoundStartDto.FundNavChangeDto> fundNavChanges = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : fundNavChangeRates.entrySet()) {
            fundNavChanges.add(RoundStartDto.FundNavChangeDto.builder()
                .fundId(entry.getKey())
                .changeRate(entry.getValue())
                .build());
        }
        
        // 시장 변동
        RoundStartDto.MarketMovementDto marketMovement = RoundStartDto.MarketMovementDto.builder()
            .baseRate(baseRate)
            .baseRateChange(baseRateChange)
            .stockPriceChange(stockPriceChanges)
            .fundNavChange(fundNavChanges)
            .build();
        
        // 뉴스 (주식 패턴 반영)
        List<RoundStartDto.NewsItemDto> stockNews = new ArrayList<>();
        if (session.getStockPatterns() != null) {
            // 3개 주식 뉴스 (STOCK_01, STOCK_02, STOCK_03)
            for (int i = 1; i <= 3; i++) {
                String stockId = "STOCK_0" + i;
                String pattern = session.getStockPatterns().getOrDefault(stockId, "UP");
                // 패턴 기반 뉴스 키: NEWS_STOCK_01_UP_R1 또는 NEWS_STOCK_01_DOWN_R1
                String newsKey = "NEWS_" + stockId + "_" + pattern + "_R" + currentRound;
                stockNews.add(RoundStartDto.NewsItemDto.builder().newsKey(newsKey).build());
            }
        } else {
            // fallback (패턴 없을 경우)
            stockNews = List.of(
                RoundStartDto.NewsItemDto.builder().newsKey("NEWS_STOCK_01_R" + currentRound).build(),
                RoundStartDto.NewsItemDto.builder().newsKey("NEWS_STOCK_02_R" + currentRound).build(),
                RoundStartDto.NewsItemDto.builder().newsKey("NEWS_STOCK_03_R" + currentRound).build()
            );
        }
        
        RoundStartDto.NewsDto news = RoundStartDto.NewsDto.builder()
            .stockNews(stockNews)
            .economicNews(RoundStartDto.NewsItemDto.builder().newsKey("NEWS_ECONOMY_R" + currentRound).build())
            .todayFortune(RoundStartDto.NewsItemDto.builder().newsKey("NEWS_FORTUNE_R" + currentRound).build())
            .build();
        
        // 기업 검색 데이터 (주식 패턴 정보)
        List<RoundStartDto.BrowserDataDto> browserData = new ArrayList<>();
        if (session.getStockPatterns() != null) {
            for (Map.Entry<String, String> entry : session.getStockPatterns().entrySet()) {
                browserData.add(RoundStartDto.BrowserDataDto.builder()
                    .companyKey(entry.getKey())
                    .pattern(entry.getValue())
                    .build());
            }
        }
        
        // 인생이벤트 발생 판정
        RoundStartDto.LifeEventDto lifeEvent = null;
        if (lifeEventService.shouldEventOccur(session, currentRound)) {
            // 이벤트 키 생성 (모드별 다름)
            String prefix = (gameMode == GameMode.TUTORIAL) ? "EVENT_TUTORIAL" : "EVENT_COMPETITION";
            int eventNumber = random.nextInt(3) + 1; // 1~3번 중 랜덤
            String eventKey = prefix + "_R" + currentRound + "_" + String.format("%02d", eventNumber);
            
            // 이벤트 타입과 금액 (임시 값 - 나중에 CSV 규칙으로 교체)
            boolean isExpense = random.nextBoolean();
            String eventType = isExpense ? "EXPENSE" : "INCOME";
            long amount = isExpense 
                ? (random.nextInt(5) + 1) * 100000L  // 지출: 10만~50만
                : (random.nextInt(3) + 1) * 50000L;  // 수입: 5만~15만
            
            // 보험 처리 가능 여부 (EXPENSE 이벤트 중 일부만)
            boolean insurableEvent = isExpense && random.nextBoolean();
            
            lifeEvent = RoundStartDto.LifeEventDto.builder()
                .eventKey(eventKey)
                .eventType(eventType)
                .amount(isExpense ? -amount : amount)
                .insurableEvent(insurableEvent)
                .build();
            
            log.info("Life event occurred: uid={}, round={}, eventKey={}, type={}, amount={}, insurable={}", 
                session.getUid(), currentRound, eventKey, eventType, amount, insurableEvent);
        }
        
        return RoundStartDto.builder()
            .news(news)
            .economicPopup(null) // TODO: 특정 라운드에만 존재
            .phoneMessages(new ArrayList<>()) // TODO: 단서 시스템 구현 시 추가
            .browserData(browserData)
            .marketMovement(marketMovement)
            .lifeEvent(lifeEvent)
            .build();
    }
    
    /**
     * Portfolio Summary 응답 생성 (간단 버전)
     */
    private Map<String, Object> buildPortfolioSummaryResponse(PortfolioDto portfolio) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAssets", portfolio.getTotalAssets());
        summary.put("totalLiabilities", portfolio.getTotalLiabilities());
        summary.put("netWorth", portfolio.getNetWorth());
        summary.put("allocation", portfolio.getAllocation());
        return summary;
    }
}

