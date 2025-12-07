package com.cas.api.controller.v1;

import com.cas.api.constant.GameConstants;
import com.cas.api.dto.domain.FundHoldingDto;
import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.dto.domain.LoanDto;
import com.cas.api.dto.domain.PortfolioDto;
import com.cas.api.dto.domain.StockHoldingDto;
import com.cas.api.dto.request.BuyAdditionalInfoRequest;
import com.cas.api.dto.request.NpcRequest;
import com.cas.api.dto.request.ResolveLifeEventRequest;
import com.cas.api.dto.request.UseAdviceRequest;
import com.cas.api.dto.response.PortfolioResponseDto;
import com.cas.api.dto.response.RoundStartDto;
import com.cas.api.dto.request.ProductCalculationRequest;
import com.cas.api.dto.response.GameStatusDto;
import com.cas.api.dto.response.ProductCalculationDto;
import com.cas.api.dto.response.RoundStateDto;
import com.cas.api.dto.response.SettlementDto;
import com.cas.api.dto.response.StartSettlementResultDto;
import com.cas.api.enums.GameMode;
import com.cas.api.service.financial.DepositService;
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
    private final AchievementService achievementService;
    private final DepositService depositService;
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
            if (portfolio == null) {
                log.error("Portfolio is null for session: uid={}", uid);
                return ApiResponse.error("PORTFOLIO_NOT_FOUND", "포트폴리오 정보를 찾을 수 없습니다. 게임을 다시 시작해주세요.");
            }
            
            // 플레이어 액션 처리 (매수/매도, 예적금 가입 등)
            actionService.processActions(session, portfolio, actions);
            
            // 라운드 종료 처리
            roundService.endRound(session, portfolio);
            
            // 게임이 완료되지 않았으면 다음 라운드 시작
            StartSettlementResultDto settlementResult = null;
            if (!session.getCompleted()) {
                settlementResult = roundService.startRound(session, portfolio);
                portfolioService.updatePortfolioSummary(portfolio);
            }
            
            // 세션 업데이트
            gameSessionService.updateSession(uid, GameMode.COMPETITION, session);
            
            // 응답 생성
            RoundStateDto response = buildRoundState(session, portfolio);
            
            // 자동납입 실패 정보 추가
            if (settlementResult != null && settlementResult.hasFailures()) {
                response.getRoundStart().setAutoPayments(settlementResult.getAutoPayments());
                response.getRoundStart().setAutoPaymentFailures(settlementResult.getAutoPaymentFailures());
            }
            
            log.info("Competition round proceeded: uid={}, round={}, completed={}, autoPaymentFailures={}", 
                uid, session.getCurrentRound(), session.getCompleted(),
                settlementResult != null ? settlementResult.getAutoPaymentFailures().size() : 0);
            
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
            
            // 힌트 생성 (조언 사용 후)
            AdviceService.HintResult hint = adviceService.generateHint(
                session, request.getRoundNo(), GameMode.COMPETITION);
            
            gameSessionService.updateSession(uid, GameMode.COMPETITION, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("remainingAdviceCount", adviceService.getRemainingAdviceCount(session));
            data.put("adviceUsed", true);
            data.put("hint", hint.toMap());
            
            log.info("Advice used successfully: uid={}, remaining={}, hintKey={}", 
                uid, data.get("remainingAdviceCount"), hint.getKey());
            
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
                    // 현금으로 지급
                    Long cashAmount = request.getCashAmount();
                    if (cashAmount == null || cashAmount <= 0) {
                        return ApiResponse.error("INVALID_CASH_AMOUNT", "현금 금액이 올바르지 않습니다.");
                    }
                    
                    // 현금 지급 가능 여부 확인
                    LifeEventService.CashPaymentResult cashCheck = 
                        lifeEventService.checkCashPayment(portfolio, cashAmount);
                    
                    if (!cashCheck.isSufficient()) {
                        result.put("resolved", false);
                        result.put("method", "CASH");
                        result.put("currentCash", cashCheck.getCurrentCash());
                        result.put("additionalPaymentRequired", true);
                        result.put("hasRedeemableAssets", lifeEventService.hasRedeemableAssets(portfolio));
                        result.put("message", "현금이 부족합니다. 추가 지급 필요");
                        return ApiResponse.success(result);
                    }
                    
                    // 현금 지급 처리
                    lifeEventService.processCashPayment(portfolio, cashAmount);
                    result.put("resolved", true);
                    result.put("method", "CASH");
                    result.put("cashPaid", cashAmount);
                    break;
                    
                case "SELL_ASSETS":
                case "FORCE_SELL":
                    // 투자상품 해지만
                    if (request.getSellActions() == null) {
                        return ApiResponse.error("NO_SELL_ACTIONS", "매도할 상품 정보가 없습니다.");
                    }
                    
                    long beforeCash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sellActions = (Map<String, Object>) request.getSellActions();
                    actionService.processActions(session, portfolio, sellActions);
                    
                    long afterCash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
                    long assetsValue = afterCash - beforeCash;
                    
                    result.put("resolved", true);
                    result.put("method", "SELL_ASSETS");
                    result.put("assetsValue", assetsValue);
                    result.put("totalSecured", assetsValue); // 매도로 확보한 금액
                    result.put("reason", request.getReason());
                    result.put("hasRedeemableAssets", lifeEventService.hasRedeemableAssets(portfolio));
                    break;
                    
                case "MIXED":
                    // 현금 + 투자상품 복합
                    Long mixedCashAmount = request.getCashAmount();
                    if (request.getSellActions() == null) {
                        return ApiResponse.error("NO_SELL_ACTIONS", "매도할 상품 정보가 없습니다.");
                    }
                    
                    long beforeMixedCash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mixedSellActions = (Map<String, Object>) request.getSellActions();
                    actionService.processActions(session, portfolio, mixedSellActions);
                    
                    long afterMixedCash = portfolio.getCash() != null ? portfolio.getCash() : 0L;
                    long mixedAssetsValue = afterMixedCash - beforeMixedCash;
                    
                    // 필요 금액
                    long requiredAmount = request.getShortfallAmount() != null 
                        ? request.getShortfallAmount() 
                        : (mixedCashAmount != null ? mixedCashAmount : 0L) + mixedAssetsValue;
                    
                    // 복합 해결 처리
                    LifeEventService.MixedResolutionResult mixedResult = 
                        lifeEventService.processMixedResolution(
                            portfolio, 
                            mixedCashAmount, 
                            mixedAssetsValue, 
                            requiredAmount,
                            request.getReason()
                        );
                    
                    result.put("resolved", mixedResult.isResolved());
                    result.put("method", "MIXED");
                    result.put("cashPaid", mixedCashAmount);
                    result.put("assetsValue", mixedAssetsValue);
                    result.put("totalSecured", mixedAssetsValue); // 매도로 확보한 금액만 (현금 제외)
                    result.put("reason", mixedResult.getReason());
                    
                    if (!mixedResult.isResolved()) {
                        result.put("additionalPaymentRequired", true);
                        result.put("hasRedeemableAssets", lifeEventService.hasRedeemableAssets(portfolio));
                        result.put("message", "추가 지급 필요");
                    }
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
            
            // 업적 체크 (게임 완료 시 모든 업적 체크)
            achievementService.checkAchievements(session);
            achievementService.checkFinancialComprehensive(session);
            gameSessionService.updateSession(uid, GameMode.COMPETITION, session);
            
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
        
        // GameStatus - 게임 상태 정보
        GameStatusDto gameStatus = buildGameStatus(session);
        
        return RoundStateDto.builder()
            .currentRound(currentRound)
            .settlement(settlement)
            .portfolio(portfolioResponse)
            .roundStart(roundStart)
            .gameStatus(gameStatus)
            .build();
    }
    
    /**
     * GameStatus 정보 구성
     */
    private GameStatusDto buildGameStatus(GameSessionDto session) {
        // 영상 시청 상태 (경쟁모드에서는 사용 안 함, null로 설정)
        GameStatusDto.VideoStatusDto videoStatus = null;
        
        // 퀴즈 정답 상태 (경쟁모드에서는 사용 안 함, null로 설정)
        GameStatusDto.QuizStatusDto quizStatus = null;
        
        // 조언 남은 횟수 계산
        int adviceUsed = session.getAdviceUsedCount() != null ? session.getAdviceUsedCount() : 0;
        int adviceRemaining = 3 - adviceUsed;
        
        return GameStatusDto.builder()
            // NPC 정보
            .npcType(session.getNpcType())
            .npcAssignmentCompleted(session.getNpcAssignmentCompleted())
            .npcSelectionCompleted(session.getNpcSelectionCompleted())
            // 진행 상태
            .completed(session.getCompleted())
            .openingStoryCompleted(session.getOpeningStoryCompleted())
            .propensityTestCompleted(session.getPropensityTestCompleted())
            .propensityType(session.getPropensityType())
            .resultAnalysisCompleted(session.getResultAnalysisCompleted())
            // 보험/대출 상태
            .insuranceSubscribed(session.getInsuranceSubscribed())
            .monthlyInsurancePremium(session.getMonthlyInsurancePremium())
            .loanUsed(session.getLoanUsed())
            .illegalLoanUsed(session.getIllegalLoanUsed())
            .insurableEventOccurred(session.getInsurableEventOccurred())
            // 조언 사용
            .adviceUsedCount(adviceUsed)
            .adviceRemaining(adviceRemaining)
            // 영상/퀴즈 상태 (경쟁모드에서는 null)
            .videoStatus(videoStatus)
            .quizStatus(quizStatus)
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
        
        // 구매 가능한 심화정보 키 목록
        List<String> additionalInfoKeys = clueService.getAvailableAdditionalInfoKeys(currentRound, false);
        
        return RoundStartDto.builder()
            .news(news)
            .economicPopup(null) // TODO: 특정 라운드에만 존재
            .phoneMessages(new ArrayList<>()) // TODO: 단서 시스템 구현 시 추가
            .browserData(browserData)
            .marketMovement(marketMovement)
            .lifeEvent(lifeEvent)
            .availableAdditionalInfo(additionalInfoKeys)
            .build();
    }
    
    /**
     * 게임 로드 상태 확인
     * GET /api/v1/competition/check-load
     * 
     * 저장된 게임 세션이 있으면 진행 상황 정보를 반환합니다.
     */
    @GetMapping("/check-load")
    public ApiResponse<Map<String, Object>> checkLoad(@RequestHeader("uid") String uid) {
        
        log.info("Checking load status: uid={}", uid);
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.COMPETITION);
            
            Map<String, Object> data = new HashMap<>();
            
            if (session == null) {
                data.put("hasSession", false);
                data.put("message", "저장된 게임이 없습니다.");
                return ApiResponse.success(data);
            }
            
            data.put("hasSession", true);
            data.put("currentRound", session.getCurrentRound());
            data.put("completed", session.getCompleted());
            data.put("updatedAt", session.getUpdatedAt() != null ? session.getUpdatedAt().toString() : null);
            
            // 진행 상황 플래그 (경쟁모드는 NPC 선택만)
            Map<String, Boolean> progress = new HashMap<>();
            progress.put("npcSelectionCompleted", Boolean.TRUE.equals(session.getNpcSelectionCompleted()));
            data.put("progress", progress);
            
            // 포트폴리오 요약
            if (session.getPortfolio() != null) {
                PortfolioDto portfolio = session.getPortfolio();
                Map<String, Object> portfolioSummary = new HashMap<>();
                portfolioSummary.put("cash", portfolio.getCash());
                portfolioSummary.put("totalAssets", portfolio.getTotalAssets());
                portfolioSummary.put("netWorth", portfolio.getNetWorth());
                data.put("portfolioSummary", portfolioSummary);
            }
            
            // 기타 게임 정보
            data.put("npcType", session.getNpcType());
            data.put("adviceUsedCount", session.getAdviceUsedCount());
            data.put("insuranceSubscribed", session.getInsuranceSubscribed());
            data.put("loanUsed", session.getLoanUsed());
            data.put("illegalLoanUsed", session.getIllegalLoanUsed());
            
            log.info("Load status checked: uid={}, currentRound={}, completed={}, illegalLoanUsed={}", 
                uid, session.getCurrentRound(), session.getCompleted(), session.getIllegalLoanUsed());
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to check load status: uid={}", uid, e);
            return ApiResponse.error("CHECK_LOAD_FAILED", "로드 상태 확인 실패: " + e.getMessage());
        }
    }
    
    /**
     * 불법사금융 사용
     * POST /api/v1/competition/use-illegal-loan
     * 
     * 경쟁모드 6라운드에서 불법사금융 광고 클릭 시 호출
     * (리스크 관리 점수에서 -20점 패널티)
     */
    @PostMapping("/use-illegal-loan")
    public ApiResponse<Map<String, Object>> useIllegalLoan(@RequestHeader("uid") String uid) {
        
        log.info("Using illegal loan (competition - penalty applied): uid={}", uid);
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.COMPETITION);
            
            if (session == null) {
                return ApiResponse.error("SESSION_NOT_FOUND", "게임 세션을 찾을 수 없습니다.");
            }
            
            // 이미 사용한 경우
            if (Boolean.TRUE.equals(session.getIllegalLoanUsed())) {
                return ApiResponse.error("ALREADY_USED", "이미 불법사금융을 이용했습니다.");
            }
            
            // 불법사금융 사용 플래그 설정
            session.setIllegalLoanUsed(true);
            gameSessionService.updateSession(uid, GameMode.COMPETITION, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("used", true);
            data.put("message", "불법사금융을 이용했습니다.");
            data.put("warning", "⚠️ 불법사금융은 금융범죄입니다. 실제로 이용하면 안 됩니다!");
            data.put("penalty", -20); // 경쟁모드는 -20점 패널티
            data.put("penaltyDescription", "리스크 관리 점수에서 -20점이 차감됩니다.");
            
            log.info("Illegal loan used (competition): uid={}, penalty=-20", uid);
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to use illegal loan: uid={}", uid, e);
            return ApiResponse.error("FAILED", "불법사금융 사용 처리 실패: " + e.getMessage());
        }
    }
    
    /**
     * 게임 초기화 (강제 종료)
     * DELETE /api/v1/competition/reset
     */
    @DeleteMapping("/reset")
    public ApiResponse<Map<String, Object>> resetGame(@RequestHeader("uid") String uid) {
        
        log.info("Resetting competition game: uid={}", uid);
        
        try {
            // 세션 삭제
            gameSessionService.deleteSession(uid, GameMode.COMPETITION);
            
            Map<String, Object> data = new HashMap<>();
            data.put("reset", true);
            data.put("message", "게임이 초기화되었습니다. 새로 시작할 수 있습니다.");
            
            log.info("Competition game reset successfully: uid={}", uid);
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to reset competition game: uid={}", uid, e);
            return ApiResponse.error("FAILED", "게임 초기화 실패: " + e.getMessage());
        }
    }
    
    /**
     * NPC 선택
     * POST /api/v1/competition/select-npc
     */
    @PostMapping("/select-npc")
    public ApiResponse<Map<String, Object>> selectNpc(
            @RequestHeader("uid") String uid,
            @RequestBody NpcRequest request) {
        
        log.info("Selecting NPC: uid={}, npcType={}", uid, request.getNpcType());
        
        try {
            // 세션이 없으면 자동 생성
            GameSessionDto session = gameSessionService.getOrCreateSession(uid, GameMode.COMPETITION);
            
            session.setNpcType(request.getNpcType());
            session.setNpcSelectionCompleted(true);
            
            gameSessionService.updateSession(uid, GameMode.COMPETITION, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("selected", true);
            data.put("npcType", request.getNpcType());
            
            log.info("NPC selected: uid={}, npcType={}", uid, request.getNpcType());
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to select NPC: uid={}", uid, e);
            return ApiResponse.error("FAILED", "NPC 선택 실패: " + e.getMessage());
        }
    }
    
    /**
     * 상품 구매 계산 미리보기
     * POST /api/v1/competition/calculate-product
     */
    @PostMapping("/calculate-product")
    public ApiResponse<ProductCalculationDto> calculateProduct(
            @RequestHeader("uid") String uid,
            @RequestBody ProductCalculationRequest request) {
        
        log.info("Calculating product: uid={}, type={}, key={}, action={}", 
            uid, request.getProductType(), request.getProductKey(), request.getAction());
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.COMPETITION);
            if (session == null) {
                return ApiResponse.error("SESSION_NOT_FOUND", "게임 세션을 찾을 수 없습니다.");
            }
            
            PortfolioDto portfolio = session.getPortfolio();
            Long currentCash = portfolio.getCash();
            
            ProductCalculationDto result = calculateProductInternal(
                session, portfolio, request, currentCash);
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("Failed to calculate product: uid={}", uid, e);
            return ApiResponse.error("FAILED", "상품 계산 실패: " + e.getMessage());
        }
    }
    
    /**
     * 상품 계산 내부 로직
     */
    private ProductCalculationDto calculateProductInternal(
            GameSessionDto session, PortfolioDto portfolio,
            ProductCalculationRequest request, Long currentCash) {
        
        String productType = request.getProductType().toUpperCase();
        String productKey = request.getProductKey();
        String action = request.getAction() != null ? request.getAction().toUpperCase() : "BUY";
        
        ProductCalculationDto.ProductCalculationDtoBuilder builder = ProductCalculationDto.builder()
            .productType(productType)
            .productKey(productKey)
            .action(action);
        
        switch (productType) {
            case "STOCK":
                return calculateStock(session, portfolio, request, currentCash, builder);
            case "FUND":
                return calculateFund(session, portfolio, request, currentCash, builder);
            case "DEPOSIT":
                return calculateDeposit(session, request, currentCash, builder);
            case "SAVING":
                return calculateSaving(session, request, currentCash, builder);
            case "BOND":
                return calculateBond(session, request, currentCash, builder);
            default:
                return builder
                    .productName("알 수 없는 상품")
                    .insufficientCash(false)
                    .build();
        }
    }
    
    /**
     * 주식 계산
     */
    private ProductCalculationDto calculateStock(
            GameSessionDto session, PortfolioDto portfolio,
            ProductCalculationRequest request, Long currentCash,
            ProductCalculationDto.ProductCalculationDtoBuilder builder) {
        
        String stockId = request.getProductKey();
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 0;
        String action = request.getAction() != null ? request.getAction().toUpperCase() : "BUY";
        
        Long currentPrice = marketEventService.getCurrentStockPrice(
            stockId, session.getGameMode(), session.getCurrentRound());
        
        // 기존 보유 주식 찾기
        StockHoldingDto existingStock = portfolio.getStocks().stream()
            .filter(s -> s.getStockId().equals(stockId))
            .findFirst()
            .orElse(null);
        
        int currentQuantity = existingStock != null ? existingStock.getQuantity() : 0;
        long currentAvgPrice = existingStock != null ? existingStock.getAvgPrice() : 0;
        
        if ("BUY".equals(action)) {
            long totalCost = currentPrice * quantity;
            boolean insufficientCash = currentCash < totalCost;
            
            // 매수 후 예상 값
            int expectedQuantity = currentQuantity + quantity;
            long totalPurchaseAmount = (currentAvgPrice * currentQuantity) + (currentPrice * quantity);
            long expectedAvgPrice = expectedQuantity > 0 ? totalPurchaseAmount / expectedQuantity : currentPrice;
            long expectedEvaluation = currentPrice * expectedQuantity;
            
            return builder
                .productName(getStockName(stockId))
                .requestQuantity(quantity)
                .currentPrice(currentPrice)
                .quantity(quantity)
                .totalCost(totalCost)
                .expectedCashAfter(currentCash - totalCost)
                .insufficientCash(insufficientCash)
                .expectedQuantityAfter(expectedQuantity)
                .expectedAvgPrice(expectedAvgPrice)
                .expectedEvaluationAmount(expectedEvaluation)
                .build();
        } else { // SELL
            long totalReceived = currentPrice * quantity;
            boolean canSell = currentQuantity >= quantity;
            
            int expectedQuantity = Math.max(0, currentQuantity - quantity);
            long expectedEvaluation = currentPrice * expectedQuantity;
            
            return builder
                .productName(getStockName(stockId))
                .requestQuantity(quantity)
                .currentPrice(currentPrice)
                .quantity(quantity)
                .totalCost(-totalReceived)
                .expectedCashAfter(currentCash + totalReceived)
                .insufficientCash(!canSell)
                .expectedQuantityAfter(expectedQuantity)
                .expectedAvgPrice(currentAvgPrice)
                .expectedEvaluationAmount(expectedEvaluation)
                .build();
        }
    }
    
    /**
     * 펀드 계산
     */
    private ProductCalculationDto calculateFund(
            GameSessionDto session, PortfolioDto portfolio,
            ProductCalculationRequest request, Long currentCash,
            ProductCalculationDto.ProductCalculationDtoBuilder builder) {
        
        String fundId = request.getProductKey();
        Long amount = request.getAmount() != null ? request.getAmount() : 0L;
        String action = request.getAction() != null ? request.getAction().toUpperCase() : "BUY";
        
        Long currentNav = marketEventService.getCurrentFundNav(
            fundId, session.getGameMode(), session.getCurrentRound());
        
        // 좌수 계산
        int shares = (int) Math.round((double) amount / currentNav);
        long totalCost = currentNav * shares;
        
        // 기존 보유 펀드 찾기
        FundHoldingDto existingFund = portfolio.getFunds().stream()
            .filter(f -> f.getFundId().equals(fundId))
            .findFirst()
            .orElse(null);
        
        int currentShares = existingFund != null ? existingFund.getShares() : 0;
        long currentAvgNav = existingFund != null ? existingFund.getAvgNav() : 0;
        
        if ("BUY".equals(action)) {
            boolean insufficientCash = currentCash < totalCost;
            
            // 매수 후 예상 값
            int expectedShares = currentShares + shares;
            long totalPurchaseAmount = (currentAvgNav * currentShares) + (currentNav * shares);
            long expectedAvgNav = expectedShares > 0 ? totalPurchaseAmount / expectedShares : currentNav;
            long expectedEvaluation = currentNav * expectedShares;
            
            return builder
                .productName(getFundName(fundId))
                .requestAmount(amount)
                .currentNav(currentNav)
                .shares(shares)
                .totalCost(totalCost)
                .expectedCashAfter(currentCash - totalCost)
                .insufficientCash(insufficientCash)
                .expectedSharesAfter(expectedShares)
                .expectedAvgNav(expectedAvgNav)
                .expectedEvaluationAmount(expectedEvaluation)
                .build();
        } else { // SELL
            boolean canSell = currentShares >= shares;
            long totalReceived = currentNav * shares;
            
            int expectedShares = Math.max(0, currentShares - shares);
            long expectedEvaluation = currentNav * expectedShares;
            
            return builder
                .productName(getFundName(fundId))
                .requestAmount(amount)
                .currentNav(currentNav)
                .shares(shares)
                .totalCost(-totalReceived)
                .expectedCashAfter(currentCash + totalReceived)
                .insufficientCash(!canSell)
                .expectedSharesAfter(expectedShares)
                .expectedAvgNav(currentAvgNav)
                .expectedEvaluationAmount(expectedEvaluation)
                .build();
        }
    }
    
    /**
     * 예금 계산
     */
    private ProductCalculationDto calculateDeposit(
            GameSessionDto session, ProductCalculationRequest request, Long currentCash,
            ProductCalculationDto.ProductCalculationDtoBuilder builder) {
        
        Long amount = request.getAmount() != null ? request.getAmount() : 0L;
        boolean insufficientCash = currentCash < amount;
        
        // 만기 계산 (경쟁모드)
        int maturityMonths = GameConstants.COMPETITION_DEPOSIT_MATURITY_MONTHS;
        int maturityRound = Math.min(
            session.getCurrentRound() + maturityMonths,
            session.getGameMode().getMaxRounds());
        
        // 예상 이자 계산
        BigDecimal interestRate = GameConstants.DEPOSIT_BASE_RATE;
        BigDecimal expectedMaturity = depositService.calculateDepositMaturity(
            BigDecimal.valueOf(amount), interestRate, maturityMonths);
        long expectedInterest = expectedMaturity.longValue() - amount;
        
        return builder
            .productName("예금")
            .requestAmount(amount)
            .totalCost(amount)
            .expectedCashAfter(currentCash - amount)
            .insufficientCash(insufficientCash)
            .interestRate(interestRate)
            .preferentialApplied(false)
            .maturityRound(maturityRound)
            .expectedMaturityAmount(expectedMaturity.longValue())
            .expectedInterest(expectedInterest)
            .build();
    }
    
    /**
     * 적금 계산
     */
    private ProductCalculationDto calculateSaving(
            GameSessionDto session, ProductCalculationRequest request, Long currentCash,
            ProductCalculationDto.ProductCalculationDtoBuilder builder) {
        
        String productKey = request.getProductKey();
        Long monthlyAmount = request.getAmount() != null ? request.getAmount() : 0L;
        boolean insufficientCash = currentCash < monthlyAmount;
        
        // 상품별 금리 및 만기 (경쟁모드)
        BigDecimal interestRate;
        int maturityMonths;
        String productName;
        
        if ("SAVING_A".equals(productKey)) {
            interestRate = GameConstants.SAVING_A_BASE_RATE;
            maturityMonths = GameConstants.COMPETITION_SAVING_A_MATURITY_MONTHS;
            productName = "적금 A";
        } else {
            interestRate = GameConstants.SAVING_B_BASE_RATE;
            maturityMonths = GameConstants.COMPETITION_SAVING_B_MATURITY_MONTHS;
            productName = "적금 B";
        }
        
        int maturityRound = Math.min(
            session.getCurrentRound() + maturityMonths,
            session.getGameMode().getMaxRounds());
        
        // 예상 만기 금액 계산
        BigDecimal expectedMaturity = depositService.calculateSavingMaturity(
            BigDecimal.valueOf(monthlyAmount), interestRate, maturityMonths);
        long totalContribution = monthlyAmount * maturityMonths;
        long expectedInterest = expectedMaturity.longValue() - totalContribution;
        
        return builder
            .productName(productName)
            .requestAmount(monthlyAmount)
            .totalCost(monthlyAmount)
            .expectedCashAfter(currentCash - monthlyAmount)
            .insufficientCash(insufficientCash)
            .interestRate(interestRate)
            .preferentialApplied(false)
            .maturityRound(maturityRound)
            .expectedMaturityAmount(expectedMaturity.longValue())
            .expectedInterest(expectedInterest)
            .build();
    }
    
    /**
     * 채권 계산
     */
    private ProductCalculationDto calculateBond(
            GameSessionDto session, ProductCalculationRequest request, Long currentCash,
            ProductCalculationDto.ProductCalculationDtoBuilder builder) {
        
        String bondId = request.getProductKey();
        Long amount = request.getAmount() != null ? request.getAmount() : 0L;
        boolean insufficientCash = currentCash < amount;
        
        // 기준금리 조회
        BigDecimal baseRate = marketEventService.getBaseRate(session);
        
        // 채권별 금리 및 만기 (경쟁모드)
        BigDecimal interestRate;
        int maturityMonths;
        String productName;
        
        if ("BOND_NATIONAL".equals(bondId)) {
            interestRate = baseRate.add(GameConstants.BOND_NATIONAL_SPREAD);
            maturityMonths = GameConstants.COMPETITION_BOND_NATIONAL_MATURITY_MONTHS;
            productName = "국채";
        } else {
            interestRate = baseRate.add(GameConstants.BOND_CORPORATE_SPREAD);
            maturityMonths = GameConstants.COMPETITION_BOND_CORPORATE_MATURITY_MONTHS;
            productName = "회사채";
        }
        
        int maturityRound = session.getCurrentRound() + maturityMonths;
        
        // 예상 이자 계산 (단순 계산)
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        long expectedInterest = BigDecimal.valueOf(amount)
            .multiply(monthlyRate)
            .multiply(BigDecimal.valueOf(maturityMonths))
            .longValue();
        long expectedMaturityAmount = amount + expectedInterest;
        
        return builder
            .productName(productName)
            .requestAmount(amount)
            .totalCost(amount)
            .expectedCashAfter(currentCash - amount)
            .insufficientCash(insufficientCash)
            .interestRate(interestRate)
            .preferentialApplied(false)
            .maturityRound(maturityRound)
            .expectedMaturityAmount(expectedMaturityAmount)
            .expectedInterest(expectedInterest)
            .build();
    }
    
    /**
     * 주식명 조회
     */
    private String getStockName(String stockId) {
        Map<String, String> stockNames = Map.of(
            "STOCK_01", "성장형주식",
            "STOCK_02", "가치형주식",
            "STOCK_03", "배당형주식",
            "STOCK_04", "IT주식",
            "STOCK_05", "바이오주식",
            "STOCK_06", "금융주식",
            "STOCK_07", "에너지주식"
        );
        return stockNames.getOrDefault(stockId, stockId);
    }
    
    /**
     * 펀드명 조회
     */
    private String getFundName(String fundId) {
        Map<String, String> fundNames = Map.of(
            "FUND_01", "성장형펀드",
            "FUND_02", "IT성장펀드",
            "FUND_03", "배당형펀드"
        );
        return fundNames.getOrDefault(fundId, fundId);
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
    
    /**
     * 랭킹 조회 (목업 데이터)
     * GET /api/v1/competition/ranking
     * 
     * TODO: DB 연동 시 실제 랭킹 데이터로 대체 필요
     */
    @GetMapping("/ranking")
    public ApiResponse<Map<String, Object>> getRanking(@RequestHeader("uid") String uid) {
        
        log.info("Getting ranking (MOCK): uid={}", uid);
        
        try {
            // 목업 랭킹 데이터 생성
            List<Map<String, Object>> mockRankings = Arrays.asList(
                createMockRankingEntry(1, "금융천재", 2847.5, 11780000L, "135.6%", false),
                createMockRankingEntry(2, "투자고수", 2654.3, 10850000L, "117.0%", false),
                createMockRankingEntry(3, "재테크왕", 2512.8, 10240000L, "104.8%", true),
                createMockRankingEntry(4, "머니메이커", 2389.2, 9685000L, "93.7%", false),
                createMockRankingEntry(5, "자산가", 2276.4, 9120000L, "82.4%", false),
                createMockRankingEntry(6, "포트폴리오마스터", 2165.7, 8654000L, "73.1%", false),
                createMockRankingEntry(7, "펀드러너", 2058.3, 8245000L, "64.9%", false),
                createMockRankingEntry(8, "리스크헌터", 1954.2, 7890000L, "57.8%", false),
                createMockRankingEntry(9, "배당수집가", 1856.8, 7512000L, "50.2%", false),
                createMockRankingEntry(10, "장기투자자", 1765.4, 7185000L, "43.7%", false)
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("rankings", mockRankings);
            data.put("myRank", 3);
            data.put("totalPlayers", 150);
            data.put("updateTime", java.time.LocalDateTime.now().toString());
            data.put("isMockData", true);
            
            log.info("Ranking retrieved (MOCK): myRank=3, totalPlayers=150");
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to get ranking: uid={}", uid, e);
            return ApiResponse.error("FAILED", "랭킹 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 다른 사용자 포트폴리오 조회 (목업 데이터)
     * GET /api/v1/competition/portfolio/{targetUid}
     * 
     * TODO: DB 연동 시 실제 포트폴리오 데이터로 대체 필요
     */
    @GetMapping("/portfolio/{targetUid}")
    public ApiResponse<Map<String, Object>> getUserPortfolio(
            @RequestHeader("uid") String uid,
            @PathVariable String targetUid) {
        
        log.info("Getting user portfolio (MOCK): requester={}, target={}", uid, targetUid);
        
        try {
            // 목업 포트폴리오 데이터 생성
            Map<String, Object> mockPortfolio = createMockPortfolioData(targetUid);
            
            Map<String, Object> data = new HashMap<>();
            data.put("uid", targetUid);
            data.put("nickname", mockPortfolio.get("nickname"));
            data.put("rank", mockPortfolio.get("rank"));
            data.put("totalScore", mockPortfolio.get("totalScore"));
            data.put("returnRate", mockPortfolio.get("returnRate"));
            data.put("finalNetWorth", mockPortfolio.get("finalNetWorth"));
            data.put("portfolio", mockPortfolio.get("portfolio"));
            data.put("isMockData", true);
            
            log.info("Portfolio retrieved (MOCK): target={}, rank={}", targetUid, mockPortfolio.get("rank"));
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to get user portfolio: requester={}, target={}", uid, targetUid, e);
            return ApiResponse.error("FAILED", "포트폴리오 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 목업 포트폴리오 데이터 생성 (uid별로 다른 스타일)
     */
    private Map<String, Object> createMockPortfolioData(String targetUid) {
        // targetUid의 해시값으로 일관된 데이터 생성
        int userType = Math.abs(targetUid.hashCode() % 3);
        
        Map<String, Object> data = new HashMap<>();
        
        switch (userType) {
            case 0: // 공격형 투자자 (주식/펀드 중심)
                data.put("nickname", "주식고수");
                data.put("rank", 1);
                data.put("totalScore", 2847.5);
                data.put("returnRate", "135.6%");
                data.put("finalNetWorth", 11780000L);
                data.put("portfolio", createAggressivePortfolio());
                break;
            case 1: // 균형형 투자자
                data.put("nickname", "균형투자자");
                data.put("rank", 5);
                data.put("totalScore", 2276.4);
                data.put("returnRate", "82.4%");
                data.put("finalNetWorth", 9120000L);
                data.put("portfolio", createBalancedPortfolio());
                break;
            case 2: // 안정형 투자자 (예적금/채권 중심)
                data.put("nickname", "안정추구");
                data.put("rank", 8);
                data.put("totalScore", 1954.2);
                data.put("returnRate", "57.8%");
                data.put("finalNetWorth", 7890000L);
                data.put("portfolio", createConservativePortfolio());
                break;
        }
        
        return data;
    }
    
    /**
     * 공격형 포트폴리오 (주식/펀드 80%)
     */
    private Map<String, Object> createAggressivePortfolio() {
        Map<String, Object> portfolio = new HashMap<>();
        
        // Summary
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAssets", 11780000L);
        summary.put("totalLiabilities", 0L);
        summary.put("netWorth", 11780000L);
        
        Map<String, Object> allocation = new HashMap<>();
        allocation.put("cashRatio", 0.15);
        allocation.put("depositRatio", 0.05);
        allocation.put("savingRatio", 0.0);
        allocation.put("bondRatio", 0.0);
        allocation.put("stockRatio", 0.55);
        allocation.put("fundRatio", 0.25);
        allocation.put("pensionRatio", 0.0);
        summary.put("allocation", allocation);
        portfolio.put("summary", summary);
        
        // Holdings
        Map<String, Object> holdings = new HashMap<>();
        holdings.put("cash", 1767000L);
        
        // 예금 (소량)
        holdings.put("deposits", Arrays.asList(
            Map.of(
                "depositId", "DEP_1",
                "productKey", "DEPOSIT",
                "name", "정기예금",
                "principal", 500000L,
                "balance", 589000L,
                "expectedMaturityAmount", 589000L,
                "interestRate", 2.5,
                "subscriptionRound", 1,
                "maturityRound", 7,
                "accumulatedInterest", 89000L
            )
        ));
        
        holdings.put("savings", Arrays.asList());
        holdings.put("bonds", Arrays.asList());
        
        // 주식 (대량 보유, 고수익)
        holdings.put("stocks", Arrays.asList(
            Map.of(
                "stockId", "STOCK_01",
                "name", "에버반도체",
                "quantity", 50,
                "avgPrice", 95000L,
                "currentPrice", 145000L,
                "evaluationAmount", 7250000L,
                "profitLoss", 2500000L,
                "returnRate", 0.526
            ),
            Map.of(
                "stockId", "STOCK_03",
                "name", "온라인뱅크",
                "quantity", 30,
                "avgPrice", 28000L,
                "currentPrice", 38500L,
                "evaluationAmount", 1155000L,
                "profitLoss", 315000L,
                "returnRate", 0.375
            )
        ));
        
        // 펀드 (중간 보유)
        holdings.put("funds", Arrays.asList(
            Map.of(
                "fundId", "FUND_02",
                "name", "IT성장펀드",
                "shares", 152,
                "avgNav", 10200L,
                "currentNav", 12850L,
                "evaluationAmount", 1953200L,
                "profitLoss", 402800L,
                "returnRate", 0.26,
                "purchaseRound", 2
            )
        ));
        
        holdings.put("pensions", Arrays.asList());
        holdings.put("loans", Arrays.asList());
        
        portfolio.put("holdings", holdings);
        
        return portfolio;
    }
    
    /**
     * 균형형 포트폴리오 (모든 상품 골고루)
     */
    private Map<String, Object> createBalancedPortfolio() {
        Map<String, Object> portfolio = new HashMap<>();
        
        // Summary
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAssets", 9120000L);
        summary.put("totalLiabilities", 0L);
        summary.put("netWorth", 9120000L);
        
        Map<String, Object> allocation = new HashMap<>();
        allocation.put("cashRatio", 0.25);
        allocation.put("depositRatio", 0.12);
        allocation.put("savingRatio", 0.18);
        allocation.put("bondRatio", 0.10);
        allocation.put("stockRatio", 0.20);
        allocation.put("fundRatio", 0.10);
        allocation.put("pensionRatio", 0.05);
        summary.put("allocation", allocation);
        portfolio.put("summary", summary);
        
        // Holdings
        Map<String, Object> holdings = new HashMap<>();
        holdings.put("cash", 2280000L);
        
        // 예금
        holdings.put("deposits", Arrays.asList(
            Map.of(
                "depositId", "DEP_1",
                "productKey", "DEPOSIT",
                "name", "정기예금",
                "principal", 1000000L,
                "balance", 1094400L,
                "expectedMaturityAmount", 1094400L,
                "interestRate", 2.5,
                "subscriptionRound", 2,
                "maturityRound", 8,
                "accumulatedInterest", 94400L
            )
        ));
        
        // 적금
        Map<String, Object> saving1 = new HashMap<>();
        saving1.put("savingId", "SAV_1");
        saving1.put("productKey", "SAVING_A");
        saving1.put("name", "적금 A");
        saving1.put("monthlyAmount", 150000L);
        saving1.put("balance", 1638000L);
        saving1.put("expectedMaturityAmount", 1638000L);
        saving1.put("interestRate", 2.6);
        saving1.put("subscriptionRound", 2);
        saving1.put("maturityRound", 12);
        saving1.put("paymentCount", 11);
        saving1.put("accumulatedInterest", 138000L);
        holdings.put("savings", Arrays.asList(saving1));
        
        // 채권
        holdings.put("bonds", Arrays.asList(
            Map.of(
                "bondId", "BOND_1",
                "productKey", "NATIONAL_BOND",
                "name", "국채",
                "principal", 900000L,
                "balance", 912000L,
                "expectedMaturityAmount", 912000L,
                "interestRate", 3.0,
                "subscriptionRound", 5,
                "maturityRound", 11,
                "accumulatedInterest", 12000L
            )
        ));
        
        // 주식
        holdings.put("stocks", Arrays.asList(
            Map.of(
                "stockId", "STOCK_02",
                "name", "글로벌조선",
                "quantity", 20,
                "avgPrice", 85000L,
                "currentPrice", 95000L,
                "evaluationAmount", 1900000L,
                "profitLoss", 200000L,
                "returnRate", 0.118
            )
        ));
        
        // 펀드
        holdings.put("funds", Arrays.asList(
            Map.of(
                "fundId", "FUND_01",
                "name", "안정형펀드",
                "shares", 85,
                "avgNav", 10500L,
                "currentNav", 11200L,
                "evaluationAmount", 952000L,
                "profitLoss", 59500L,
                "returnRate", 0.067,
                "purchaseRound", 1
            )
        ));
        
        // 연금
        holdings.put("pensions", Arrays.asList(
            Map.of(
                "pensionId", "PENSION_1",
                "productKey", "PERSONAL_PENSION",
                "name", "개인연금(채권형)",
                "totalContribution", 400000L,
                "balance", 455600L,
                "expectedMaturityValue", 455600L,
                "returnRate", 3.2,
                "subscriptionRound", 3,
                "contributionCount", 10,
                "accumulatedReturn", 55600L
            )
        ));
        
        holdings.put("loans", Arrays.asList());
        
        portfolio.put("holdings", holdings);
        
        return portfolio;
    }
    
    /**
     * 안정형 포트폴리오 (예적금/채권 70%)
     */
    private Map<String, Object> createConservativePortfolio() {
        Map<String, Object> portfolio = new HashMap<>();
        
        // Summary
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAssets", 7890000L);
        summary.put("totalLiabilities", 0L);
        summary.put("netWorth", 7890000L);
        
        Map<String, Object> allocation = new HashMap<>();
        allocation.put("cashRatio", 0.20);
        allocation.put("depositRatio", 0.25);
        allocation.put("savingRatio", 0.30);
        allocation.put("bondRatio", 0.15);
        allocation.put("stockRatio", 0.05);
        allocation.put("fundRatio", 0.05);
        allocation.put("pensionRatio", 0.0);
        summary.put("allocation", allocation);
        portfolio.put("summary", summary);
        
        // Holdings
        Map<String, Object> holdings = new HashMap<>();
        holdings.put("cash", 1578000L);
        
        // 예금 (대량)
        holdings.put("deposits", Arrays.asList(
            Map.of(
                "depositId", "DEP_1",
                "productKey", "DEPOSIT",
                "name", "정기예금",
                "principal", 1800000L,
                "balance", 1972500L,
                "expectedMaturityAmount", 1972500L,
                "interestRate", 2.5,
                "subscriptionRound", 1,
                "maturityRound", 7,
                "accumulatedInterest", 172500L
            )
        ));
        
        // 적금 (대량)
        Map<String, Object> conservativeSaving1 = new HashMap<>();
        conservativeSaving1.put("savingId", "SAV_1");
        conservativeSaving1.put("productKey", "SAVING_A");
        conservativeSaving1.put("name", "적금 A");
        conservativeSaving1.put("monthlyAmount", 200000L);
        conservativeSaving1.put("balance", 2367200L);
        conservativeSaving1.put("expectedMaturityAmount", 2367200L);
        conservativeSaving1.put("interestRate", 2.6);
        conservativeSaving1.put("subscriptionRound", 1);
        conservativeSaving1.put("maturityRound", 12);
        conservativeSaving1.put("paymentCount", 12);
        conservativeSaving1.put("accumulatedInterest", 167200L);
        holdings.put("savings", Arrays.asList(conservativeSaving1));
        
        // 채권
        holdings.put("bonds", Arrays.asList(
            Map.of(
                "bondId", "BOND_1",
                "productKey", "NATIONAL_BOND",
                "name", "국채",
                "principal", 600000L,
                "balance", 1183500L,
                "expectedMaturityAmount", 1183500L,
                "interestRate", 3.0,
                "subscriptionRound", 3,
                "maturityRound", 9,
                "accumulatedInterest", 83500L
            ),
            Map.of(
                "bondId", "BOND_2",
                "productKey", "CORPORATE_BOND",
                "name", "회사채",
                "principal", 500000L,
                "balance", 594500L,
                "expectedMaturityAmount", 594500L,
                "interestRate", 3.5,
                "subscriptionRound", 6,
                "maturityRound", 12,
                "accumulatedInterest", 94500L
            )
        ));
        
        // 주식 (소량)
        holdings.put("stocks", Arrays.asList(
            Map.of(
                "stockId", "STOCK_05",
                "name", "포용생명",
                "quantity", 5,
                "avgPrice", 72000L,
                "currentPrice", 78000L,
                "evaluationAmount", 390000L,
                "profitLoss", 30000L,
                "returnRate", 0.083
            )
        ));
        
        // 펀드 (소량)
        holdings.put("funds", Arrays.asList(
            Map.of(
                "fundId", "FUND_03",
                "name", "배당형펀드",
                "shares", 38,
                "avgNav", 10000L,
                "currentNav", 10500L,
                "evaluationAmount", 399000L,
                "profitLoss", 19000L,
                "returnRate", 0.05,
                "purchaseRound", 1
            )
        ));
        
        holdings.put("pensions", Arrays.asList());
        holdings.put("loans", Arrays.asList());
        
        portfolio.put("holdings", holdings);
        
        return portfolio;
    }
    
    /**
     * 목업 랭킹 엔트리 생성 헬퍼 메서드
     */
    private Map<String, Object> createMockRankingEntry(
            int rank, 
            String nickname, 
            double totalScore,
            long netWorth,
            String returnRate,
            boolean isMe) {
        
        Map<String, Object> entry = new HashMap<>();
        entry.put("rank", rank);
        entry.put("nickname", nickname);
        entry.put("totalScore", Math.ceil(totalScore)); // 소수점 올림
        entry.put("finalNetWorth", netWorth);
        entry.put("returnRate", returnRate);
        entry.put("isMe", isMe);
        
        return entry;
    }
}

