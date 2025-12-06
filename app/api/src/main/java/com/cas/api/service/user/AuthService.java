package com.cas.api.service.user;

import com.cas.api.service.external.TransactionService;
import com.cas.common.core.util.KinfaRunException;
import com.cas.common.infra.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 인증 관리 Service
 * - 로그인/로그아웃 처리
 * - 세션 관리 (Redis)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TransactionService transactionService;
    private final UserService userService;
    private final CacheService cacheService;

    // Redis 키 패턴
    private static final String REDIS_KEY_LOGIN_SESSION = "login:session:%s";
    private static final int LOGIN_SESSION_TTL = 86400; // 24시간

    // 로그인 방법 코드
    public static final String LOGIN_METHOD_SIMPLE = "01"; // 간편인증
    public static final String LOGIN_METHOD_PASS = "02";   // PASS 인증

    // 로그인 구분 코드
    public static final String LOGIN_TYPE_NORMAL = "01";   // 일반 로그인
    public static final String LOGIN_TYPE_AUTO = "02";     // 자동 로그인

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 로그인 처리
     * 
     * @param mbrSno 회원일련번호
     */
    public Map<String, Object> login(Long mbrSno) {
        log.info("■ AuthService.login - mbrSno: {}", mbrSno);

        Map<String, Object> result = new HashMap<>();
        String loginDt = LocalDateTime.now().format(DATE_FORMATTER);

        try {
            // 1. 사용자 확인
            Map<String, Object> userCheck = userService.checkUser(mbrSno);
            if (!(Boolean) userCheck.get("exists")) {
                log.warn("■ User not found for login: {}", mbrSno);
                result.put("success", false);
                result.put("message", "등록되지 않은 사용자입니다.");
                return result;
            }

            // 2. 기존 세션 확인 (중복 로그인 체크)
            String redisKey = String.format(REDIS_KEY_LOGIN_SESSION, mbrSno);
            Map<String, Object> existingSession = cacheService.getObject(redisKey, Map.class);
            
            if (existingSession != null) {
                log.info("■ Existing session found, will be replaced: {}", mbrSno);
                // 기존 세션의 로그아웃 처리 (필요시)
                String prevLoginDt = (String) existingSession.get("loginDt");
                if (prevLoginDt != null) {
                    try {
                        transactionService.updateLogout(mbrSno, prevLoginDt);
                    } catch (KinfaRunException e) {
                        log.warn("■ Failed to logout previous session: {}", e.getMessage());
                    }
                }
            }

            // 3. DB에 로그인 기록 저장 (기본값 사용)
            HashMap<String, Object> dbResult = transactionService.createLoginLog(mbrSno, LOGIN_METHOD_SIMPLE, LOGIN_TYPE_NORMAL);
            log.info("■ Login log created in DB: {}", dbResult);

            // 4. Redis에 세션 저장
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("mbrSno", mbrSno);
            sessionData.put("loginDt", loginDt);
            sessionData.put("user", userCheck.get("user"));
            sessionData.put("isLoggedIn", true);

            cacheService.setObject(redisKey, sessionData, LOGIN_SESSION_TTL, TimeUnit.SECONDS);
            log.info("■ Session saved to Redis: {}", redisKey);

            result.put("success", true);
            result.put("mbrSno", mbrSno);
            result.put("loginTime", loginDt);
            result.put("message", "로그인 성공");

        } catch (KinfaRunException e) {
            log.error("■ Login error: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 로그아웃 처리
     */
    public Map<String, Object> logout(Long mbrSno) {
        log.info("■ AuthService.logout - mbrSno: {}", mbrSno);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Redis에서 세션 조회
            String redisKey = String.format(REDIS_KEY_LOGIN_SESSION, mbrSno);
            Map<String, Object> sessionData = cacheService.getObject(redisKey, Map.class);

            if (sessionData == null) {
                log.warn("■ No session found for logout: {}", mbrSno);
                result.put("success", false);
                result.put("message", "로그인 세션이 없습니다.");
                return result;
            }

            // 2. DB에 로그아웃 시간 업데이트
            String loginDt = (String) sessionData.get("loginDt");
            if (loginDt != null) {
                HashMap<String, Object> dbResult = transactionService.updateLogout(mbrSno, loginDt);
                log.info("■ Logout time updated in DB: {}", dbResult);
            }

            // 3. Redis에서 세션 삭제
            cacheService.delete(redisKey);
            log.info("■ Session removed from Redis: {}", redisKey);

            result.put("success", true);
            result.put("message", "로그아웃 성공");

        } catch (KinfaRunException e) {
            log.error("■ Logout error: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "로그아웃 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 세션 조회
     */
    public Map<String, Object> getSession(Long mbrSno) {
        log.debug("■ AuthService.getSession - mbrSno: {}", mbrSno);

        String redisKey = String.format(REDIS_KEY_LOGIN_SESSION, mbrSno);
        Map<String, Object> sessionData = cacheService.getObject(redisKey, Map.class);

        if (sessionData == null) {
            return null;
        }

        return sessionData;
    }

    /**
     * 로그인 상태 확인
     */
    public boolean isLoggedIn(Long mbrSno) {
        Map<String, Object> session = getSession(mbrSno);
        if (session == null) {
            return false;
        }
        return Boolean.TRUE.equals(session.get("isLoggedIn"));
    }

    /**
     * 세션 갱신 (TTL 연장)
     */
    public void refreshSession(Long mbrSno) {
        String redisKey = String.format(REDIS_KEY_LOGIN_SESSION, mbrSno);
        Map<String, Object> sessionData = cacheService.getObject(redisKey, Map.class);

        if (sessionData != null) {
            cacheService.setObject(redisKey, sessionData, LOGIN_SESSION_TTL, TimeUnit.SECONDS);
            log.debug("■ Session refreshed: {}", redisKey);
        }
    }

    /**
     * 모든 세션 로그아웃 (관리자용)
     */
    public void forceLogout(Long mbrSno) {
        log.info("■ AuthService.forceLogout - mbrSno: {}", mbrSno);

        String redisKey = String.format(REDIS_KEY_LOGIN_SESSION, mbrSno);
        cacheService.delete(redisKey);

        // 사용자 캐시도 삭제
        userService.removeUserFromCache(mbrSno);

        log.info("■ Force logout completed: {}", mbrSno);
    }
}

