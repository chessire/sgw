package com.cas.api.controller.v1;

import com.cas.common.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 업적 시스템 Controller (목업)
 * 
 * TODO: DB 연동 시 실제 업적 데이터로 대체 필요
 */
@Slf4j
@RestController
@RequestMapping("/v1/achievements")
@RequiredArgsConstructor
public class AchievementController {
    
    /**
     * 업적 목록 조회 (목업 데이터)
     * GET /api/v1/achievements
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getAchievements(@RequestHeader("uid") String uid) {
        
        log.info("Getting achievements (MOCK): uid={}", uid);
        
        try {
            // 목업 업적 데이터 생성 (총 20개)
            List<Map<String, Object>> mockAchievements = Arrays.asList(
                createMockAchievement(1, "튜토리얼 완주", "튜토리얼 완료", true, "2025-11-30T15:30:00", 100),
                createMockAchievement(2, "대박 수익", "한 라운드에 500만원 이상 수익", true, "2025-12-01T10:15:00", 100),
                createMockAchievement(3, "주식 고수", "주식으로 100% 이상 수익률 달성", true, "2025-12-01T14:20:00", 100),
                createMockAchievement(4, "펀드 컬렉터", "5종류 펀드 투자", false, null, 60),
                createMockAchievement(5, "채권 마니아", "연금(채권형) 보유기간 12개월 달성", false, null, 75),
                createMockAchievement(6, "하이 리스커", "주식/펀드 비중 80% 이상 유지", true, "2025-12-02T09:45:00", 100),
                createMockAchievement(7, "저축왕", "예금/적금 12개월 연속 보유", false, null, 83),
                createMockAchievement(8, "조언 수집가", "NPC 조언 수집 3회 완료", true, "2025-12-01T16:30:00", 100),
                createMockAchievement(9, "금융 입문자", "모든 금융 교육 영상 시청 완료", true, "2025-12-02T20:00:00", 100),
                createMockAchievement(10, "복리의 마법", "예금/적금 만기 수령", true, "2025-12-01T12:00:00", 100),
                createMockAchievement(11, "무차입 완주", "대출 없이 12개월 완주", false, null, 50),
                createMockAchievement(12, "정보 수집가", "모든 단서 수집/열람 완료", false, null, 40),
                createMockAchievement(13, "상위 랭커", "경쟁 모드 상위 10% 최초 달성", true, "2025-12-02T18:30:00", 100),
                createMockAchievement(14, "월간 랭커", "월간 랭킹 상위 10% 달성", false, null, 0),
                createMockAchievement(15, "연속 도전", "튜토리얼 & 경쟁 모드 연속 완료", true, "2025-12-02T19:00:00", 100),
                createMockAchievement(16, "금융 종합", "모든 금융 상품에 최소 1회 이상 투자", false, null, 85),
                createMockAchievement(17, "장기 투자자", "주식/펀드 6개월 이상 보유", true, "2025-12-01T11:00:00", 100),
                createMockAchievement(18, "리플레이 5회", "게임(튜토리얼/경쟁 모드) 총 5회 완료", false, null, 60),
                createMockAchievement(19, "리플레이 10회", "게임(튜토리얼/경쟁 모드) 총 10회 완료", false, null, 30),
                createMockAchievement(20, "순자산의 힘", "모든 인생 이벤트 현금 대처", false, null, 70)
            );
            
            // 달성된 업적 개수 계산
            long achievedCount = mockAchievements.stream()
                .filter(a -> (Boolean) a.get("achieved"))
                .count();
            
            Map<String, Object> data = new HashMap<>();
            data.put("achievements", mockAchievements);
            data.put("totalAchievements", 20);
            data.put("achievedCount", (int) achievedCount);
            data.put("achievementRate", String.format("%.1f%%", (achievedCount * 100.0 / 20)));
            data.put("isMockData", true);
            
            log.info("Achievements retrieved (MOCK): achieved={}/{}", achievedCount, 20);
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Failed to get achievements: uid={}", uid, e);
            return ApiResponse.error("FAILED", "업적 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 목업 업적 엔트리 생성 헬퍼 메서드
     */
    private Map<String, Object> createMockAchievement(
            int id,
            String name,
            String description,
            boolean achieved,
            String achievedAt,
            int progress) {
        
        Map<String, Object> achievement = new HashMap<>();
        achievement.put("achievementId", id);
        achievement.put("name", name);
        achievement.put("description", description);
        achievement.put("achieved", achieved);
        achievement.put("achievedAt", achievedAt);
        achievement.put("progress", progress);
        
        return achievement;
    }
}

