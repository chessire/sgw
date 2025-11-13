package com.example.common.web.dto;

import com.example.common.web.constant.ApiConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * API 응답 공통 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String code;
    private String message;
    private T data;
    
    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ApiConstants.SUCCESS_CODE, "Success", data);
    }
    
    /**
     * 성공 응답 생성 (데이터 없음)
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }
    
    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
    
    /**
     * 실패 응답 생성 (기본 에러 코드)
     */
    public static <T> ApiResponse<T> error(String message) {
        return error(ApiConstants.ERROR_CODE, message);
    }
}

