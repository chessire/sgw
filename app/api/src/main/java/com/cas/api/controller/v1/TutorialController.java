package com.cas.api.controller.v1;

import com.cas.api.constant.GameConstants;
import com.cas.api.dto.domain.FundHoldingDto;
import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.dto.domain.LoanDto;
import com.cas.api.dto.domain.PortfolioDto;
import com.cas.api.dto.domain.StockHoldingDto;
import com.cas.api.dto.request.BuyAdditionalInfoRequest;
import com.cas.api.dto.request.CompleteVideoRequest;
import com.cas.api.dto.request.NpcRequest;
import com.cas.api.dto.request.PropensityResultRequest;
import com.cas.api.dto.request.PropensityTestRequest;
import com.cas.api.dto.request.ResolveLifeEventRequest;
import com.cas.api.dto.request.StartGameRequest;
import com.cas.api.dto.request.SubmitQuizRequest;
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
 * 튜토리얼 모드 Controller
 */
@Slf4j
@RestController
@RequestMapping("/v1/tutorial")
@RequiredArgsConstructor
public class TutorialController {
    
    private final GameSessionService gameSessionService;
    private final RoundService roundService;
    private final PortfolioService portfolioService;
    private final MarketEventService marketEventService;
    private final AdviceService adviceService;
    private final ClueService clueService;
    private final AchievementService achievementService;
    private final ActionService actionService;
    private final LifeEventService lifeEventService;
    private final RankingService rankingService;
    private final DepositService depositService;
    private final java.util.Random random = new java.util.Random();
    
    /**
     * 게임 시작
     * POST /api/v1/tutorial/start
     */
    @PostMapping("/start")
    public ApiResponse<RoundStateDto> startGame(
            @RequestHeader("uid") String uid,
            @RequestBody StartGameRequest request) {
        
        log.info("Starting tutorial game: uid={}", uid);
        
        try {
            // 기존 세션 확인
            GameSessionDto existingSession = gameSessionService.getSession(uid, GameMode.TUTORIAL);
            if (existingSession != null && !existingSession.getCompleted()) {
                log.warn("Tutorial game already in progress: uid={}", uid);
                return ApiResponse.error("GAME_IN_PROGRESS", "이미 진행 중인 게임이 있습니다.");
            }
            
            // 초기 수입/지출 정보
            Long monthlyIncome = request.getIncome() != null ? 
                request.getIncome().getMonthlyIncome() : GameConstants.DEFAULT_INITIAL_CASH;
            Long monthlyExpense = request.getExpense() != null ?
                request.getExpense().getMonthlyFixedExpense() : GameConstants.DEFAULT_MONTHLY_LIVING_EXPENSE;
            
            // 초기 포트폴리오 생성
            PortfolioDto portfolio = PortfolioDto.builder()
                .cash(GameConstants.DEFAULT_INITIAL_CASH)
                .deposits(new ArrayList<>())
                .savings(new ArrayList<>())
                .bonds(new ArrayList<>())
                .stocks(new ArrayList<>())
                .funds(new ArrayList<>())
                .pensions(new ArrayList<>())
                .build();
            
            // 주식 패턴 랜덤 결정 (게임 전체 라운드에 사용)
            Map<String, String> stockPatterns = new HashMap<>();
            Random random = new Random();
            for (int i = 1; i <= 7; i++) {
                String stockId = "STOCK_0" + i;
                String pattern = random.nextBoolean() ? "UP" : "DOWN";
                stockPatterns.put(stockId, pattern);
                log.debug("Stock pattern set: {}={}", stockId, pattern);
            }
            
            // 게임 세션 생성
            GameSessionDto session = GameSessionDto.builder()
                .uid(uid)
                .gameMode(GameMode.TUTORIAL)
                .currentRound(1)
                .completed(false)
                .monthlySalary(monthlyIncome)
                .monthlyLiving(monthlyExpense)
                .initialCash(GameConstants.DEFAULT_INITIAL_CASH) // 점수 계산용
                .monthlyInsurancePremium(GameConstants.DEFAULT_MONTHLY_INSURANCE_PREMIUM)
                .adviceUsedCount(0)
                .insuranceSubscribed(false)
                .loanUsed(false)
                .illegalLoanUsed(false)
                .insurableEventOccurred(false)
                .portfolio(portfolio)
                .stockPatterns(stockPatterns)
                .build();
            
            gameSessionService.createSession(uid, GameMode.TUTORIAL, session);
            
            // 라운드 시작 처리
            roundService.startRound(session, portfolio);
            
            // 포트폴리오 평가 (총자산, 순자산 등 계산)
            portfolioService.updatePortfolioSummary(portfolio);
            
            // 응답 생성
            RoundStateDto response = buildRoundState(session, portfolio);
            
            log.info("Tutorial game started: uid={}, round={}", uid, session.getCurrentRound());
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("Failed to start tutorial game: uid={}", uid, e);
            return ApiResponse.error("START_FAILED", "게임 시작 실패: " + e.getMessage());
        }
    }
    
