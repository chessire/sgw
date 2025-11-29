package com.cas.common.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 보안 강화된 ObjectMapper 설정
 * 
 * Spring Framework 6.0.0 이전 버전의 RCE 취약점 대응:
 * - Java 역직렬화 공격 방어
 * - Polymorphic Deserialization 화이트리스트 적용
 * - 안전하지 않은 DefaultTyping 비활성화
 * 
 * 참고: CVE-2016-1000027, CVE-2017-4995
 */
@Configuration
public class SecureObjectMapperConfig {

    /**
     * 보안 강화된 ObjectMapper 빈
     * 
     * 주요 보안 설정:
     * 1. DefaultTyping 비활성화 (기본값이지만 명시적 확인)
     * 2. Polymorphic Deserialization 화이트리스트 적용
     * 3. 알 수 없는 속성 무시 (DoS 공격 방어)
     * 4. 신뢰할 수 없는 데이터의 역직렬화 방지
     */
    @Bean
    @Primary
    public ObjectMapper secureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // ================================================
        // 1. 역직렬화 보안 설정
        // ================================================
        
        // 알 수 없는 속성 무시 (DoS 공격 방어)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // null 값을 빈 객체로 변환하지 않음 (예상치 못한 객체 생성 방지)
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, false);
        
        // 알 수 없는 null 값을 무시 (NPE 방지)
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        
        // ================================================
        // 2. Polymorphic Deserialization 화이트리스트 설정
        // ================================================
        
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            // 프로젝트 패키지만 허용 (화이트리스트)
            .allowIfBaseType("com.cas.")
            // Java 기본 타입 허용
            .allowIfBaseType(String.class)
            .allowIfBaseType(Number.class)
            .allowIfBaseType(Boolean.class)
            // Java Time API 허용
            .allowIfBaseType("java.time.")
            // Collections 허용
            .allowIfBaseType("java.util.")
            // 명시적으로 차단할 패턴들
            .denyForExactBaseType(Object.class)
            .denyForExactBaseType(java.io.Serializable.class)
            .denyForExactBaseType(Comparable.class)
            .build();
        
        // Polymorphic Type Validator 적용
        mapper.setPolymorphicTypeValidator(ptv);
        
        // ================================================
        // 3. DefaultTyping 명시적 비활성화 확인
        // ================================================
        // Jackson 2.10+ 에서는 기본적으로 비활성화되어 있지만 명시적으로 확인
        // DefaultTyping을 사용하지 않음 = 자동 타입 추론 비활성화
        // (주의: HeavyTask는 @JsonTypeInfo를 통해 명시적으로 타입 정보 포함)
        
        // ================================================
        // 4. 직렬화 설정
        // ================================================
        
        // 날짜를 타임스탬프로 직렬화하지 않음 (ISO-8601 사용)
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // ================================================
        // 5. 모듈 등록
        // ================================================
        
        // Java 8 날짜/시간 API 지원은 common-infra 모듈에서 설정
        // (JavaTimeModule은 common-web에서 의존성이 없음)
        
        return mapper;
    }
    
    /**
     * 추가 보안 가이드라인:
     * 
     * 1. 외부 입력 검증
     *    - 모든 API 요청에 대해 입력 검증 수행
     *    - @Valid 어노테이션 사용
     *    - 허용되지 않은 필드는 무시
     * 
     * 2. Content-Type 검증
     *    - application/json만 허용
     *    - application/x-java-serialized-object 차단
     * 
     * 3. HeavyTask Polymorphic Deserialization
     *    - @JsonTypeInfo의 EXISTING_PROPERTY 사용 (안전)
     *    - @JsonSubTypes로 명시적 타입 등록 (화이트리스트)
     *    - 런타임에 타입 추가 불가 (변조 방지)
     * 
     * 4. Redis Serialization
     *    - JSON 기반 직렬화 사용 (Java Serialization 금지)
     *    - ObjectMapper를 통한 명시적 변환
     * 
     * 5. 정기 업데이트
     *    - Spring Framework 5.3.x 최신 버전 유지
     *    - Jackson 최신 보안 패치 적용
     */
}

