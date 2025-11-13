package com.example.common.core.constant;

/**
 * 애플리케이션 전역 상수
 */
public class Constants {
    
    private Constants() {
        throw new IllegalStateException("Utility class");
    }
    
    // 인코딩
    public static final String DEFAULT_ENCODING = "UTF-8";
    
    // 날짜 포맷
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmss";
    
    // 페이징
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_PAGE_NUMBER = 1;
    
    // 응답 코드
    public static final String SUCCESS_CODE = "0000";
    public static final String ERROR_CODE = "9999";
}

