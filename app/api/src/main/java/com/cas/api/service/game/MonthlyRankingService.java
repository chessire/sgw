package com.cas.api.service.game;

import com.cas.api.service.external.TransactionService;
import com.cas.common.core.util.KinfaRunException;
import com.cas.common.infra.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 월간 랭킹 서비스
 * - Redis 캐싱 기반 월간 랭킹 조회
 * - properties에 정의된 시간에 자동 갱신 (예: 매월 1일 10:00)
 * 
 * 설정 예시:
 * monthly-ranking.refresh.enabled=true
 * monthly-ranking.refresh.schedule=1 10:00  # 매월 1일 10시 0분
 * monthly-ranking.cache.size=100
 * monthly-ranking.cache.ttl=86400
 */
@Slf4j
@Service
public class MonthlyRankingService implements InitializingBean {

    private final TransactionService transactionService;
    private final CacheService cacheService;

    public MonthlyRankingService(TransactionService transactionService, CacheService cacheService) {
        this.transactionService = transactionService;
        this.cacheService = cacheService;
    }

    // Redis 키 패턴 (월별)
    private static final String REDIS_KEY_MONTHLY_RANKING = "ranking:monthly:%s";  // YYYYMM
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Properties 설정
    @Value("${monthly-ranking.refresh.enabled:true}")
    private boolean refreshEnabled;

    /**
     * 월간 랭킹 갱신 스케줄
     * 형식: "일 시:분" (예: "1 10:00" = 매월 1일 10시 0분)
     */
    @Value("${monthly-ranking.refresh.schedule:1 10:00}")
    private String refreshSchedule;

    @Value("${monthly-ranking.cache.size:100}")
    private int cacheSize;

    @Value("${monthly-ranking.cache.ttl:86400}")  // 기본 24시간
    private int cacheTtl;

    // 파싱된 갱신 스케줄
    private int refreshDay;
    private String refreshTime;

    @Override
    public void afterPropertiesSet() {
        // 스케줄 파싱 (형식: "1 10:00")
        parseSchedule();
        
        log.info("■ MonthlyRankingService initialized - enabled: {}, schedule: {} {}, cacheSize: {}, ttl: {}s",
            refreshEnabled, refreshDay, refreshTime, cacheSize, cacheTtl);

        // 서비스 시작 시 현재 월 랭킹 캐싱
        if (refreshEnabled) {
            refreshMonthlyRankingCache();
        }
    }

    /**
     * 스케줄 문자열 파싱
     * 형식: "일 시:분" (예: "1 10:00", "15 09:00")
     */
    private void parseSchedule() {
        try {
            String[] parts = refreshSchedule.trim().split(" ");
            if (parts.length == 2) {
                refreshDay = Integer.parseInt(parts[0]);
                refreshTime = parts[1];
            } else {
                // 기본값
                refreshDay = 1;
                refreshTime = "10:00";
            }
        } catch (Exception e) {
            log.warn("■ Failed to parse monthly-ranking.refresh.schedule: {}, using default", refreshSchedule);
            refreshDay = 1;
            refreshTime = "10:00";
        }
    }

