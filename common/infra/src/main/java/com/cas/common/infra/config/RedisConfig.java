package com.cas.common.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 클래스
 * NHN EasyCache(Redis 기반)를 포함한 Redis 연동 설정
 * 
 * 보안 강화: Java Serialization 차단, JSON 기반 직렬화만 사용
 */
@Configuration
@SuppressWarnings({"deprecation", "null"})
public class RedisConfig {

    @Value("${redis.host:localhost}")
    private String redisHost;

    @Value("${redis.port:6379}")
    private int redisPort;

    @Value("${redis.password:#{null}}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(redisHost);
        jedisConnectionFactory.setPort(redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            jedisConnectionFactory.setPassword(redisPassword);
        }
        jedisConnectionFactory.setUsePool(true);
        return jedisConnectionFactory;
    }

    /**
     * RedisTemplate 설정
     * 
     * 보안 강화:
     * - GenericJackson2JsonRedisSerializer 대신 커스텀 ObjectMapper 사용
     * - Java Serialization 차단
     * - JSON 기반 직렬화만 허용
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        
        // Key Serializer
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        
        // Value Serializer: 보안 강화된 ObjectMapper 사용
        // GenericJackson2JsonRedisSerializer는 DefaultTyping을 사용할 수 있어 위험
        // 대신 커스텀 ObjectMapper를 사용하여 명시적 타입 제어
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(secureRedisObjectMapper());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);
        
        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }

    /**
     * CacheService용 보안 강화된 ObjectMapper
     * 
     * 보안 조치:
     * - Polymorphic Deserialization 화이트리스트 적용
     * - DefaultTyping 비활성화
     * - Java Time API 지원
     * 
     * Note: SecureObjectMapperConfig의 secureObjectMapper가 @Primary로 등록되어 있으므로
     * 이 빈은 명시적으로 주입받을 때만 사용됩니다.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return secureRedisObjectMapper();
    }
    
    /**
     * Redis 직렬화용 보안 강화된 ObjectMapper (private)
     */
    private ObjectMapper secureRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Java Time API 지원
        mapper.registerModule(new JavaTimeModule());
        
        // 보안 설정: DefaultTyping 비활성화 (명시적 확인)
        // Jackson 2.10+ 에서는 기본적으로 비활성화되어 있음
        
        // 알 수 없는 속성 무시
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Polymorphic Type Validator 적용
        com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator ptv = 
            com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("com.cas.")  // 프로젝트 패키지만 허용
                .allowIfBaseType("java.time.") // Java Time API 허용
                .allowIfBaseType("java.util.") // Collections 허용
                .build();
        
        mapper.setPolymorphicTypeValidator(ptv);
        
        return mapper;
    }
}

