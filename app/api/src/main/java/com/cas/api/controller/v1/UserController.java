package com.cas.api.controller.v1;

import com.cas.api.service.user.AuthService;
import com.cas.api.service.user.UserService;
import com.cas.common.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 관리 API Controller
 * - 사용자 생성/조회
 * - 로그인/로그아웃
 */
@Slf4j
@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * 사용자 존재 여부 확인
     * GET /v1/user/check-user
     */
    @GetMapping("/check-user")
    public ApiResponse<Map<String, Object>> checkUser(@RequestHeader("uid") String uid) {
        Long mbrSno = Long.parseLong(uid);
        log.info("■ [API] GET /check-user - mbrSno: {}", mbrSno);
        
        Map<String, Object> result = userService.checkUser(mbrSno);
        return ApiResponse.success(result);
    }

    /**
     * 사용자 생성 (닉네임 자동 생성 - 형용사, NPC 모두 랜덤)
     * POST /v1/user/create-user
     */
    @PostMapping("/create-user")
    public ApiResponse<Map<String, Object>> createUser(@RequestHeader("uid") String uid) {
        
        Long mbrSno = Long.parseLong(uid);
        log.info("■ [API] POST /create-user - mbrSno: {}", mbrSno);
        
        Map<String, Object> result = userService.createUser(mbrSno);
        
        if ((Boolean) result.get("success")) {
            return ApiResponse.success(result);
        } else {
            return ApiResponse.error("CREATE_FAILED", (String) result.get("message"));
        }
    }

    /**
     * 닉네임 랜덤 변경 (형용사 + NPC 모두 랜덤)
     * POST /v1/user/change-nickname
     */
    @PostMapping("/change-nickname")
    public ApiResponse<Map<String, Object>> changeNickname(@RequestHeader("uid") String uid) {
        
        Long mbrSno = Long.parseLong(uid);
        log.info("■ [API] POST /change-nickname - mbrSno: {}", mbrSno);
        
        Map<String, Object> result = userService.changeNickname(mbrSno);
        
        if ((Boolean) result.get("success")) {
            return ApiResponse.success(result);
        } else {
            return ApiResponse.error("CHANGE_FAILED", (String) result.get("message"));
        }
    }

    /**
     * 로그인
     * POST /v1/user/login
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestHeader("uid") String uid) {
        
        Long mbrSno = Long.parseLong(uid);
        log.info("■ [API] POST /login - mbrSno: {}", mbrSno);
        
        Map<String, Object> result = authService.login(mbrSno);
        
        if ((Boolean) result.get("success")) {
            return ApiResponse.success(result);
        } else {
            return ApiResponse.error("LOGIN_FAILED", (String) result.get("message"));
        }
    }

    /**
     * 로그아웃
     * POST /v1/user/logout
     */
    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout(@RequestHeader("uid") String uid) {
        Long mbrSno = Long.parseLong(uid);
        log.info("■ [API] POST /logout - mbrSno: {}", mbrSno);
        
        Map<String, Object> result = authService.logout(mbrSno);
        
        if ((Boolean) result.get("success")) {
            return ApiResponse.success(result);
        } else {
            return ApiResponse.error("LOGOUT_FAILED", (String) result.get("message"));
        }
    }

    /**
     * 로그인 상태 확인
     * GET /v1/user/check-login
     */
    @GetMapping("/check-login")
    public ApiResponse<Map<String, Object>> checkLogin(@RequestHeader("uid") String uid) {
        Long mbrSno = Long.parseLong(uid);
        log.info("■ [API] GET /check-login - mbrSno: {}", mbrSno);
        
        boolean isLoggedIn = authService.isLoggedIn(mbrSno);
        Map<String, Object> session = authService.getSession(mbrSno);
        
        Map<String, Object> result = new HashMap<>();
        result.put("isLoggedIn", isLoggedIn);
        result.put("session", session);
        
        return ApiResponse.success(result);
    }

    /**
     * 세션 갱신
     * POST /v1/user/refresh-session
     */
    @PostMapping("/refresh-session")
    public ApiResponse<String> refreshSession(@RequestHeader("uid") String uid) {
        Long mbrSno = Long.parseLong(uid);
        log.info("■ [API] POST /refresh-session - mbrSno: {}", mbrSno);
        
        if (!authService.isLoggedIn(mbrSno)) {
            return ApiResponse.error("NOT_LOGGED_IN", "로그인 상태가 아닙니다.");
        }
        
        authService.refreshSession(mbrSno);
        return ApiResponse.success("세션이 갱신되었습니다.");
    }

    /**
     * 사용자 정보 조회 (캐시)
     * GET /v1/user/info
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getUserInfo(@RequestHeader("uid") String uid) {
        Long mbrSno = Long.parseLong(uid);
        log.info("■ [API] GET /info - mbrSno: {}", mbrSno);
        
        Map<String, Object> userInfo = userService.loadUserFromCache(mbrSno);
        
        if (userInfo != null) {
            return ApiResponse.success(userInfo);
        } else {
            // 캐시에 없으면 DB에서 조회
            Map<String, Object> result = userService.checkUser(mbrSno);
            if ((Boolean) result.get("exists")) {
                return ApiResponse.success((Map<String, Object>) result.get("user"));
            }
            return ApiResponse.error("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
    }
}