    /**
     * 라운드 진행
     * POST /api/v1/tutorial/proceed-round
     */
    @PostMapping("/proceed-round")
    public ApiResponse<RoundStateDto> proceedRound(
            @RequestHeader("uid") String uid,
            @RequestBody Map<String, Object> actions) {
        
        log.info("Proceeding tutorial round: uid={}", uid);
        
        try {
            // 세션 조회
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.TUTORIAL);
            if (session == null) {
                log.warn("Tutorial session not found: uid={}", uid);
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
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
            // 응답 생성
            RoundStateDto response = buildRoundState(session, portfolio);
            
            // 자동납입 실패 정보 추가
            if (settlementResult != null && settlementResult.hasFailures()) {
                response.getRoundStart().setAutoPayments(settlementResult.getAutoPayments());
                response.getRoundStart().setAutoPaymentFailures(settlementResult.getAutoPaymentFailures());
            }
            
            log.info("Tutorial round proceeded: uid={}, round={}, completed={}, autoPaymentFailures={}", 
                uid, session.getCurrentRound(), session.getCompleted(),
                settlementResult != null ? settlementResult.getAutoPaymentFailures().size() : 0);
            
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("Failed to proceed tutorial round: uid={}", uid, e);
            return ApiResponse.error("PROCEED_FAILED", "라운드 진행 실패: " + e.getMessage());
        }
    }
    
