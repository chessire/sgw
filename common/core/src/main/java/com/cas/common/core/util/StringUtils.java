package com.cas.common.core.util;

/**
 * 문자열 유틸리티 클래스
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {
    
    /**
     * 문자열이 비어있는지 확인
     * 
     * @param str 확인할 문자열
     * @return 비어있으면 true
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 문자열이 비어있지 않은지 확인
     * 
     * @param str 확인할 문자열
     * @return 비어있지 않으면 true
     */
    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }
}

