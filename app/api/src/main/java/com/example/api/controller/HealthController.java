package com.example.api.controller;

import com.example.common.web.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = "Health Check")
public class HealthController {

    @GetMapping
    @ApiOperation(value = "헬스체크", notes = "서버 상태를 확인합니다.")
    public ApiResponse<Map<String, Object>> health() {
        log.debug("Health check requested");
        
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiResponse.success(data);
    }
}

