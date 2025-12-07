package com.cas.api.service.game;

import com.cas.api.service.external.TransactionService;
import com.cas.common.core.util.KinfaRunException;
import com.cas.common.infra.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 랭킹 서비스
 * - Redis 캐싱 기반 랭킹 조회
 * - properties에 정의된 시간에 자동 갱신
 */
@Slf4j
@Service
public class RankingService implements InitializingBean {

    private final TransactionService transactionService;
    private final CacheService cacheService;

    public RankingService(TransactionService transactionService, CacheService cacheService) {
        this.transactionService = transactionService;
        this.cacheService = cacheService;
    }

    // Redis 키
    private static final String REDIS_KEY_RANKING = "ranking:competition";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Properties 설정
    @Value("${ranking.refresh.enabled:true}")
    private boolean refreshEnabled;

    @Value("${ranking.refresh.times:09:00,12:00,18:00,21:00}")
    private String refreshTimesStr;

    @Value("${ranking.cache.size:100}")
    private int cacheSize;

    @Value("${ranking.cache.ttl:300}")
    private int cacheTtl;

    // 파싱된 갱신 시간 리스트
    private Set<String> refreshTimes;

    @Override
    public void afterPropertiesSet() {
        // 갱신 시간 리스트 파싱
        refreshTimes = new HashSet<>();
        if (refreshTimesStr != null && !refreshTimesStr.isEmpty()) {
            String[] times = refreshTimesStr.split(",");
            for (String time : times) {
                refreshTimes.add(time.trim());
            }
        }
        log.info("■ RankingService initialized - enabled: {}, times: {}, cacheSize: {}, ttl: {}s",
            refreshEnabled, refreshTimes, cacheSize, cacheTtl);

        // 서비스 시작 시 초기 캐싱
        if (refreshEnabled) {
            refreshRankingCache();
        }
    }

    /**
     * 매 분마다 실행하여 설정된 시간인지 확인
     * 설정된 시간이면 랭킹 캐시 갱신
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    public void checkAndRefreshRanking() {
        if (!refreshEnabled) {
            return;
        }

        String currentTime = LocalTime.now().format(TIME_FORMATTER);

        if (refreshTimes.contains(currentTime)) {
            log.info("■ Scheduled ranking refresh triggered at {}", currentTime);
            refreshRankingCache();
        }
    }

    /**
     * 랭킹 조회 (Redis 캐시 우선)
     *
     * @param limit 조회할 순위 개수
     * @return 랭킹 데이터
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRanking(Integer limit) {
        log.debug("■ RankingService.getRanking - limit: {}", limit);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Redis에서 먼저 조회
            List<Map<String, Object>> cachedRanking = cacheService.getObject(REDIS_KEY_RANKING, List.class);

            if (cachedRanking != null && !cachedRanking.isEmpty()) {
                log.debug("■ Ranking loaded from Redis cache");

                // 요청한 개수만큼만 반환
                List<Map<String, Object>> limitedRanking = cachedRanking.stream()
                    .limit(limit != null ? limit : cacheSize)
                    .toList();

                result.put("success", true);
                result.put("source", "redis");
                result.put("data", limitedRanking);
                result.put("totalCount", cachedRanking.size());
                return result;
            }

            // 2. 캐시 없으면 DB에서 조회 후 캐싱
            log.info("■ Ranking cache miss, fetching from DB");
            return fetchAndCacheRanking(limit);

        } catch (Exception e) {
            log.error("■ Error getting ranking: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "랭킹 조회 중 오류가 발생했습니다: " + e.getMessage());
            return result;
        }
    }

    /**
     * 랭킹 캐시 강제 갱신
     */
    public Map<String, Object> refreshRankingCache() {
        log.info("■ RankingService.refreshRankingCache - Refreshing ranking cache");
        
        // 기존 캐시 삭제
        cacheService.delete(REDIS_KEY_RANKING);
        
        // 새로 조회하여 캐싱
        return fetchAndCacheRanking(cacheSize);
    }