    /**
     * NPC 조언 사용
     * POST /api/v1/tutorial/use-advice
     */
    @PostMapping("/use-advice")
    public ApiResponse<Map<String, Object>> useAdvice(
            @RequestHeader("uid") String uid,
            @RequestBody UseAdviceRequest request) {
        
        log.info("Using advice: uid={}, roundNo={}", uid, request.getRoundNo());
        
        try {
            // 세션 조회
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.TUTORIAL);
            if (session == null) {
                return ApiResponse.error("SESSION_NOT_FOUND", "게임 세션을 찾을 수 없습니다.");
            }
            
            // 조언 사용 가능 여부 확인
            if (!adviceService.canUseAdvice(session)) {
                return ApiResponse.error("ADVICE_LIMIT_EXCEEDED", "조언 횟수를 모두 사용했습니다.");
            }
            
            // 조언 사용 처리
            boolean success = adviceService.useAdvice(session);
            
            if (!success) {
                return ApiResponse.error("ADVICE_USE_FAILED", "조언 사용에 실패했습니다.");
            }
            
            // 업적 체크: 조언 수집가 (3회 사용)
            achievementService.checkAchievements(session);
            
            // 세션 업데이트
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
            // 응답 데이터
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
     * POST /api/v1/tutorial/buy-additional-info
     */
    @PostMapping("/buy-additional-info")
    public ApiResponse<Map<String, Object>> buyAdditionalInfo(
            @RequestHeader("uid") String uid,
            @RequestBody BuyAdditionalInfoRequest request) {
        
        log.info("Buying additional info: uid={}, infoKey={}", uid, request.getInfoKey());
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.TUTORIAL);
            if (session == null) {
                return ApiResponse.error("SESSION_NOT_FOUND", "게임 세션을 찾을 수 없습니다.");
            }
            
            PortfolioDto portfolio = session.getPortfolio();
            
            boolean success = clueService.buyAdditionalInfo(session, portfolio, request.getInfoKey());
            
            if (!success) {
                return ApiResponse.error("PURCHASE_FAILED", "현금이 부족하거나 구매에 실패했습니다.");
            }
            
            // 세션 업데이트
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
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
     * POST /api/v1/tutorial/resolve-life-event
     */
    @PostMapping("/resolve-life-event")
    public ApiResponse<Map<String, Object>> resolveLifeEvent(
            @RequestHeader("uid") String uid,
            @RequestBody ResolveLifeEventRequest request) {
        
        log.info("Resolving life event: uid={}, eventKey={}, resolutionType={}", 
            uid, request.getEventKey(), request.getResolutionType());
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.TUTORIAL);
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
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
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
     * 게임 결과 조회
     * GET /api/v1/tutorial/result
     */
    @GetMapping("/result")
    public ApiResponse<Map<String, Object>> getResult(@RequestHeader("uid") String uid) {
        log.info("Getting tutorial result: uid={}", uid);
        
        try {
            // 세션 조회
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.TUTORIAL);
            if (session == null) {
                return ApiResponse.error("SESSION_NOT_FOUND", "게임 세션을 찾을 수 없습니다.");
            }
            
            if (!session.getCompleted()) {
                return ApiResponse.error("GAME_NOT_COMPLETED", "게임이 아직 완료되지 않았습니다.");
            }
            
            PortfolioDto portfolio = session.getPortfolio();
            
            // 초기 자본 (튜토리얼은 가변: session에 저장된 값)
            long initialCash = session.getInitialCash() != null ? session.getInitialCash() : 0L;
            
            // 업적 체크 (게임 완료 시 모든 업적 체크)
            achievementService.checkAchievements(session);
            achievementService.checkFinancialComprehensive(session);
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
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
            double returnRate = initialCash > 0 ? ((double)profit / initialCash) * 100.0 : 0.0;
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
            
            log.info("Tutorial result retrieved: uid={}, netWorth={}, score={}", 
                uid, portfolio.getNetWorth(), scoreResult.getTotalScore());
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("Failed to get tutorial result: uid={}", uid, e);
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
        // 영상 시청 상태
        GameStatusDto.VideoStatusDto videoStatus = GameStatusDto.VideoStatusDto.builder()
            .depositVideoCompleted(session.getDepositVideoCompleted())
            .stockVideoCompleted(session.getStockVideoCompleted())
            .bondVideoCompleted(session.getBondVideoCompleted())
            .pensionVideoCompleted(session.getPensionVideoCompleted())
            .fundVideoCompleted(session.getFundVideoCompleted())
            .insuranceVideoCompleted(session.getInsuranceVideoCompleted())
            .build();
        
        // 퀴즈 정답 상태
        GameStatusDto.QuizStatusDto quizStatus = GameStatusDto.QuizStatusDto.builder()
            .depositQuizPassed(session.getDepositQuizPassed())
            .stockQuizPassed(session.getStockQuizPassed())
            .bondQuizPassed(session.getBondQuizPassed())
            .pensionQuizPassed(session.getPensionQuizPassed())
            .fundQuizPassed(session.getFundQuizPassed())
            .insuranceQuizPassed(session.getInsuranceQuizPassed())
            .build();
        
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
            // 영상/퀴즈 상태
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
        List<String> additionalInfoKeys = clueService.getAvailableAdditionalInfoKeys(currentRound, true);
        
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
     * GET /api/v1/tutorial/check-load
     * 
     * 저장된 게임 세션이 있으면 진행 상황 정보를 반환합니다.
     */
    @GetMapping("/check-load")
    public ApiResponse<Map<String, Object>> checkLoad(@RequestHeader("uid") String uid) {
        
        log.info("Checking load status: uid={}", uid);
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.TUTORIAL);
            
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
            
            // 진행 상황 플래그
            Map<String, Boolean> progress = new HashMap<>();
            progress.put("openingStoryCompleted", Boolean.TRUE.equals(session.getOpeningStoryCompleted()));
            progress.put("propensityTestCompleted", Boolean.TRUE.equals(session.getPropensityTestCompleted()));
            progress.put("resultAnalysisCompleted", Boolean.TRUE.equals(session.getResultAnalysisCompleted()));
            progress.put("npcAssignmentCompleted", Boolean.TRUE.equals(session.getNpcAssignmentCompleted()));
            data.put("progress", progress);
            
            // 영상 시청 완료 여부
            Map<String, Boolean> videoCompleted = new HashMap<>();
            videoCompleted.put("deposit", Boolean.TRUE.equals(session.getDepositVideoCompleted()));
            videoCompleted.put("stock", Boolean.TRUE.equals(session.getStockVideoCompleted()));
            videoCompleted.put("bond", Boolean.TRUE.equals(session.getBondVideoCompleted()));
            videoCompleted.put("pension", Boolean.TRUE.equals(session.getPensionVideoCompleted()));
            videoCompleted.put("fund", Boolean.TRUE.equals(session.getFundVideoCompleted()));
            videoCompleted.put("insurance", Boolean.TRUE.equals(session.getInsuranceVideoCompleted()));
            data.put("videoCompleted", videoCompleted);
            
            // 퀴즈 정답 여부 (우대금리 적용)
            Map<String, Boolean> quizPassed = new HashMap<>();
            quizPassed.put("deposit", Boolean.TRUE.equals(session.getDepositQuizPassed()));
            quizPassed.put("stock", Boolean.TRUE.equals(session.getStockQuizPassed()));
            quizPassed.put("bond", Boolean.TRUE.equals(session.getBondQuizPassed()));
            quizPassed.put("pension", Boolean.TRUE.equals(session.getPensionQuizPassed()));
            quizPassed.put("fund", Boolean.TRUE.equals(session.getFundQuizPassed()));
            quizPassed.put("insurance", Boolean.TRUE.equals(session.getInsuranceQuizPassed()));
            data.put("quizPassed", quizPassed);
            
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
            data.put("propensityType", session.getPropensityType());
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
     * POST /api/v1/tutorial/use-illegal-loan
     * 
     * 튜토리얼 4라운드에서 불법사금융 광고 클릭 시 호출
     * (교육 목적, 점수 영향 없음)
     */
    @PostMapping("/use-illegal-loan")
    public ApiResponse<Map<String, Object>> useIllegalLoan(@RequestHeader("uid") String uid) {
        
        log.info("Using illegal loan (tutorial - educational): uid={}", uid);
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.TUTORIAL);
            
            if (session == null) {
                return ApiResponse.error("SESSION_NOT_FOUND", "게임 세션을 찾을 수 없습니다.");
            }
            
            // 불법사금융 사용 플래그 설정
            session.setIllegalLoanUsed(true);
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("used", true);
            data.put("message", "불법사금융을 이용했습니다. (교육 목적 - 점수 영향 없음)");
            data.put("warning", "⚠️ 불법사금융은 금융범죄입니다. 실제로 이용하면 안 됩니다!");
            data.put("penalty", 0); // 튜토리얼은 패널티 없음
            
            log.info("Illegal loan used (tutorial): uid={}", uid);
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to use illegal loan: uid={}", uid, e);
            return ApiResponse.error("FAILED", "불법사금융 사용 처리 실패: " + e.getMessage());
        }
    }
    
