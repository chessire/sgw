package com.cas.common.web.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 보안 헤더 필터
 * 
 * Spring Framework RCE 취약점 방어를 위한 추가 보안 조치:
 * - 위험한 Content-Type 차단
 * - 보안 헤더 추가
 * - 입력 검증 강화
 */
public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 초기화 로직 (필요 시)
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // ================================================
        // 1. 위험한 Content-Type 차단
        // ================================================
        String contentType = httpRequest.getContentType();
        if (contentType != null) {
            // Java Serialization Content-Type 차단
            if (contentType.contains("application/x-java-serialized-object") ||
                contentType.contains("application/x-java-serialization") ||
                contentType.contains("application/octet-stream")) {
                
                httpResponse.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write(
                    "{\"error\":\"UNSUPPORTED_MEDIA_TYPE\",\"message\":\"Java serialization is not allowed for security reasons\"}"
                );
                return;
            }
        }
        
        // ================================================
        // 2. 보안 헤더 추가
        // ================================================
        
        // XSS 방어
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        
        // CSP (Content Security Policy)
        httpResponse.setHeader("Content-Security-Policy", "default-src 'self'");
        
        // HSTS (HTTPS 강제 - 프로덕션 환경에서만 활성화 권장)
        // httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        
        // Referrer Policy
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions Policy
        httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 정리 로직 (필요 시)
    }
}