    /**
     * 매 분마다 실행하여 설정된 시간인지 확인
     * 설정된 날짜/시간이면 이전 달 월간 랭킹 캐시 갱신
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    public void checkAndRefreshMonthlyRanking() {
        if (!refreshEnabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int currentDay = now.getDayOfMonth();
        String currentTime = now.format(TIME_FORMATTER);

        // 설정된 날짜와 시간인지 확인
        if (currentDay == refreshDay && currentTime.equals(refreshTime)) {
            log.info("■ Scheduled monthly ranking refresh triggered at day {} time {}", currentDay, currentTime);
            
            // 이전 달 랭킹 갱신 (매월 1일에 이전 달 랭킹 확정)
            LocalDate previousMonth = now.toLocalDate().minusMonths(1);
            String prevYearMonth = previousMonth.format(YEAR_MONTH_FORMATTER);
            
            refreshMonthlyRankingCacheInternal(prevYearMonth);
            
            // 현재 달 랭킹도 갱신 (진행 중인 랭킹)
            String currentYearMonth = now.format(YEAR_MONTH_FORMATTER);
            refreshMonthlyRankingCacheInternal(currentYearMonth);
        }
    }

    /**
     * 월간 랭킹 조회 (Redis 캐시 우선) - 현재 월 전용
     *
     * @param limit 조회할 순위 개수
     * @return 랭킹 데이터
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMonthlyRanking(Integer limit) {
        // 항상 현재 월 사용
        String yearMonth = LocalDate.now().format(YEAR_MONTH_FORMATTER);
        
        log.debug("■ MonthlyRankingService.getMonthlyRanking - yearMonth: {}, limit: {}", yearMonth, limit);

        Map<String, Object> result = new HashMap<>();
        String redisKey = String.format(REDIS_KEY_MONTHLY_RANKING, yearMonth);

        try {
            // 1. Redis에서 먼저 조회
            List<Map<String, Object>> cachedRanking = cacheService.getObject(redisKey, List.class);

            if (cachedRanking != null && !cachedRanking.isEmpty()) {
                log.debug("■ Monthly ranking loaded from Redis cache: {}", yearMonth);

                // 요청한 개수만큼만 반환
                List<Map<String, Object>> limitedRanking = cachedRanking.stream()
                    .limit(limit != null ? limit : cacheSize)
                    .toList();

                result.put("success", true);
                result.put("source", "redis");
                result.put("yearMonth", yearMonth);
                result.put("data", limitedRanking);
                result.put("totalCount", cachedRanking.size());
                return result;
            }

            // 2. 캐시 없으면 DB에서 조회 후 캐싱
            log.info("■ Monthly ranking cache miss for {}, fetching from DB", yearMonth);
            return fetchAndCacheMonthlyRanking(yearMonth, limit);

        } catch (Exception e) {
            log.error("■ Error getting monthly ranking: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "월간 랭킹 조회 중 오류가 발생했습니다: " + e.getMessage());
            return result;
        }
    }

    /**
     * DB에서 월간 랭킹 조회 후 Redis에 캐싱
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchAndCacheMonthlyRanking(String yearMonth, Integer limit) {
        Map<String, Object> result = new HashMap<>();
        String redisKey = String.format(REDIS_KEY_MONTHLY_RANKING, yearMonth);

        try {
            HashMap<String, Object> dbResult = transactionService.getMonthlyRanking(yearMonth, cacheSize);

            // Mock 모드 또는 실제 DB 응답에서 데이터 추출
            List<Map<String, Object>> rankingList = null;
            
            if (dbResult.get("data") instanceof List) {
                rankingList = (List<Map<String, Object>>) dbResult.get("data");
            } else if (dbResult.get("mock") != null) {
                // Mock 모드: 빈 리스트 또는 테스트 데이터 생성
                rankingList = generateMockMonthlyRanking(yearMonth);
            }

            if (rankingList == null) {
                rankingList = new ArrayList<>();
            }

            // 순위 번호 추가
            for (int i = 0; i < rankingList.size(); i++) {
                rankingList.get(i).put("rank", i + 1);
            }

            // Redis에 캐싱
            if (!rankingList.isEmpty()) {
                cacheService.setObject(redisKey, rankingList, cacheTtl, TimeUnit.SECONDS);
                log.info("■ Monthly ranking cached: yearMonth={}, count={}", yearMonth, rankingList.size());
            }

            int effectiveLimit = (limit != null) ? limit : cacheSize;
            List<Map<String, Object>> limitedRanking = rankingList.stream()
                .limit(effectiveLimit)
                .toList();

            result.put("success", true);
            result.put("source", "db");
            result.put("yearMonth", yearMonth);
            result.put("data", limitedRanking);
            result.put("totalCount", rankingList.size());
            return result;

        } catch (KinfaRunException e) {
            log.error("■ Error fetching monthly ranking from DB: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "DB에서 월간 랭킹을 조회하는 중 오류가 발생했습니다.");
            return result;
        }
    }

    /**
     * 월간 랭킹 캐시 강제 갱신 (현재 월)
     */
    public void refreshMonthlyRankingCache() {
        String yearMonth = LocalDate.now().format(YEAR_MONTH_FORMATTER);
        log.info("■ Refreshing monthly ranking cache: {}", yearMonth);
        String redisKey = String.format(REDIS_KEY_MONTHLY_RANKING, yearMonth);
        cacheService.delete(redisKey);
        fetchAndCacheMonthlyRanking(yearMonth, cacheSize);
    }

    /**
     * 월간 랭킹 캐시 강제 갱신 (특정 월 - 스케줄러 내부용)
     */
    private void refreshMonthlyRankingCacheInternal(String yearMonth) {
        log.info("■ Refreshing monthly ranking cache (internal): {}", yearMonth);
        String redisKey = String.format(REDIS_KEY_MONTHLY_RANKING, yearMonth);
        cacheService.delete(redisKey);
        fetchAndCacheMonthlyRanking(yearMonth, cacheSize);
    }