    /**
     * 게임 초기화 (강제 종료)
     * DELETE /api/v1/tutorial/reset
     */
    @DeleteMapping("/reset")
    public ApiResponse<Map<String, Object>> resetGame(@RequestHeader("uid") String uid) {
        
        log.info("Resetting tutorial game: uid={}", uid);
        
        try {
            // 세션 삭제
            gameSessionService.deleteSession(uid, GameMode.TUTORIAL);
            
            Map<String, Object> data = new HashMap<>();
            data.put("reset", true);
            data.put("message", "게임이 초기화되었습니다. 새로 시작할 수 있습니다.");
            
            log.info("Tutorial game reset successfully: uid={}", uid);
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to reset tutorial game: uid={}", uid, e);
            return ApiResponse.error("FAILED", "게임 초기화 실패: " + e.getMessage());
        }
    }
    
    /**
     * 오프닝 스토리 완료
     * POST /api/v1/tutorial/complete-opening-story
     */
    @PostMapping("/complete-opening-story")
    public ApiResponse<Map<String, Object>> completeOpeningStory(@RequestHeader("uid") String uid) {
        
        log.info("Completing opening story: uid={}", uid);
        
        try {
            // 세션이 없으면 자동 생성 (getOrCreateSession 사용)
            GameSessionDto session = gameSessionService.getOrCreateSession(uid, GameMode.TUTORIAL);
            session.setOpeningStoryCompleted(true);
            
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("completed", true);
            
            log.info("Opening story completed: uid={}", uid);
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to complete opening story: uid={}", uid, e);
            return ApiResponse.error("FAILED", "오프닝 스토리 완료 실패: " + e.getMessage());
        }
    }
    
