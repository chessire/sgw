package com.cas.api.controller;

import com.cas.common.web.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 헬스체크 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/health")
@Tag(name = "Health Check", description = "서버 상태 확인 API")
public class HealthController {

    @GetMapping
    @Operation(summary = "헬스체크", description = "서버 상태를 확인합니다.")
    public ApiResponse<Map<String, Object>> health() {
        log.debug("Health check requested");
        
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiResponse.success(data);
    }
}

