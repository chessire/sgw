package com.cas.api.service.game;

import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.dto.domain.PortfolioDto;
import com.cas.api.enums.GameMode;
import com.cas.api.service.external.TransactionService;
import com.cas.common.core.util.KinfaRunException;
import com.cas.common.infra.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 게임 데이터 로드/저장 Service
 * - DB에서 게임 데이터 로드 → Redis에 캐싱
 * - Redis 기반으로 게임 진행
 * - 라운드 종료 시 DB에 영구 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameLoadService {

    private final TransactionService transactionService;
    private final GameSessionService gameSessionService;
    private final CacheService cacheService;

    // Redis 키 패턴
    private static final String REDIS_KEY_GAME_DATA = "game:data:%s:%s"; // mbrSno:gameMode
    private static final String REDIS_KEY_TUTORIAL_PROGRESS = "game:tutorial:%s";
    private static final String REDIS_KEY_COMPETITION_PROGRESS = "game:competition:%s";
    private static final int GAME_DATA_TTL = 86400; // 24시간

    // 금융상품 코드 매핑 (FNPRD_NO)
    private static final Map<Integer, String> PRODUCT_NAMES = Map.of(
        1, "deposit", 2, "stock", 3, "bond",
        4, "pension", 5, "fund", 6, "insurance"
    );

    /**
     * 게임 시작 시 데이터 로드
     * 1. Redis 캐시 확인 → 있으면 바로 반환 (게임 이어하기)
     * 2. 없으면 DB에서 조회 → Redis에 캐싱 (게임 복원)
     * 3. DB에도 없으면 새 게임 안내
     * 
     * @param mbrSno 회원 일련번호
     * @param gameMode 게임 모드 (TUTORIAL/COMPETITION)
     * @return 게임 데이터 및 GameSessionDto
     */
    public Map<String, Object> loadGame(Long mbrSno, GameMode gameMode) {
        log.info("■ GameLoadService.loadGame - mbrSno: {}, gameMode: {}", mbrSno, gameMode);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. GameSessionService에서 Redis 세션 확인
            GameSessionDto existingSession = gameSessionService.getSession(String.valueOf(mbrSno), gameMode);
            
            if (existingSession != null) {
                log.info("■ Game session loaded from Redis (GameSessionService)");
                result.put("success", true);
                result.put("source", "redis");
                result.put("session", existingSession);
                result.put("message", "진행 중인 게임이 있습니다.");
                return result;
            }

            // 2. DB에서 조회
            Map<String, Object> dbData = loadFromDb(mbrSno, gameMode);

            if (dbData != null && dbData.get("activeGame") != null) {
                log.info("■ Game data loaded from DB, caching to Redis");

                // GameSessionDto로 변환
                GameSessionDto sessionDto = convertToGameSessionDto(dbData, mbrSno, gameMode);
                
                if (sessionDto != null) {
                    // Redis에 세션 저장 (GameSessionService 사용)
                    gameSessionService.updateSession(String.valueOf(mbrSno), gameMode, sessionDto);
                    
                    // 추가 데이터도 Redis에 캐싱
                    String redisKey = String.format(REDIS_KEY_GAME_DATA, mbrSno, gameMode.getCode());
                    cacheService.setObject(redisKey, dbData, GAME_DATA_TTL, TimeUnit.SECONDS);
                    
                    result.put("success", true);
                    result.put("source", "db");
                    result.put("session", sessionDto);
                    result.put("data", dbData);
                    result.put("message", "게임 데이터를 복원했습니다.");
                    return result;
                }
            }

            // 3. 완료된 게임만 있는 경우
            if (dbData != null && Boolean.TRUE.equals(dbData.get("allCompleted"))) {
                log.info("■ All games completed for mbrSno: {}", mbrSno);
                result.put("success", true);
                result.put("source", "new");
                result.put("data", null);
                result.put("allCompleted", true);
                result.put("message", "이전 게임이 모두 완료되었습니다. 새 게임을 시작하세요.");
                return result;
            }

            // 4. 데이터가 없으면 새 게임
            log.info("■ No existing game data, creating new game");
            result.put("success", true);
            result.put("source", "new");
            result.put("data", null);
            result.put("message", "새 게임을 시작합니다.");

        } catch (Exception e) {
            log.error("■ Error loading game: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "게임 데이터 로드 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * DB에서 게임 데이터 로드 (완전 구현)
     * 
     * 로드 순서:
     * 1. 사용자 기본 정보 (KMHAD055M)
     * 2. 튜토리얼/경쟁 목록 (KMHAD057M/060M)
     * 3. 진행 중인 게임 식별 (MFILE_FNSH_YN = 'N')
     * 4. 해당 게임의 라운드 정보 (KMHAD058M/061M)
     * 5. 학습 정보 (KMHAD063M)
     */
    private Map<String, Object> loadFromDb(Long mbrSno, GameMode gameMode) throws KinfaRunException {
        Map<String, Object> gameData = new HashMap<>();

        // 1. 사용자 기본 정보 로드
        HashMap<String, Object> userInfo = transactionService.getGameBasicInfo(mbrSno);
        if (userInfo != null) {
            gameData.put("user", userInfo);
            log.info("■ User info loaded: {}", userInfo);
        }

        if (gameMode == GameMode.TUTORIAL) {
            loadTutorialData(mbrSno, gameData);
        } else if (gameMode == GameMode.COMPETITION) {
            loadCompetitionData(mbrSno, gameData);
        }

        return gameData.isEmpty() ? null : gameData;
    }

    /**
     * 튜토리얼 데이터 로드
     */
    private void loadTutorialData(Long mbrSno, Map<String, Object> gameData) throws KinfaRunException {
        // 2. 튜토리얼 목록 조회
        HashMap<String, Object> tutorialResult = transactionService.getTutorialListByUser(mbrSno);
        List<Map<String, Object>> tutorialList = extractList(tutorialResult, "data");

        if (tutorialList == null || tutorialList.isEmpty()) {
            log.info("■ No tutorials found for mbrSno: {}", mbrSno);
            return;
        }

        gameData.put("tutorials", tutorialList);
        log.info("■ Found {} tutorials", tutorialList.size());

        // 3. 진행 중인 튜토리얼 찾기 (MFILE_FNSH_YN = 'N' 또는 null)
        Map<String, Object> activeTutorial = findActiveGame(tutorialList, "MFILE_FNSH_YN");

        if (activeTutorial == null) {
            log.info("■ No active tutorial found, all completed");
            gameData.put("allCompleted", true);
            return;
        }

        Long ttrlSno = extractLong(activeTutorial, "TTRL_SNO");
        gameData.put("activeGame", activeTutorial);
        gameData.put("activeGameId", ttrlSno);
        log.info("■ Active tutorial found: TTRL_SNO={}", ttrlSno);

        // 4. 해당 튜토리얼의 전체 라운드 정보 조회
        HashMap<String, Object> roundsResult = transactionService.getTutorialAllRounds(ttrlSno);
        List<Map<String, Object>> rounds = extractList(roundsResult, "data");

        if (rounds != null && !rounds.isEmpty()) {
            gameData.put("rounds", rounds);

            // 마지막 라운드 정보 추출
            Map<String, Object> lastRound = rounds.get(rounds.size() - 1);
            Integer lastRoundNo = extractInt(lastRound, "TTRL_RND_NO");
            gameData.put("currentRound", lastRoundNo);
            gameData.put("lastRoundData", lastRound);

            // 포트폴리오 정보 추출
            Map<String, Object> portfolio = extractPortfolioFromRound(lastRound);
            gameData.put("portfolio", portfolio);

            log.info("■ Loaded {} rounds, current round: {}", rounds.size(), lastRoundNo);
        } else {
            // 라운드가 없으면 1라운드 시작 전
            gameData.put("currentRound", 0);
            log.info("■ No rounds found, game not started yet");
        }

        // 5. 학습 정보 조회 (영상 시청, 퀴즈 정답 여부)
        HashMap<String, Object> learningResult = transactionService.getLearningInfoByTutorial(ttrlSno);
        List<Map<String, Object>> learningList = extractList(learningResult, "data");

        if (learningList != null && !learningList.isEmpty()) {
            Map<String, Object> learningStatus = parseLearningStatus(learningList);
            gameData.put("learning", learningStatus);
            log.info("■ Learning info loaded: {} items", learningList.size());
        }
    }

    /**
     * 경쟁 모드 데이터 로드
     */
    private void loadCompetitionData(Long mbrSno, Map<String, Object> gameData) throws KinfaRunException {
        // 2. 경쟁 목록 조회
        HashMap<String, Object> competitionResult = transactionService.getCompetitionListByUser(mbrSno);
        List<Map<String, Object>> competitionList = extractList(competitionResult, "data");

        if (competitionList == null || competitionList.isEmpty()) {
            log.info("■ No competitions found for mbrSno: {}", mbrSno);
            return;
        }

        gameData.put("competitions", competitionList);
        log.info("■ Found {} competitions", competitionList.size());

        // 3. 진행 중인 경쟁 찾기 (MFILE_FNSH_YN = 'N' 또는 null)
        Map<String, Object> activeCompetition = findActiveGame(competitionList, "MFILE_FNSH_YN");

        if (activeCompetition == null) {
            log.info("■ No active competition found, all completed");
            gameData.put("allCompleted", true);
            return;
        }

        Long cmpttSno = extractLong(activeCompetition, "CMPTT_SNO");
        gameData.put("activeGame", activeCompetition);
        gameData.put("activeGameId", cmpttSno);
        log.info("■ Active competition found: CMPTT_SNO={}", cmpttSno);

        // 4. 해당 경쟁의 전체 라운드 정보 조회
        HashMap<String, Object> roundsResult = transactionService.getCompetitionAllRounds(cmpttSno);
        List<Map<String, Object>> rounds = extractList(roundsResult, "data");

        if (rounds != null && !rounds.isEmpty()) {
            gameData.put("rounds", rounds);

            // 마지막 라운드 정보 추출
            Map<String, Object> lastRound = rounds.get(rounds.size() - 1);
            Integer lastRoundNo = extractInt(lastRound, "CMPTT_RND_NO");
            gameData.put("currentRound", lastRoundNo);
            gameData.put("lastRoundData", lastRound);

            // 포트폴리오 정보 추출
            Map<String, Object> portfolio = extractPortfolioFromRound(lastRound);
            gameData.put("portfolio", portfolio);

            log.info("■ Loaded {} rounds, current round: {}", rounds.size(), lastRoundNo);
        } else {
            gameData.put("currentRound", 0);
            log.info("■ No rounds found, game not started yet");
        }

        // 5. 학습 정보 조회
        HashMap<String, Object> learningResult = transactionService.getLearningInfoByCompetition(cmpttSno);
        List<Map<String, Object>> learningList = extractList(learningResult, "data");

        if (learningList != null && !learningList.isEmpty()) {
            Map<String, Object> learningStatus = parseLearningStatus(learningList);
            gameData.put("learning", learningStatus);
            log.info("■ Learning info loaded: {} items", learningList.size());
        }
    }

    /**
     * DB 데이터를 GameSessionDto로 변환
     * Redis에 저장할 게임 세션 객체 생성
     */
    private GameSessionDto convertToGameSessionDto(Map<String, Object> dbData, Long mbrSno, GameMode gameMode) {
        try {
            GameSessionDto.GameSessionDtoBuilder builder = GameSessionDto.builder()
                .uid(String.valueOf(mbrSno))
                .gameMode(gameMode)
                .startedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .completed(false);

            // 현재 라운드
            Integer currentRound = extractInt(dbData, "currentRound");
            builder.currentRound(currentRound != null && currentRound > 0 ? currentRound : 1);

            // 게임 정보 추출
            Map<String, Object> activeGame = (Map<String, Object>) dbData.get("activeGame");
            if (activeGame != null) {
                // NPC 정보
                Integer npcNo = extractInt(activeGame, "NPC_NO");
                if (npcNo != null) {
                    builder.npcType(npcNo == 1 ? "POYONGI" : "CHAEUMI");
                }

                // 월급, 생활비
                builder.monthlySalary(extractLong(activeGame, "MM_AVRG_INCME_AMT"));
                builder.monthlyLiving(extractLong(activeGame, "FIX_EXPND_AMT"));
                
                // 재무성향
                String fipatNm = (String) activeGame.get("FIPAT_NM");
                if (fipatNm != null) {
                    builder.propensityType(fipatNm);
                }
            }

            // 포트폴리오 정보
            Map<String, Object> portfolioMap = (Map<String, Object>) dbData.get("portfolio");
            if (portfolioMap != null) {
                PortfolioDto portfolio = PortfolioDto.builder()
                    .cash(extractLong(portfolioMap, "CASH_PTFLO_AMT"))
                    .totalAssets(extractLong(portfolioMap, "TOT_ASST_AMT"))
                    .totalLiabilities(extractLong(portfolioMap, "LON_AMT"))
                    .build();
                builder.portfolio(portfolio);
                builder.initialCash(extractLong(portfolioMap, "CASH_PTFLO_AMT"));
            }

            // 학습 정보 (퀴즈/영상)
            Map<String, Object> learning = (Map<String, Object>) dbData.get("learning");
            if (learning != null) {
                // 영상 시청 완료 여부
                builder.depositVideoCompleted(Boolean.TRUE.equals(learning.get("depositVideoCompleted")));
                builder.stockVideoCompleted(Boolean.TRUE.equals(learning.get("stockVideoCompleted")));
                builder.bondVideoCompleted(Boolean.TRUE.equals(learning.get("bondVideoCompleted")));
                builder.pensionVideoCompleted(Boolean.TRUE.equals(learning.get("pensionVideoCompleted")));
                builder.fundVideoCompleted(Boolean.TRUE.equals(learning.get("fundVideoCompleted")));
                builder.insuranceVideoCompleted(Boolean.TRUE.equals(learning.get("insuranceVideoCompleted")));

                // 퀴즈 정답 여부
                builder.depositQuizPassed(Boolean.TRUE.equals(learning.get("depositQuizPassed")));
                builder.stockQuizPassed(Boolean.TRUE.equals(learning.get("stockQuizPassed")));
                builder.bondQuizPassed(Boolean.TRUE.equals(learning.get("bondQuizPassed")));
                builder.pensionQuizPassed(Boolean.TRUE.equals(learning.get("pensionQuizPassed")));
                builder.fundQuizPassed(Boolean.TRUE.equals(learning.get("fundQuizPassed")));
                builder.insuranceQuizPassed(Boolean.TRUE.equals(learning.get("insuranceQuizPassed")));
            } else {
                // 학습 정보 없으면 기본값 false
                builder.depositVideoCompleted(false).stockVideoCompleted(false)
                    .bondVideoCompleted(false).pensionVideoCompleted(false)
                    .fundVideoCompleted(false).insuranceVideoCompleted(false);
                builder.depositQuizPassed(false).stockQuizPassed(false)
                    .bondQuizPassed(false).pensionQuizPassed(false)
                    .fundQuizPassed(false).insuranceQuizPassed(false);
            }

            // 기본값 설정
            builder.adviceUsedCount(0);
            builder.insuranceSubscribed(false);
            builder.loanUsed(false);
            builder.illegalLoanUsed(false);
            builder.insurableEventOccurred(false);
            builder.openingStoryCompleted(true); // DB에서 로드한 경우 오프닝 완료
            builder.propensityTestCompleted(true); // DB에서 로드한 경우 성향검사 완료
            builder.resultAnalysisCompleted(true);
            builder.npcAssignmentCompleted(true);
            builder.npcSelectionCompleted(true);
            builder.achievedAchievements(new HashSet<>());
            builder.achievementProgress(new HashMap<>());

            GameSessionDto session = builder.build();
            log.info("■ GameSessionDto created: uid={}, mode={}, round={}", 
                session.getUid(), session.getGameMode(), session.getCurrentRound());

            return session;

        } catch (Exception e) {
            log.error("■ Error converting to GameSessionDto: {}", e.getMessage(), e);
            return null;
        }
    }

    // ========================================
    // 유틸리티 메서드
    // ========================================

    /**
     * 진행 중인 게임 찾기 (가장 최근의 미완료 게임)
     * MFILE_FNSH_YN = 'N' 또는 null인 게임 검색
     */
    private Map<String, Object> findActiveGame(List<Map<String, Object>> gameList, String finishField) {
        if (gameList == null || gameList.isEmpty()) return null;

        // 역순으로 탐색 (가장 최근 게임부터)
        for (int i = gameList.size() - 1; i >= 0; i--) {
            Map<String, Object> game = gameList.get(i);
            Object finishYn = game.get(finishField);

            // 'N' 또는 null이면 진행 중
            if (finishYn == null || "N".equals(finishYn.toString())) {
                return game;
            }
        }
        return null;
    }

    /**
     * 라운드 데이터에서 포트폴리오 정보 추출
     */
    private Map<String, Object> extractPortfolioFromRound(Map<String, Object> roundData) {
        Map<String, Object> portfolio = new HashMap<>();

        // 자산 금액 필드들
        portfolio.put("TOT_ASST_AMT", roundData.get("TOT_ASST_AMT"));
        portfolio.put("LON_AMT", roundData.get("LON_AMT"));
        portfolio.put("CASH_PTFLO_AMT", roundData.get("CASH_PTFLO_AMT"));
        portfolio.put("DPSIT_PTFLO_AMT", roundData.get("DPSIT_PTFLO_AMT"));
        portfolio.put("ISV_PTFLO_AMT", roundData.get("ISV_PTFLO_AMT"));
        portfolio.put("ANNTY_PTFLO_AMT", roundData.get("ANNTY_PTFLO_AMT"));
        portfolio.put("STOCK_PTFLO_AMT", roundData.get("STOCK_PTFLO_AMT"));
        portfolio.put("FUND_PTFLO_AMT", roundData.get("FUND_PTFLO_AMT"));
        portfolio.put("BOND_PTFLO_AMT", roundData.get("BOND_PTFLO_AMT"));

        // 평가 금액 필드들
        portfolio.put("CASH_PTFLO_EVL_AMT", roundData.get("CASH_PTFLO_EVL_AMT"));
        portfolio.put("DPSIT_PTFLO_EVL_AMT", roundData.get("DPSIT_PTFLO_EVL_AMT"));
        portfolio.put("ISV_PTFLO_EVL_AMT", roundData.get("ISV_PTFLO_EVL_AMT"));
        portfolio.put("ANNTY_PTFLO_EVL_AMT", roundData.get("ANNTY_PTFLO_EVL_AMT"));
        portfolio.put("STOCK_PTFLO_EVL_AMT", roundData.get("STOCK_PTFLO_EVL_AMT"));
        portfolio.put("FUND_PTFLO_EVL_AMT", roundData.get("FUND_PTFLO_EVL_AMT"));
        portfolio.put("BOND_PTFLO_EVL_AMT", roundData.get("BOND_PTFLO_EVL_AMT"));

        // 포트폴리오 구성 내역 (JSON 문자열)
        portfolio.put("PTFLO_CPST_CTNS", roundData.get("PTFLO_CPST_CTNS"));

        return portfolio;
    }

    /**
     * 학습 정보 파싱 (영상 시청, 퀴즈 정답 여부)
     */
    private Map<String, Object> parseLearningStatus(List<Map<String, Object>> learningList) {
        Map<String, Object> status = new HashMap<>();

        for (Map<String, Object> learning : learningList) {
            Integer fnprdNo = extractInt(learning, "FNPRD_NO");
            String productName = PRODUCT_NAMES.get(fnprdNo);

            if (productName != null) {
                // 영상 시청 완료 여부
                Object mfileFinshYn = learning.get("MFILE_FNSH_YN");
                status.put(productName + "VideoCompleted", "Y".equals(mfileFinshYn));

                // 퀴즈 정답 여부
                Object cranYn = learning.get("CRAN_YN");
                status.put(productName + "QuizPassed", "Y".equals(cranYn));
            }
        }

        return status;
    }

    /**
     * 결과에서 리스트 추출
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractList(HashMap<String, Object> result, String key) {
        if (result == null) return null;
        Object data = result.get(key);
        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        }
        // Mock 모드에서는 data 키 없이 바로 리스트가 올 수 있음
        if (result.get("mock") != null && result.get("data") instanceof List) {
            return (List<Map<String, Object>>) result.get("data");
        }
        return null;
    }

    /**
     * Long 값 추출
     */
    private Long extractLong(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Integer 값 추출
     */
    private Integer extractInt(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ========================================
    // 게임 저장 메서드
    // ========================================

    /**
     * 게임 데이터 저장 (Redis + DB 동기화 옵션)
     */
    public Map<String, Object> saveGame(Long mbrSno, GameMode gameMode, GameSessionDto sessionData) {
        log.info("■ GameLoadService.saveGame - mbrSno: {}, gameMode: {}", mbrSno, gameMode);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. GameSessionService로 Redis 저장
            gameSessionService.updateSession(String.valueOf(mbrSno), gameMode, sessionData);
            log.info("■ Game session saved to Redis via GameSessionService");

            // 2. 추가 데이터도 Redis에 캐싱
            String redisKey = String.format(REDIS_KEY_GAME_DATA, mbrSno, gameMode.getCode());
            Map<String, Object> saveData = new HashMap<>();
            saveData.put("mbrSno", mbrSno);
            saveData.put("gameMode", gameMode.getCode());
            saveData.put("currentRound", sessionData.getCurrentRound());
            saveData.put("completed", sessionData.getCompleted());
            saveData.put("updatedAt", sessionData.getUpdatedAt());

            if (sessionData.getPortfolio() != null) {
                saveData.put("portfolio", sessionData.getPortfolio());
            }

            cacheService.setObject(redisKey, saveData, GAME_DATA_TTL, TimeUnit.SECONDS);

            result.put("success", true);
            result.put("message", "게임이 저장되었습니다.");

        } catch (Exception e) {
            log.error("■ Error saving game: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "게임 저장 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 라운드 진행 정보 저장 (Redis 업데이트 + DB 영구 저장)
     */
    public Map<String, Object> saveRoundProgress(Long mbrSno, GameMode gameMode,
                                                  Long gameId, Integer roundNo,
                                                  Map<String, Object> portfolioData,
                                                  String logCtns, String actiTime) {
        log.info("■ GameLoadService.saveRoundProgress - mbrSno: {}, gameMode: {}, round: {}",
            mbrSno, gameMode, roundNo);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. DB에 라운드 정보 저장
            if (gameMode == GameMode.TUTORIAL) {
                HashMap<String, Object> dbResult = transactionService.createTutorialRound(
                    gameId, roundNo, mbrSno, null, portfolioData
                );
                result.put("dbResult", dbResult);
            } else if (gameMode == GameMode.COMPETITION) {
                HashMap<String, Object> dbResult = transactionService.createCompetitionRound(
                    gameId, roundNo, mbrSno, null, portfolioData
                );
                result.put("dbResult", dbResult);
            }

            // 2. Redis 세션 업데이트
            GameSessionDto session = gameSessionService.getSession(String.valueOf(mbrSno), gameMode);
            if (session != null) {
                session.setCurrentRound(roundNo);
                session.setUpdatedAt(LocalDateTime.now());
                gameSessionService.updateSession(String.valueOf(mbrSno), gameMode, session);
            }

            result.put("success", true);
            result.put("message", "라운드 진행 정보가 저장되었습니다.");

        } catch (KinfaRunException e) {
            log.error("■ Error saving round progress: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "라운드 저장 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 게임 결과 저장 (DB 영구 저장 + Redis 캐시 삭제)
     */
    public Map<String, Object> saveGameResult(Long mbrSno, GameMode gameMode,
                                               Long gameId, Long score,
                                               Long fnnrMngScr, Long riskMngScr, Long abslYildScr) {
        log.info("■ GameLoadService.saveGameResult - mbrSno: {}, gameMode: {}, score: {}",
            mbrSno, gameMode, score);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. DB에 결과 저장
            if (gameMode == GameMode.TUTORIAL) {
                HashMap<String, Object> dbResult = transactionService.saveTutorialResult(
                    mbrSno, gameId, score
                );
                result.put("dbResult", dbResult);
            } else if (gameMode == GameMode.COMPETITION) {
                HashMap<String, Object> dbResult = transactionService.saveCompetitionResult(
                    mbrSno, gameId, score, fnnrMngScr, riskMngScr, abslYildScr
                );
                result.put("dbResult", dbResult);
            }

            // 2. Redis 세션 완료 표시 후 삭제
            GameSessionDto session = gameSessionService.getSession(String.valueOf(mbrSno), gameMode);
            if (session != null) {
                session.setCompleted(true);
                session.setUpdatedAt(LocalDateTime.now());
                gameSessionService.updateSession(String.valueOf(mbrSno), gameMode, session);
            }

            // 3. Redis 캐시 삭제 (게임 완료)
            clearGameCache(mbrSno, gameMode);

            result.put("success", true);
            result.put("message", "게임 결과가 저장되었습니다.");

        } catch (KinfaRunException e) {
            log.error("■ Error saving game result: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "결과 저장 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    // ========================================
    // 게임 시작 메서드
    // ========================================

    /**
     * 튜토리얼 시작 (DB 저장 + Redis 세션 생성)
     */
    public Map<String, Object> startTutorial(Long mbrSno, String fipatNm, Integer rndmNo,
                                              Long monthlyIncome, Long fixedExpense,
                                              Integer npcNo, String propensityLog) {
        log.info("■ GameLoadService.startTutorial - mbrSno: {}", mbrSno);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. DB에 튜토리얼 생성
            HashMap<String, Object> dbResult = transactionService.createTutorial(
                mbrSno, fipatNm, rndmNo, monthlyIncome, fixedExpense, npcNo, propensityLog
            );

            // 2. Redis에 게임 세션 생성
            GameSessionDto session = GameSessionDto.builder()
                .uid(String.valueOf(mbrSno))
                .gameMode(GameMode.TUTORIAL)
                .currentRound(1)
                .startedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .completed(false)
                .propensityType(fipatNm)
                .npcType(npcNo == 1 ? "POYONGI" : "CHAEUMI")
                .monthlySalary(monthlyIncome)
                .monthlyLiving(fixedExpense)
                .adviceUsedCount(0)
                .insuranceSubscribed(false)
                .loanUsed(false)
                .illegalLoanUsed(false)
                .insurableEventOccurred(false)
                .openingStoryCompleted(true)
                .propensityTestCompleted(true)
                .resultAnalysisCompleted(true)
                .npcAssignmentCompleted(true)
                .depositVideoCompleted(false)
                .stockVideoCompleted(false)
                .bondVideoCompleted(false)
                .pensionVideoCompleted(false)
                .fundVideoCompleted(false)
                .insuranceVideoCompleted(false)
                .depositQuizPassed(false)
                .stockQuizPassed(false)
                .bondQuizPassed(false)
                .pensionQuizPassed(false)
                .fundQuizPassed(false)
                .insuranceQuizPassed(false)
                .achievedAchievements(new HashSet<>())
                .achievementProgress(new HashMap<>())
                .build();

            gameSessionService.updateSession(String.valueOf(mbrSno), GameMode.TUTORIAL, session);

            result.put("success", true);
            result.put("message", "튜토리얼이 시작되었습니다.");
            result.put("dbResult", dbResult);
            result.put("session", session);

        } catch (KinfaRunException e) {
            log.error("■ Error starting tutorial: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "튜토리얼 시작 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 경쟁 모드 시작 (DB 저장 + Redis 세션 생성)
     */
    public Map<String, Object> startCompetition(Long mbrSno, String fipatNm,
                                                 Integer rndmNo, Integer npcNo) {
        log.info("■ GameLoadService.startCompetition - mbrSno: {}", mbrSno);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. DB에 경쟁 생성
            HashMap<String, Object> dbResult = transactionService.createCompetition(
                mbrSno, fipatNm, rndmNo, npcNo
            );

            // 2. Redis에 게임 세션 생성
            GameSessionDto session = GameSessionDto.builder()
                .uid(String.valueOf(mbrSno))
                .gameMode(GameMode.COMPETITION)
                .currentRound(1)
                .startedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .completed(false)
                .propensityType(fipatNm)
                .npcType(npcNo == 1 ? "POYONGI" : "CHAEUMI")
                .adviceUsedCount(0)
                .insuranceSubscribed(false)
                .loanUsed(false)
                .illegalLoanUsed(false)
                .insurableEventOccurred(false)
                .npcSelectionCompleted(true)
                .depositVideoCompleted(false)
                .stockVideoCompleted(false)
                .bondVideoCompleted(false)
                .pensionVideoCompleted(false)
                .fundVideoCompleted(false)
                .insuranceVideoCompleted(false)
                .depositQuizPassed(false)
                .stockQuizPassed(false)
                .bondQuizPassed(false)
                .pensionQuizPassed(false)
                .fundQuizPassed(false)
                .insuranceQuizPassed(false)
                .achievedAchievements(new HashSet<>())
                .achievementProgress(new HashMap<>())
                .build();

            gameSessionService.updateSession(String.valueOf(mbrSno), GameMode.COMPETITION, session);

            result.put("success", true);
            result.put("message", "경쟁 모드가 시작되었습니다.");
            result.put("dbResult", dbResult);
            result.put("session", session);

        } catch (KinfaRunException e) {
            log.error("■ Error starting competition: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "경쟁 모드 시작 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    // ========================================
    // 캐시 관리 메서드
    // ========================================

    /**
     * 게임 캐시 초기화
     */
    public void clearGameCache(Long mbrSno, GameMode gameMode) {
        // Redis 데이터 키 삭제
        String redisKey = String.format(REDIS_KEY_GAME_DATA, mbrSno, gameMode.getCode());
        cacheService.delete(redisKey);

        // GameSessionService 세션 삭제
        gameSessionService.deleteSession(String.valueOf(mbrSno), gameMode);

        log.info("■ Game cache cleared: mbrSno={}, gameMode={}", mbrSno, gameMode);
    }

    /**
     * 모든 게임 캐시 초기화
     */
    public void clearAllGameCache(Long mbrSno) {
        clearGameCache(mbrSno, GameMode.TUTORIAL);
        clearGameCache(mbrSno, GameMode.COMPETITION);

        String tutorialKey = String.format(REDIS_KEY_TUTORIAL_PROGRESS, mbrSno);
        String competitionKey = String.format(REDIS_KEY_COMPETITION_PROGRESS, mbrSno);
        cacheService.delete(tutorialKey);
        cacheService.delete(competitionKey);

        log.info("■ All game cache cleared for mbrSno: {}", mbrSno);
    }

    /**
     * 현재 게임 세션 조회 (Redis에서)
     */
    public GameSessionDto getCurrentSession(Long mbrSno, GameMode gameMode) {
        return gameSessionService.getSession(String.valueOf(mbrSno), gameMode);
    }

    /**
     * 게임 세션 존재 여부 확인
     */
    public boolean hasActiveSession(Long mbrSno, GameMode gameMode) {
        return gameSessionService.existsSession(String.valueOf(mbrSno), gameMode);
    }

    // ========================================
    // 업적 및 학습 정보 저장
    // ========================================

    /**
     * 업적 달성 저장 (Redis + DB 영구 저장)
     * 
     * @param mbrSno 회원 일련번호
     * @param achievementId 달성한 업적 ID (1~20)
     * @param gameMode 게임 모드
     * @return 저장 결과
     */
    public Map<String, Object> saveAchievement(Long mbrSno, Integer achievementId, GameMode gameMode) {
        log.info("■ GameLoadService.saveAchievement - mbrSno: {}, achievementId: {}", mbrSno, achievementId);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Redis 세션 업데이트
            GameSessionDto session = gameSessionService.getSession(String.valueOf(mbrSno), gameMode);
            if (session != null && session.getAchievedAchievements() != null) {
                session.getAchievedAchievements().add(achievementId);
                gameSessionService.updateSession(String.valueOf(mbrSno), gameMode, session);
                log.info("■ Achievement added to Redis session: {}", achievementId);
            }

            // 2. DB에 영구 저장 (ACHM_NO 비트마스크 업데이트)
            // ACHM_NO는 비트마스크로 여러 업적을 저장 (예: 1번+3번 업적 = 0b101 = 5)
            Long achmNo = calculateAchievementBitmask(session);
            HashMap<String, Object> dbResult = transactionService.updateAchievement(mbrSno, achmNo);

            result.put("success", true);
            result.put("achievementId", achievementId);
            result.put("totalAchievements", achmNo);
            result.put("dbResult", dbResult);
            result.put("message", "업적이 저장되었습니다.");

        } catch (KinfaRunException e) {
            log.error("■ Error saving achievement: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "업적 저장 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 업적 비트마스크 계산
     * 업적 ID 1~20을 비트마스크로 변환
     * 예: 업적 1, 3, 5 달성 = 2^0 + 2^2 + 2^4 = 1 + 4 + 16 = 21
     */
    private Long calculateAchievementBitmask(GameSessionDto session) {
        if (session == null || session.getAchievedAchievements() == null) {
            return 0L;
        }

        long bitmask = 0L;
        for (Integer achievementId : session.getAchievedAchievements()) {
            if (achievementId >= 1 && achievementId <= 20) {
                bitmask |= (1L << (achievementId - 1));
            }
        }
        return bitmask;
    }

    /**
     * 학습 정보 저장 (영상 시청, 퀴즈 응답 - DB 영구 저장)
     * 
     * @param mbrSno 회원 일련번호
     * @param gameId 게임 ID (튜토리얼/경쟁 SNO)
     * @param gameMode 게임 모드
     * @param productNo 금융상품 번호 (1:예적금, 2:주식, 3:채권, 4:연금, 5:펀드, 6:보험)
     * @param videoCompleted 영상 시청 완료 여부
     * @param quizNo 퀴즈 번호
     * @param answerNo 선택한 답변 번호
     * @param isCorrect 정답 여부
     * @param activityTime 활동 시간 (초)
     * @return 저장 결과
     */
    public Map<String, Object> saveLearningInfo(Long mbrSno, Long gameId, GameMode gameMode,
                                                 Integer productNo, Boolean videoCompleted,
                                                 Integer quizNo, Integer answerNo, Boolean isCorrect,
                                                 String activityTime) {
        log.info("■ GameLoadService.saveLearningInfo - mbrSno: {}, productNo: {}, quizNo: {}", 
            mbrSno, productNo, quizNo);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Redis 세션 업데이트 (영상/퀴즈 상태)
            GameSessionDto session = gameSessionService.getSession(String.valueOf(mbrSno), gameMode);
            if (session != null) {
                updateLearningStatusInSession(session, productNo, videoCompleted, isCorrect);
                gameSessionService.updateSession(String.valueOf(mbrSno), gameMode, session);
                log.info("■ Learning status updated in Redis session");
            }

            // 2. DB에 영구 저장
            Long ttrlSno = (gameMode == GameMode.TUTORIAL) ? gameId : null;
            Long cmpttSno = (gameMode == GameMode.COMPETITION) ? gameId : null;
            Integer mfileFinshYnInt = Boolean.TRUE.equals(videoCompleted) ? 1 : 0;
            Integer cranYnInt = Boolean.TRUE.equals(isCorrect) ? 1 : 0;

            HashMap<String, Object> dbResult = transactionService.saveLearningInfo(
                mbrSno, ttrlSno, cmpttSno, productNo, mfileFinshYnInt,
                String.valueOf(quizNo), String.valueOf(answerNo), cranYnInt, activityTime
            );

            result.put("success", true);
            result.put("productNo", productNo);
            result.put("videoCompleted", videoCompleted);
            result.put("isCorrect", isCorrect);
            result.put("dbResult", dbResult);
            result.put("message", "학습 정보가 저장되었습니다.");

        } catch (KinfaRunException e) {
            log.error("■ Error saving learning info: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "학습 정보 저장 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * Redis 세션의 학습 상태 업데이트
     */
    private void updateLearningStatusInSession(GameSessionDto session, Integer productNo,
                                                Boolean videoCompleted, Boolean quizPassed) {
        if (session == null || productNo == null) return;

        switch (productNo) {
            case 1: // 예적금
                if (videoCompleted != null) session.setDepositVideoCompleted(videoCompleted);
                if (quizPassed != null) session.setDepositQuizPassed(quizPassed);
                break;
            case 2: // 주식
                if (videoCompleted != null) session.setStockVideoCompleted(videoCompleted);
                if (quizPassed != null) session.setStockQuizPassed(quizPassed);
                break;
            case 3: // 채권
                if (videoCompleted != null) session.setBondVideoCompleted(videoCompleted);
                if (quizPassed != null) session.setBondQuizPassed(quizPassed);
                break;
            case 4: // 연금
                if (videoCompleted != null) session.setPensionVideoCompleted(videoCompleted);
                if (quizPassed != null) session.setPensionQuizPassed(quizPassed);
                break;
            case 5: // 펀드
                if (videoCompleted != null) session.setFundVideoCompleted(videoCompleted);
                if (quizPassed != null) session.setFundQuizPassed(quizPassed);
                break;
            case 6: // 보험
                if (videoCompleted != null) session.setInsuranceVideoCompleted(videoCompleted);
                if (quizPassed != null) session.setInsuranceQuizPassed(quizPassed);
                break;
            default:
                log.warn("■ Unknown product number: {}", productNo);
        }
    }
}