    /**
     * 재무성향검사 제출
     * POST /api/v1/tutorial/submit-propensity-test
     */
    @PostMapping("/submit-propensity-test")
    public ApiResponse<Map<String, Object>> submitPropensityTest(
            @RequestHeader("uid") String uid,
            @RequestBody PropensityTestRequest request) {
        
        log.info("Submitting propensity test: uid={}, answers={}", uid, request.getAnswers());
        
        try {
            // 세션이 없으면 자동 생성
            GameSessionDto session = gameSessionService.getOrCreateSession(uid, GameMode.TUTORIAL);
            
            session.setPropensityTestAnswers(request.getAnswers());
            session.setPropensityTestCompleted(true);
            
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("submitted", true);
            data.put("answers", request.getAnswers());
            
            log.info("Propensity test submitted: uid={}, answerCount={}", uid, request.getAnswers().size());
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to submit propensity test: uid={}", uid, e);
            return ApiResponse.error("FAILED", "재무성향검사 제출 실패: " + e.getMessage());
        }
    }
    
    /**
     * 재무성향 결과 저장
     * POST /api/v1/tutorial/save-propensity-result
     */
    @PostMapping("/save-propensity-result")
    public ApiResponse<Map<String, Object>> savePropensityResult(
            @RequestHeader("uid") String uid,
            @RequestBody PropensityResultRequest request) {
        
        log.info("Saving propensity result: uid={}, type={}", uid, request.getPropensityType());
        
        try {
            // 세션이 없으면 자동 생성
            GameSessionDto session = gameSessionService.getOrCreateSession(uid, GameMode.TUTORIAL);
            
            session.setPropensityType(request.getPropensityType());
            session.setResultAnalysisCompleted(true);
            
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("saved", true);
            data.put("propensityType", request.getPropensityType());
            
            log.info("Propensity result saved: uid={}, type={}", uid, request.getPropensityType());
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to save propensity result: uid={}", uid, e);
            return ApiResponse.error("FAILED", "재무성향 결과 저장 실패: " + e.getMessage());
        }
    }
    
    /**
     * NPC 배정
     * POST /api/v1/tutorial/assign-npc
     */
    @PostMapping("/assign-npc")
    public ApiResponse<Map<String, Object>> assignNpc(
            @RequestHeader("uid") String uid,
            @RequestBody NpcRequest request) {
        
        log.info("Assigning NPC: uid={}, npcType={}", uid, request.getNpcType());
        
        try {
            // 세션이 없으면 자동 생성
            GameSessionDto session = gameSessionService.getOrCreateSession(uid, GameMode.TUTORIAL);
            
            session.setNpcType(request.getNpcType());
            session.setNpcAssignmentCompleted(true);
            
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("assigned", true);
            data.put("npcType", request.getNpcType());
            data.put("propensityType", session.getPropensityType());
            
            log.info("NPC assigned: uid={}, npcType={}", uid, request.getNpcType());
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to assign NPC: uid={}", uid, e);
            return ApiResponse.error("FAILED", "NPC 배정 실패: " + e.getMessage());
        }
    }
    
    /**
     * 교육 영상 시청 완료
     * POST /api/v1/tutorial/complete-video
     */
    @PostMapping("/complete-video")
    public ApiResponse<Map<String, Object>> completeVideo(
            @RequestHeader("uid") String uid,
            @RequestBody CompleteVideoRequest request) {
        
        log.info("Completing video: uid={}, videoType={}", uid, request.getVideoType());
        
        try {
            GameSessionDto session = gameSessionService.getOrCreateSession(uid, GameMode.TUTORIAL);
            
            // 영상 타입에 따라 해당 필드 업데이트
            switch (request.getVideoType().toUpperCase()) {
                case "DEPOSIT":
                    session.setDepositVideoCompleted(true);
                    break;
                case "STOCK":
                    session.setStockVideoCompleted(true);
                    break;
                case "BOND":
                    session.setBondVideoCompleted(true);
                    break;
                case "PENSION":
                    session.setPensionVideoCompleted(true);
                    break;
                case "FUND":
                    session.setFundVideoCompleted(true);
                    break;
                case "INSURANCE":
                    session.setInsuranceVideoCompleted(true);
                    break;
                default:
                    return ApiResponse.error("INVALID_VIDEO_TYPE", "올바르지 않은 영상 타입입니다: " + request.getVideoType());
            }
            
            // 업적 체크: 금융 입문자 (모든 영상 시청)
            achievementService.checkAchievements(session);
            
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("completed", true);
            data.put("videoType", request.getVideoType());
            
            log.info("Video completed: uid={}, videoType={}", uid, request.getVideoType());
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to complete video: uid={}", uid, e);
            return ApiResponse.error("FAILED", "영상 시청 완료 처리 실패: " + e.getMessage());
        }
    }
    
