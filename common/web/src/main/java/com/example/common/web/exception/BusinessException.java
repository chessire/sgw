package com.example.common.web.exception;

import lombok.Getter;

/**
 * 비즈니스 예외 클래스
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final String errorMessage;
    
    public BusinessException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public BusinessException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}