    /**
     * 사용자의 월간 순위 조회 (현재 월)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMyMonthlyRanking(Long mbrSno) {
        String yearMonth = LocalDate.now().format(YEAR_MONTH_FORMATTER);
        
        log.debug("■ MonthlyRankingService.getMyMonthlyRanking - mbrSno: {}, yearMonth: {}", mbrSno, yearMonth);

        Map<String, Object> result = new HashMap<>();

        try {
            // 먼저 전체 월간 랭킹 조회
            Map<String, Object> rankingResult = getMonthlyRanking(cacheSize);
            
            if (!(Boolean) rankingResult.getOrDefault("success", false)) {
                return rankingResult;
            }

            List<Map<String, Object>> rankings = (List<Map<String, Object>>) rankingResult.get("data");
            
            // 내 순위 찾기
            Optional<Map<String, Object>> myRank = rankings.stream()
                .filter(entry -> {
                    Object mbrSnoObj = entry.get("mbrSno");
                    if (mbrSnoObj instanceof Number) {
                        return ((Number) mbrSnoObj).longValue() == mbrSno;
                    }
                    return false;
                })
                .findFirst();

            if (myRank.isPresent()) {
                result.put("success", true);
                result.put("yearMonth", yearMonth);
                result.put("data", myRank.get());
            } else {
                // 랭킹에 없는 경우 DB에서 직접 조회
                try {
                    HashMap<String, Object> dbResult = transactionService.getMyMonthlyRanking(mbrSno, yearMonth);
                    result.put("success", true);
                    result.put("yearMonth", yearMonth);
                    result.put("data", dbResult);
                    result.put("note", "순위권 외");
                } catch (KinfaRunException e) {
                    result.put("success", false);
                    result.put("message", "내 월간 순위를 조회할 수 없습니다.");
                }
            }

            return result;

        } catch (Exception e) {
            log.error("■ Error getting my monthly ranking: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "내 월간 순위 조회 중 오류가 발생했습니다.");
            return result;
        }
    }

    /**
     * 갱신 스케줄 정보 조회
     */
    public Map<String, Object> getRefreshScheduleInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("enabled", refreshEnabled);
        info.put("day", refreshDay);
        info.put("time", refreshTime);
        info.put("schedule", refreshSchedule);
        info.put("cacheSize", cacheSize);
        info.put("cacheTtl", cacheTtl);
        return info;
    }

    /**
     * Mock 월간 랭킹 데이터 생성 (Development 환경용)
     */
    private List<Map<String, Object>> generateMockMonthlyRanking(String yearMonth) {
        List<Map<String, Object>> mockData = new ArrayList<>();
        
        String[][] mockUsers = {
            {"user001", "당당하게나아가는포용이123", "1"},
            {"user002", "용맹한포용이4567", "1"},
            {"user003", "열정적인채우미8901", "2"},
            {"user004", "포근한마음의채우미2345", "2"},
            {"user005", "지혜로운포용이6789", "1"},
            {"user006", "씩씩한채우미1234", "2"},
            {"user007", "활발한포용이5678", "1"},
            {"user008", "차분한채우미9012", "2"},
            {"user009", "명랑한포용이3456", "1"},
            {"user010", "다정한채우미7890", "2"}
        };
        
        double[] scores = {2847.5, 2654.3, 2512.8, 2389.2, 2276.4, 2165.7, 2058.3, 1954.2, 1856.8, 1765.4};
        long[] netWorths = {11780000L, 10850000L, 10240000L, 9685000L, 9120000L, 8654000L, 8245000L, 7890000L, 7512000L, 7185000L};
        String[] returnRates = {"135.6%", "117.0%", "104.8%", "93.7%", "82.4%", "73.1%", "64.9%", "57.8%", "50.2%", "43.7%"};

        for (int i = 0; i < mockUsers.length; i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("rank", i + 1);
            entry.put("uid", mockUsers[i][0]);
            entry.put("mbrSno", 1000L + i);
            entry.put("nickname", mockUsers[i][1]);
            entry.put("npcNo", Integer.parseInt(mockUsers[i][2]));
            entry.put("totalScore", scores[i]);
            entry.put("finalNetWorth", netWorths[i]);
            entry.put("returnRate", returnRates[i]);
            entry.put("yearMonth", yearMonth);
            mockData.add(entry);
        }

        log.info("■ Generated mock monthly ranking for {}: {} entries", yearMonth, mockData.size());
        return mockData;
    }
}