    /**
     * 우대금리 퀴즈 정답 제출
     * POST /api/v1/tutorial/submit-quiz
     */
    @PostMapping("/submit-quiz")
    public ApiResponse<Map<String, Object>> submitQuiz(
            @RequestHeader("uid") String uid,
            @RequestBody SubmitQuizRequest request) {
        
        log.info("Submitting quiz: uid={}, productType={}", uid, request.getProductType());
        
        try {
            GameSessionDto session = gameSessionService.getOrCreateSession(uid, GameMode.TUTORIAL);
            
            // 상품 타입에 따라 우대금리 플래그 설정 (무조건 정답 처리)
            switch (request.getProductType().toUpperCase()) {
                case "DEPOSIT":
                    session.setDepositQuizPassed(true);
                    break;
                case "STOCK":
                    session.setStockQuizPassed(true);
                    break;
                case "BOND":
                    session.setBondQuizPassed(true);
                    break;
                case "PENSION":
                    session.setPensionQuizPassed(true);
                    break;
                case "FUND":
                    session.setFundQuizPassed(true);
                    break;
                case "INSURANCE":
                    session.setInsuranceQuizPassed(true);
                    break;
                default:
                    return ApiResponse.error("INVALID_PRODUCT_TYPE", "올바르지 않은 상품 타입입니다: " + request.getProductType());
            }
            
            gameSessionService.updateSession(uid, GameMode.TUTORIAL, session);
            
            Map<String, Object> data = new HashMap<>();
            data.put("correct", true);
            data.put("preferentialApplied", true);
            data.put("productType", request.getProductType());
            
            log.info("Quiz passed: uid={}, productType={}, preferential applied", uid, request.getProductType());
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to submit quiz: uid={}", uid, e);
            return ApiResponse.error("FAILED", "퀴즈 제출 실패: " + e.getMessage());
        }
    }
    
    /**
     * 상품 구매 계산 미리보기
     * POST /api/v1/tutorial/calculate-product
     */
    @PostMapping("/calculate-product")
    public ApiResponse<ProductCalculationDto> calculateProduct(
            @RequestHeader("uid") String uid,
            @RequestBody ProductCalculationRequest request) {
        
        log.info("Calculating product: uid={}, type={}, key={}, action={}", 
            uid, request.getProductType(), request.getProductKey(), request.getAction());
        
        try {
            GameSessionDto session = gameSessionService.getSession(uid, GameMode.TUTORIAL);
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
                .totalCost(-totalReceived)  // 매도는 음수
                .expectedCashAfter(currentCash + totalReceived)
                .insufficientCash(!canSell)  // 보유 수량 부족
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
        
        // 만기 계산
        int maturityMonths = GameConstants.TUTORIAL_DEPOSIT_MATURITY_MONTHS;
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
        
        // 상품별 금리 및 만기
        BigDecimal interestRate;
        int maturityMonths;
        String productName;
        
        if ("SAVING_A".equals(productKey)) {
            interestRate = GameConstants.SAVING_A_BASE_RATE;
            maturityMonths = GameConstants.TUTORIAL_SAVING_A_MATURITY_MONTHS;
            productName = "적금 A";
        } else {
            interestRate = GameConstants.SAVING_B_BASE_RATE;
            maturityMonths = GameConstants.TUTORIAL_SAVING_B_MATURITY_MONTHS;
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
            .totalCost(monthlyAmount)  // 첫 달 납입액
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
        
        // 채권별 금리 및 만기
        BigDecimal interestRate;
        int maturityMonths;
        String productName;
        
        if ("BOND_NATIONAL".equals(bondId)) {
            interestRate = baseRate.add(GameConstants.BOND_NATIONAL_SPREAD);
            maturityMonths = GameConstants.TUTORIAL_BOND_NATIONAL_MATURITY_MONTHS;
            productName = "국채";
        } else {
            interestRate = baseRate.add(GameConstants.BOND_CORPORATE_SPREAD);
            maturityMonths = GameConstants.TUTORIAL_BOND_CORPORATE_MATURITY_MONTHS;
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
}

