package com.cas.api.controller.v1;

import com.cas.api.dto.request.ValidateRequest;
import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.enums.GameMode;
import com.cas.api.service.game.GameSessionService;
import com.cas.common.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * 검증 Controller
 * - UID 및 게임 데이터 MD5 검증
 * - Health check는 /api/health (HealthController) 사용
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ValidationController {
    
    private final GameSessionService gameSessionService;
    
    // game_data.min.json의 MD5 체크섬 (애플리케이션 시작 시 계산)
    private static String GAME_DATA_MD5 = null;
    
    /**
     * 검증 API
     * POST /api/v1/validate
     * 
     * @param request uid, dataFileMD5
     * @return 저장된 게임 데이터 정보
     */
    @PostMapping("/validate")
    public ApiResponse<Map<String, Object>> validate(@RequestBody ValidateRequest request) {
        log.info("Validate requested: uid={}", request.getUid());
        
        try {
            String uid = request.getUid();
            String clientMD5 = request.getDataFileMD5();
            
            // 1. UID 검증 (간단한 형식 체크)
            if (!isValidUid(uid)) {
                log.warn("Invalid UID format: {}", uid);
                return ApiResponse.error("INVALID_UID", "유효하지 않은 UID 형식입니다.");
            }
            
            // 2. MD5 검증
            if (GAME_DATA_MD5 == null) {
                GAME_DATA_MD5 = calculateGameDataMD5();
            }
            
            if (!GAME_DATA_MD5.equals(clientMD5)) {
                log.warn("MD5 mismatch: expected={}, actual={}", GAME_DATA_MD5, clientMD5);
                return ApiResponse.error("INVALID_MD5", "게임 데이터 버전이 일치하지 않습니다.");
            }
            
            // 3. 저장된 게임 세션 확인
            Map<String, Object> data = new HashMap<>();
            data.put("uid", uid);
            data.put("validMD5", true);
            
            // 튜토리얼 세션 확인
            GameSessionDto tutorialSession = gameSessionService.getSession(uid, GameMode.TUTORIAL);
            if (tutorialSession != null) {
                data.put("hasTutorial", true);
                data.put("tutorialCompleted", tutorialSession.getCompleted());
                data.put("tutorialRound", tutorialSession.getCurrentRound());
            } else {
                data.put("hasTutorial", false);
                data.put("tutorialCompleted", false);
            }
            
            // 경쟁모드 세션 확인
            GameSessionDto competitionSession = gameSessionService.getSession(uid, GameMode.COMPETITION);
            if (competitionSession != null) {
                data.put("hasCompetition", true);
                data.put("competitionCompleted", competitionSession.getCompleted());
                data.put("competitionRound", competitionSession.getCurrentRound());
            } else {
                data.put("hasCompetition", false);
                data.put("competitionCompleted", false);
            }
            
            log.info("Validation successful: uid={}, hasTutorial={}, hasCompetition={}", 
                uid, data.get("hasTutorial"), data.get("hasCompetition"));
            
            return ApiResponse.success(data);
            
        } catch (Exception e) {
            log.error("Validation failed", e);
            return ApiResponse.error("VALIDATION_ERROR", "검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * UID 형식 검증 (간단한 체크)
     */
    private boolean isValidUid(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return false;
        }
        
        // 길이 체크 (예: 3~100자)
        if (uid.length() < 3 || uid.length() > 100) {
            return false;
        }
        
        // 허용된 문자만 포함 (알파벳, 숫자, 하이픈, 언더스코어)
        return uid.matches("^[a-zA-Z0-9_-]+$");
    }
    
    /**
     * game_data.min.json의 MD5 체크섬 계산
     */
    private String calculateGameDataMD5() {
        try {
            // resources/static/game_data.min.json 경로
            String resourcePath = getClass().getClassLoader()
                .getResource("static/game_data.min.json")
                .getPath();
            
            // Windows 경로 처리 (앞에 / 제거)
            if (resourcePath.startsWith("/") && resourcePath.contains(":")) {
                resourcePath = resourcePath.substring(1);
            }
            
            File file = new File(resourcePath);
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            
            String md5 = sb.toString();
            log.info("Game data MD5 calculated: {}", md5);
            return md5;
            
        } catch (Exception e) {
            log.error("Failed to calculate MD5", e);
            return "UNKNOWN";
        }
    }
}

