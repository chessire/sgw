package com.cas.common.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 제공자
 * JJWT 0.11.5+ 사용
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:mySecretKey12345678901234567890}")
    private String secretKey;

    @Value("${jwt.expiration:3600000}")
    private long validityInMilliseconds;

    private SecretKey key;

    /**
     * SecretKey 초기화
     * JJWT 0.11.5+에서는 String 대신 SecretKey 객체 사용
     */
    @PostConstruct
    public void init() {
        // secretKey를 256비트(32바이트) 이상으로 확장
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        
        // HMAC SHA-256은 최소 256비트(32바이트) 필요
        if (keyBytes.length < 32) {
            // 키가 짧으면 반복하여 32바이트로 확장
            byte[] expandedKey = new byte[32];
            for (int i = 0; i < 32; i++) {
                expandedKey[i] = keyBytes[i % keyBytes.length];
            }
            keyBytes = expandedKey;
        }
        
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT SecretKey initialized successfully");
    }

    /**
     * 토큰 생성
     * JJWT 0.11.5 API 사용
     */
    public String createToken(String subject) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)  // SecretKey 사용 (SignatureAlgorithm은 자동 감지)
                .compact();
    }

    /**
     * 토큰에서 사용자 정보 추출
     */
    public String getSubject(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("Invalid JWT token", e);
            return false;
        }
    }

    /**
     * Claims 추출
     * JJWT 0.11.5 API: parser() → parserBuilder()
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)  // SecretKey 사용
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