    /**
     * DB에서 랭킹 조회 후 Redis에 캐싱
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchAndCacheRanking(Integer limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            HashMap<String, Object> dbResult = transactionService.getRanking(cacheSize);

            // Mock 모드 또는 실제 DB 응답에서 데이터 추출
            List<Map<String, Object>> rankingList = null;
            
            if (dbResult.get("data") instanceof List) {
                rankingList = (List<Map<String, Object>>) dbResult.get("data");
            } else if (dbResult.get("mock") != null) {
                // Mock 모드: 샘플 데이터 생성
                rankingList = generateMockRankingData();
            }

            if (rankingList != null && !rankingList.isEmpty()) {
                // Redis에 캐싱
                cacheService.setObject(REDIS_KEY_RANKING, rankingList, cacheTtl, TimeUnit.SECONDS);
                log.info("■ Ranking cached to Redis: {} entries, TTL: {}s", rankingList.size(), cacheTtl);

                // 요청한 개수만큼만 반환
                List<Map<String, Object>> limitedRanking = rankingList.stream()
                    .limit(limit != null ? limit : cacheSize)
                    .toList();

                result.put("success", true);
                result.put("source", "db");
                result.put("data", limitedRanking);
                result.put("totalCount", rankingList.size());
            } else {
                result.put("success", true);
                result.put("source", "db");
                result.put("data", Collections.emptyList());
                result.put("totalCount", 0);
                result.put("message", "랭킹 데이터가 없습니다.");
            }

        } catch (KinfaRunException e) {
            log.error("■ Error fetching ranking from DB: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "랭킹 조회 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * Mock 랭킹 데이터 생성 (Development 모드용)
     */
    private List<Map<String, Object>> generateMockRankingData() {
        List<Map<String, Object>> mockData = new ArrayList<>();

        String[] adjectives = {"대단한", "현명한", "지혜로운", "용감한", "성실한", "꼼꼼한", "멋진", "훌륭한", "뛰어난", "빛나는"};
        String[] npcs = {"포용이", "채우미"};
        Random random = new Random();

        for (int i = 0; i < 20; i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("rank", i + 1);
            entry.put("ninamNm", adjectives[random.nextInt(adjectives.length)] + " " + npcs[random.nextInt(npcs.length)]);
            entry.put("cmpttModeScr", 100000 - (i * 3000) + random.nextInt(1000));
            entry.put("fnnrMngScr", 30000 + random.nextInt(5000));
            entry.put("riskMngScr", 30000 + random.nextInt(5000));
            entry.put("abslYildScr", 30000 + random.nextInt(5000));
            mockData.add(entry);
        }

        log.info("■ Generated mock ranking data: {} entries", mockData.size());
        return mockData;
    }

