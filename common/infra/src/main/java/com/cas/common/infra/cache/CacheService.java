package com.cas.common.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 캐시 서비스
 * Redis/NHN EasyCache를 활용한 캐싱 처리
 * 
 * String과 Object를 모두 지원합니다.
 * Object는 자동으로 JSON으로 직렬화/역직렬화됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 캐시 저장 (String 전용)
     */
    public void set(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Failed to set cache. key={}", key, e);
        }
    }

    /**
     * 캐시 저장 (TTL 포함, String 전용)
     */
    public void set(String key, String value, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
        } catch (Exception e) {
            log.error("Failed to set cache with TTL. key={}, timeout={}", key, timeout, e);
        }
    }

    /**
     * 캐시 조회 (String 반환)
     */
    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get cache. key={}", key, e);
            return null;
        }
    }

    /**
     * 캐시 삭제
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to delete cache. key={}", key, e);
        }
    }

    /**
     * 캐시 존재 여부 확인
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check cache existence. key={}", key, e);
            return false;
        }
    }

    /**
     * TTL 설정
     */
    public void expire(String key, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.expire(key, timeout, timeUnit);
        } catch (Exception e) {
            log.error("Failed to set expiration. key={}, timeout={}", key, timeout, e);
        }
    }

    // ========================================
    // Object 지원 메서드 (JSON 자동 변환)
    // ========================================

    /**
     * 캐시 저장 (Object 자동 JSON 변환)
     * 
     * @param key Redis 키
     * @param value 저장할 객체 (자동으로 JSON으로 변환)
     */
    public void setObject(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json);
            log.debug("Cached object: key={}, type={}", key, value.getClass().getSimpleName());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON. key={}, type={}", 
                     key, value.getClass().getName(), e);
            throw new RuntimeException("Failed to cache object: " + key, e);
        } catch (Exception e) {
            log.error("Failed to set object cache. key={}", key, e);
            throw new RuntimeException("Failed to cache object: " + key, e);
        }
    }

    /**
     * 캐시 저장 (Object 자동 JSON 변환, TTL 포함)
     * 
     * @param key Redis 키
     * @param value 저장할 객체 (자동으로 JSON으로 변환)
     * @param timeout TTL 시간
     * @param timeUnit 시간 단위
     */
    public void setObject(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, timeout, timeUnit);
            log.debug("Cached object with TTL: key={}, type={}, ttl={}{}",
                     key, value.getClass().getSimpleName(), timeout, timeUnit);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON. key={}, type={}", 
                     key, value.getClass().getName(), e);
            throw new RuntimeException("Failed to cache object: " + key, e);
        } catch (Exception e) {
            log.error("Failed to set object cache with TTL. key={}, timeout={}", key, timeout, e);
            throw new RuntimeException("Failed to cache object: " + key, e);
        }
    }

    /**
     * 캐시 조회 (JSON을 Object로 자동 변환)
     * 
     * @param key Redis 키
     * @param clazz 변환할 클래스 타입
     * @return 역직렬화된 객체, 없으면 null
     */
    public <T> T getObject(String key, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("Cache miss: key={}", key);
                return null;
            }
            T object = objectMapper.readValue(json, clazz);
            log.debug("Cache hit: key={}, type={}", key, clazz.getSimpleName());
            return object;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to object. key={}, type={}", 
                     key, clazz.getName(), e);
            return null;
        } catch (Exception e) {
            log.error("Failed to get object cache. key={}, type={}", key, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 캐시 조회 (JSON을 제네릭 타입으로 자동 변환)
     * 
     * 사용 예시:
     * <pre>
     * {@code
     * List<String> list = cacheService.getObject(
     *     "myKey", 
     *     objectMapper.getTypeFactory()
     *         .constructCollectionType(List.class, String.class)
     * );
     * }
     * </pre>
     * 
     * @param key Redis 키
     * @param typeReference 제네릭 타입 정보
     * @return 역직렬화된 객체, 없으면 null
     */
    public <T> T getObject(String key, com.fasterxml.jackson.core.type.TypeReference<T> typeReference) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("Cache miss: key={}", key);
                return null;
            }
            T object = objectMapper.readValue(json, typeReference);
            log.debug("Cache hit: key={}", key);
            return object;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to object. key={}", key, e);
            return null;
        } catch (Exception e) {
            log.error("Failed to get object cache. key={}", key, e);
            return null;
        }
    }

    // ========================================
    // Counter 지원 메서드 (원자적 증가)
    // ========================================

    /**
     * 카운터 증가 (원자적 연산)
     * 
     * @param key Redis 키
     * @return 증가 후 값
     */
    public Long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            log.debug("Incremented counter: key={}, value={}", key, value);
            return value;
        } catch (Exception e) {
            log.error("Failed to increment counter. key={}", key, e);
            return null;
        }
    }

    /**
     * 카운터 증가 (원자적 연산, delta 지정)
     * 
     * @param key Redis 키
     * @param delta 증가량
     * @return 증가 후 값
     */
    public Long increment(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            log.debug("Incremented counter: key={}, delta={}, value={}", key, delta, value);
            return value;
        } catch (Exception e) {
            log.error("Failed to increment counter. key={}, delta={}", key, delta, e);
            return null;
        }
    }

    /**
     * 카운터 값 조회
     * 
     * @param key Redis 키
     * @return 현재 카운터 값, 없으면 null
     */
    public Long getCounter(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return Long.parseLong(value);
        } catch (Exception e) {
            log.error("Failed to get counter. key={}", key, e);
            return null;
        }
    }

    /**
     * 카운터 초기화
     * 
     * @param key Redis 키
     * @param initialValue 초기값
     */
    public void setCounter(String key, long initialValue) {
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(initialValue));
            log.debug("Set counter: key={}, value={}", key, initialValue);
        } catch (Exception e) {
            log.error("Failed to set counter. key={}, value={}", key, initialValue, e);
        }
    }
}

