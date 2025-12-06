package com.cas.api.service.game;

import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.enums.GameMode;
import com.cas.api.service.external.TransactionService;
import com.cas.api.service.user.UserService;
import com.cas.common.core.util.KinfaRunException;
import com.cas.common.infra.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 게임 데이터 로드/저장 Service
 * - Redis에서 게임 상태 로드/저장
 * - DB와 동기화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameLoadService {

    private final TransactionService transactionService;
    private final GameSessionService gameSessionService;
    private final UserService userService;
    private final CacheService cacheService;

    // Redis 키 패턴
    private static final String REDIS_KEY_GAME_DATA = "game:data:%s:%s"; // mbrSno:gameMode
    private static final String REDIS_KEY_TUTORIAL_PROGRESS = "game:tutorial:%s";
    private static final String REDIS_KEY_COMPETITION_PROGRESS = "game:competition:%s";
    private static final int GAME_DATA_TTL = 86400; // 24시간

    /**
     * 게임 시작 시 데이터 로드
     * 1. Redis 캐시 확인
     * 2. 없으면 DB에서 조회
     * 3. DB에도 없으면 새로 생성
     */
    public Map<String, Object> loadGame(Long mbrSno, GameMode gameMode) {
        log.info("■ GameLoadService.loadGame - mbrSno: {}, gameMode: {}", mbrSno, gameMode);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Redis에서 먼저 확인
            String redisKey = String.format(REDIS_KEY_GAME_DATA, mbrSno, gameMode.getCode());
            Map<String, Object> cachedData = cacheService.getObject(redisKey, Map.class);

            if (cachedData != null) {
                log.info("■ Game data loaded from Redis: {}", redisKey);
                result.put("success", true);
                result.put("source", "redis");
                result.put("data", cachedData);
                return result;
            }

            // 2. DB에서 조회
            Map<String, Object> dbData = loadFromDb(mbrSno, gameMode);

            if (dbData != null && !dbData.isEmpty()) {
                log.info("■ Game data loaded from DB");

                // Redis에 캐싱
                cacheService.setObject(redisKey, dbData, GAME_DATA_TTL, TimeUnit.SECONDS);

                result.put("success", true);
                result.put("source", "db");
                result.put("data", dbData);
                return result;
            }

            // 3. 데이터가 없으면 새 게임
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
     * DB에서 게임 데이터 로드
     */
    private Map<String, Object> loadFromDb(Long mbrSno, GameMode gameMode) throws KinfaRunException {
        Map<String, Object> gameData = new HashMap<>();

        if (gameMode == GameMode.TUTORIAL) {
            // 튜토리얼 데이터 로드
            HashMap<String, Object> tutorialList = transactionService.getTutorialListByUser(mbrSno);
            if (tutorialList != null && !tutorialList.isEmpty()) {
                gameData.put("tutorials", tutorialList);

                // 최근 튜토리얼의 진행 정보 로드
                // (실제로는 튜토리얼 목록에서 ID를 가져와서 조회)
            }
        } else if (gameMode == GameMode.COMPETITION) {
            // 경쟁 모드 데이터 로드
            HashMap<String, Object> competitionList = transactionService.getCompetitionListByUser(mbrSno);
            if (competitionList != null && !competitionList.isEmpty()) {
                gameData.put("competitions", competitionList);
            }
        }

        // 사용자 기본 정보 로드
        HashMap<String, Object> userInfo = transactionService.getGameBasicInfo(mbrSno);
        if (userInfo != null) {
            gameData.put("user", userInfo);
        }

        return gameData.isEmpty() ? null : gameData;
    }

    /**
     * 게임 데이터 저장 (Redis + DB)
     */
    public Map<String, Object> saveGame(Long mbrSno, GameMode gameMode, GameSessionDto sessionData) {
        log.info("■ GameLoadService.saveGame - mbrSno: {}, gameMode: {}", mbrSno, gameMode);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Redis에 저장
            String redisKey = String.format(REDIS_KEY_GAME_DATA, mbrSno, gameMode.getCode());
            
            Map<String, Object> saveData = new HashMap<>();
            saveData.put("mbrSno", mbrSno);
            saveData.put("gameMode", gameMode.getCode());
            saveData.put("currentRound", sessionData.getCurrentRound());
            saveData.put("completed", sessionData.getCompleted());
            saveData.put("updatedAt", sessionData.getUpdatedAt());
            
            // 포트폴리오 정보
            if (sessionData.getPortfolio() != null) {
                saveData.put("portfolio", sessionData.getPortfolio());
            }

            cacheService.setObject(redisKey, saveData, GAME_DATA_TTL, TimeUnit.SECONDS);
            log.info("■ Game data saved to Redis: {}", redisKey);

            // 2. GameSessionService 업데이트
            gameSessionService.updateSession(String.valueOf(mbrSno), gameMode, sessionData);

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
     * 라운드 진행 정보 저장 (DB에 영구 저장)
     */
    public Map<String, Object> saveRoundProgress(Long mbrSno, GameMode gameMode, 
                                                  Long gameId, Integer roundNo,
                                                  Map<String, Object> portfolioData,
                                                  String logCtns, String actiTime) {
        log.info("■ GameLoadService.saveRoundProgress - mbrSno: {}, gameMode: {}, round: {}", 
            mbrSno, gameMode, roundNo);

        Map<String, Object> result = new HashMap<>();

        try {
            if (gameMode == GameMode.TUTORIAL) {
                // 튜토리얼 라운드 저장
                HashMap<String, Object> dbResult = transactionService.createTutorialRound(
                    gameId, roundNo, mbrSno, null, portfolioData
                );
                result.put("dbResult", dbResult);
            } else if (gameMode == GameMode.COMPETITION) {
                // 경쟁 모드 라운드 저장
                HashMap<String, Object> dbResult = transactionService.createCompetitionRound(
                    gameId, roundNo, mbrSno, null, portfolioData
                );
                result.put("dbResult", dbResult);
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
     * 게임 결과 저장
     */
    public Map<String, Object> saveGameResult(Long mbrSno, GameMode gameMode, 
                                               Long gameId, Long score,
                                               Long fnnrMngScr, Long riskMngScr, Long abslYildScr) {
        log.info("■ GameLoadService.saveGameResult - mbrSno: {}, gameMode: {}, score: {}", 
            mbrSno, gameMode, score);

        Map<String, Object> result = new HashMap<>();

        try {
            if (gameMode == GameMode.TUTORIAL) {
                // 튜토리얼 결과 저장
                HashMap<String, Object> dbResult = transactionService.saveTutorialResult(
                    mbrSno, gameId, score
                );
                result.put("dbResult", dbResult);
            } else if (gameMode == GameMode.COMPETITION) {
                // 경쟁 모드 결과 저장
                HashMap<String, Object> dbResult = transactionService.saveCompetitionResult(
                    mbrSno, gameId, score, fnnrMngScr, riskMngScr, abslYildScr
                );
                result.put("dbResult", dbResult);
            }

            // Redis 캐시 삭제 (게임 완료)
            String redisKey = String.format(REDIS_KEY_GAME_DATA, mbrSno, gameMode.getCode());
            cacheService.delete(redisKey);

            result.put("success", true);
            result.put("message", "게임 결과가 저장되었습니다.");

        } catch (KinfaRunException e) {
            log.error("■ Error saving game result: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "결과 저장 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 튜토리얼 시작
     */
    public Map<String, Object> startTutorial(Long mbrSno, String fipatNm, Integer rndmNo,
                                              Long monthlyIncome, Long fixedExpense,
                                              Integer npcNo, String propensityLog) {
        log.info("■ GameLoadService.startTutorial - mbrSno: {}", mbrSno);

        Map<String, Object> result = new HashMap<>();

        try {
            HashMap<String, Object> dbResult = transactionService.createTutorial(
                mbrSno, fipatNm, rndmNo, monthlyIncome, fixedExpense, npcNo, propensityLog
            );

            // Redis에 진행 상태 저장
            String redisKey = String.format(REDIS_KEY_TUTORIAL_PROGRESS, mbrSno);
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("mbrSno", mbrSno);
            progressData.put("npcNo", npcNo);
            progressData.put("currentRound", 1);
            progressData.put("started", true);

            cacheService.setObject(redisKey, progressData, GAME_DATA_TTL, TimeUnit.SECONDS);

            result.put("success", true);
            result.put("message", "튜토리얼이 시작되었습니다.");
            result.put("dbResult", dbResult);

        } catch (KinfaRunException e) {
            log.error("■ Error starting tutorial: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "튜토리얼 시작 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 경쟁 모드 시작
     */
    public Map<String, Object> startCompetition(Long mbrSno, String fipatNm, 
                                                 Integer rndmNo, Integer npcNo) {
        log.info("■ GameLoadService.startCompetition - mbrSno: {}", mbrSno);

        Map<String, Object> result = new HashMap<>();

        try {
            HashMap<String, Object> dbResult = transactionService.createCompetition(
                mbrSno, fipatNm, rndmNo, npcNo
            );

            // Redis에 진행 상태 저장
            String redisKey = String.format(REDIS_KEY_COMPETITION_PROGRESS, mbrSno);
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("mbrSno", mbrSno);
            progressData.put("npcNo", npcNo);
            progressData.put("currentRound", 1);
            progressData.put("started", true);

            cacheService.setObject(redisKey, progressData, GAME_DATA_TTL, TimeUnit.SECONDS);

            result.put("success", true);
            result.put("message", "경쟁 모드가 시작되었습니다.");
            result.put("dbResult", dbResult);

        } catch (KinfaRunException e) {
            log.error("■ Error starting competition: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "경쟁 모드 시작 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 게임 캐시 초기화
     */
    public void clearGameCache(Long mbrSno, GameMode gameMode) {
        String redisKey = String.format(REDIS_KEY_GAME_DATA, mbrSno, gameMode.getCode());
        cacheService.delete(redisKey);
        log.info("■ Game cache cleared: {}", redisKey);
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
}