    /**
     * 내 순위 조회
     *
     * @param mbrSno 회원 일련번호
     * @return 내 순위 정보
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMyRanking(Long mbrSno) {
        log.debug("■ RankingService.getMyRanking - mbrSno: {}", mbrSno);

        Map<String, Object> result = new HashMap<>();

        try {
            // 전체 랭킹에서 내 순위 찾기
            List<Map<String, Object>> cachedRanking = cacheService.getObject(REDIS_KEY_RANKING, List.class);
            
            if (cachedRanking == null || cachedRanking.isEmpty()) {
                // 캐시 없으면 갱신
                Map<String, Object> refreshResult = refreshRankingCache();
                if (refreshResult.get("data") instanceof List) {
                    cachedRanking = (List<Map<String, Object>>) refreshResult.get("data");
                }
            }

            if (cachedRanking != null) {
                // TODO: mbrSno로 내 닉네임을 찾아서 랭킹에서 검색
                // 현재는 간단히 전체 랭킹 반환
                result.put("success", true);
                result.put("totalCount", cachedRanking.size());
                result.put("message", "전체 랭킹을 확인하세요.");
            } else {
                result.put("success", false);
                result.put("message", "랭킹 데이터를 불러올 수 없습니다.");
            }

        } catch (Exception e) {
            log.error("■ Error getting my ranking: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "내 순위 조회 중 오류가 발생했습니다.");
        }

        return result;
    }

    /**
     * 랭킹 갱신 시간 목록 조회
     */
    public Map<String, Object> getRefreshSchedule() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", refreshEnabled);
        result.put("times", new ArrayList<>(refreshTimes));
        result.put("cacheSize", cacheSize);
        result.put("cacheTtl", cacheTtl);
        return result;
    }

    /**
     * 다음 갱신 시간 조회
     */
    public String getNextRefreshTime() {
        if (!refreshEnabled || refreshTimes.isEmpty()) {
            return null;
        }

        LocalTime now = LocalTime.now();
        String currentTimeStr = now.format(TIME_FORMATTER);

        // 오늘 남은 갱신 시간 중 가장 빠른 시간 찾기
        String nextTime = refreshTimes.stream()
            .filter(time -> time.compareTo(currentTimeStr) > 0)
            .min(String::compareTo)
            .orElse(null);

        if (nextTime == null) {
            // 오늘 남은 시간이 없으면 내일 첫 번째 시간
            nextTime = refreshTimes.stream()
                .min(String::compareTo)
                .orElse(null);
        }

        return nextTime;
    }

    // ========================================
    // 점수 계산
    // ========================================

    /**
     * 게임 결과 점수 계산
     *
     * @param session 게임 세션
     * @param portfolio 최종 포트폴리오
     * @param initialCash 초기 자본금
     * @return 점수 결과
     */
    public ScoreResult calculateScore(com.cas.api.dto.domain.GameSessionDto session,
                                       com.cas.api.dto.domain.PortfolioDto portfolio,
                                       long initialCash) {
        log.info("■ RankingService.calculateScore - uid: {}, initialCash: {}", 
            session != null ? session.getUid() : "null", initialCash);

        // 1. 총 자산 계산
        long totalAssets = 0;
        if (portfolio != null) {
            totalAssets = portfolio.getTotalAssets() != null ? portfolio.getTotalAssets() : 0;
            if (totalAssets == 0 && portfolio.getCash() != null) {
                totalAssets = portfolio.getCash();
            }
        }

        // 2. 수익률 계산
        double returnRate = 0.0;
        if (initialCash > 0) {
            returnRate = ((double) (totalAssets - initialCash) / initialCash) * 100;
        }

        // 3. 세부 점수 계산 (각 항목 최대 40,000점, 총 최대 120,000점)
        
        // 3.1 재무 관리 점수 (수익률 기반)
        long fnnrMngScr = calculateFinancialManagementScore(returnRate);

        // 3.2 리스크 관리 점수 (분산투자, 보험, 대출 현황 기반)
        long riskMngScr = calculateRiskManagementScore(session, portfolio);

        // 3.3 절대 수익 점수 (총 자산 기반)
        long abslYildScr = calculateAbsoluteYieldScore(totalAssets, initialCash);

        // 4. 총점 계산
        long totalScore = fnnrMngScr + riskMngScr + abslYildScr;

        // 5. 패널티 적용
        if (session != null && Boolean.TRUE.equals(session.getIllegalLoanUsed())) {
            // 불법사금융 사용 시 20% 감점
            totalScore = (long) (totalScore * 0.8);
            log.info("■ Illegal loan penalty applied: -20%");
        }

        ScoreResult result = new ScoreResult();
        result.totalScore = totalScore;
        result.fnnrMngScr = fnnrMngScr;
        result.riskMngScr = riskMngScr;
        result.abslYildScr = abslYildScr;
        result.returnRate = returnRate;
        result.totalAssets = totalAssets;

        log.info("■ Score calculated - total: {}, fnnr: {}, risk: {}, absl: {}, returnRate: {:.2f}%",
            totalScore, fnnrMngScr, riskMngScr, abslYildScr, returnRate);

        return result;
    }

    /**
     * 재무 관리 점수 계산 (수익률 기반)
     * 최대 40,000점
     */
    private long calculateFinancialManagementScore(double returnRate) {
        // 수익률 구간별 점수
        // -50% 이하: 0점
        // -50% ~ 0%: 0 ~ 20,000점
        // 0% ~ 50%: 20,000 ~ 35,000점
        // 50% ~ 100%: 35,000 ~ 40,000점
        // 100% 이상: 40,000점

        if (returnRate <= -50) return 0;
        if (returnRate < 0) return (long) ((returnRate + 50) / 50 * 20000);
        if (returnRate < 50) return (long) (20000 + (returnRate / 50 * 15000));
        if (returnRate < 100) return (long) (35000 + ((returnRate - 50) / 50 * 5000));
        return 40000;
    }

    /**
     * 리스크 관리 점수 계산
     * 최대 40,000점
     */
    private long calculateRiskManagementScore(com.cas.api.dto.domain.GameSessionDto session,
                                               com.cas.api.dto.domain.PortfolioDto portfolio) {
        long score = 0;

        if (session == null) return 20000; // 기본 점수

        // 1. 보험 가입 여부 (최대 10,000점)
        if (Boolean.TRUE.equals(session.getInsuranceSubscribed())) {
            score += 10000;
        }

        // 2. 불법사금융 미사용 (10,000점)
        if (!Boolean.TRUE.equals(session.getIllegalLoanUsed())) {
            score += 10000;
        }

        // 3. 분산투자 점수 (최대 15,000점)
        if (portfolio != null) {
            int assetTypes = 0;
            if (portfolio.getDeposits() != null && !portfolio.getDeposits().isEmpty()) assetTypes++;
            if (portfolio.getSavings() != null && !portfolio.getSavings().isEmpty()) assetTypes++;
            if (portfolio.getStocks() != null && !portfolio.getStocks().isEmpty()) assetTypes++;
            if (portfolio.getBonds() != null && !portfolio.getBonds().isEmpty()) assetTypes++;
            if (portfolio.getFunds() != null && !portfolio.getFunds().isEmpty()) assetTypes++;
            if (portfolio.getPensions() != null && !portfolio.getPensions().isEmpty()) assetTypes++;

            // 자산 종류당 2,500점 (최대 6종류 = 15,000점)
            score += Math.min(assetTypes * 2500, 15000);
        }

        // 4. 조언 활용 점수 (최대 5,000점)
        if (session.getAdviceUsedCount() != null && session.getAdviceUsedCount() > 0) {
            score += Math.min(session.getAdviceUsedCount() * 1500, 5000);
        }

        return Math.min(score, 40000);
    }

    /**
     * 절대 수익 점수 계산 (총 자산 기반)
     * 최대 40,000점
     */
    private long calculateAbsoluteYieldScore(long totalAssets, long initialCash) {
        if (initialCash <= 0) return 20000; // 기본 점수

        // 자산 증가 비율 기반
        double ratio = (double) totalAssets / initialCash;

        // 0배: 0점, 1배: 20,000점, 2배: 35,000점, 3배 이상: 40,000점
        if (ratio <= 0) return 0;
        if (ratio < 1) return (long) (ratio * 20000);
        if (ratio < 2) return (long) (20000 + ((ratio - 1) * 15000));
        if (ratio < 3) return (long) (35000 + ((ratio - 2) * 5000));
        return 40000;
    }

    /**
     * 점수 계산 결과 클래스
     */
    public static class ScoreResult {
        public long totalScore;      // 총점
        public long fnnrMngScr;      // 재무 관리 점수
        public long riskMngScr;      // 리스크 관리 점수
        public long abslYildScr;     // 절대 수익 점수 (= returnRateScore)
        public double returnRate;    // 수익률 (%)
        public long totalAssets;     // 총 자산

        public long getTotalScore() { return totalScore; }
        public long getFnnrMngScr() { return fnnrMngScr; }
        public long getRiskMngScr() { return riskMngScr; }
        public long getAbslYildScr() { return abslYildScr; }
        public double getReturnRate() { return returnRate; }
        public long getTotalAssets() { return totalAssets; }
        
        // 컨트롤러 호환용 별칭 메서드
        public long getFinancialManagementScore() { return fnnrMngScr; }
        public long getRiskManagementScore() { return riskMngScr; }
        public long getReturnRateScore() { return abslYildScr; }
    }
}
