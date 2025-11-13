package com.cas.common.infra.gameobject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * GameObject Factory Parser
 * 
 * Type key 기반으로 적절한 GameObject 인스턴스를 생성합니다.
 * Strategy Pattern + Registry Pattern을 사용하여 확장 가능한 구조를 제공합니다.
 * 
 * 사용 예시:
 * <pre>
 *   String json = "{\"objectType\":\"TestObject\", ...}";
 *   GameObject obj = GameObjectParser.parse(json);
 * </pre>
 */
@Slf4j
public class GameObjectParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, Function<JsonNode, GameObject>> PARSER_REGISTRY = new HashMap<>();

    static {
        // ObjectMapper 설정
        MAPPER.registerModule(new JavaTimeModule());
        
        // Parser 등록
        registerParser("TestObject", GameObjectParser::parseTestObject);
        
        log.info("GameObjectParser initialized with {} registered types", PARSER_REGISTRY.size());
    }

    /**
     * 새로운 GameObject 타입의 파서를 등록합니다.
     * 
     * @param objectType GameObject의 타입 (예: "TestObject", "PlayerObject", "ItemObject")
     * @param parser JsonNode를 받아 GameObject를 반환하는 함수
     */
    public static void registerParser(String objectType, Function<JsonNode, GameObject> parser) {
        if (PARSER_REGISTRY.containsKey(objectType)) {
            log.warn("Overwriting existing parser for type: {}", objectType);
        }
        PARSER_REGISTRY.put(objectType, parser);
        log.debug("Registered parser for type: {}", objectType);
    }

    /**
     * JSON 문자열을 파싱하여 적절한 GameObject 인스턴스를 생성합니다.
     * 
     * @param json GameObject를 나타내는 JSON 문자열
     * @return 파싱된 GameObject 인스턴스
     * @throws IllegalArgumentException JSON 파싱 실패 또는 알 수 없는 타입
     */
    public static GameObject parse(String json) {
        try {
            // JSON 파싱
            JsonNode rootNode = MAPPER.readTree(json);
            
            // objectType 추출
            JsonNode typeNode = rootNode.get("objectType");
            if (typeNode == null || typeNode.isNull()) {
                throw new IllegalArgumentException("Missing 'objectType' field in JSON");
            }
            
            String objectType = typeNode.asText();
            log.debug("Parsing GameObject of type: {}", objectType);
            
            // 등록된 파서 조회
            Function<JsonNode, GameObject> parser = PARSER_REGISTRY.get(objectType);
            if (parser == null) {
                throw new IllegalArgumentException(
                    "Unknown GameObject type: " + objectType + 
                    ". Available types: " + PARSER_REGISTRY.keySet()
                );
            }
            
            // 파싱 실행
            GameObject gameObject = parser.apply(rootNode);
            log.debug("Successfully parsed GameObject: {} (id: {})", 
                     gameObject.getObjectType(), gameObject.getObjectId());
            
            return gameObject;
            
        } catch (Exception e) {
            log.error("Failed to parse GameObject from JSON: {}", json, e);
            throw new IllegalArgumentException("Failed to parse GameObject: " + e.getMessage(), e);
        }
    }

    /**
     * GameObject 타입이 등록되어 있는지 확인합니다.
     * 
     * @param objectType 확인할 GameObject 타입
     * @return 등록 여부
     */
    public static boolean isRegistered(String objectType) {
        return PARSER_REGISTRY.containsKey(objectType);
    }

    /**
     * 등록된 모든 GameObject 타입을 반환합니다.
     * 
     * @return GameObject 타입 집합
     */
    public static java.util.Set<String> getRegisteredTypes() {
        return new java.util.HashSet<>(PARSER_REGISTRY.keySet());
    }

    // ========================================
    // Private Parser Methods
    // ========================================

    /**
     * TestObject 전용 파서
     */
    private static GameObject parseTestObject(JsonNode node) {
        try {
            return MAPPER.treeToValue(node, TestObject.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse TestObject", e);
        }
    }

    /**
     * 향후 다른 GameObject 타입 추가 예시:
     * 
     * private static GameObject parsePlayerObject(JsonNode node) {
     *     try {
     *         return MAPPER.treeToValue(node, PlayerObject.class);
     *     } catch (Exception e) {
     *         throw new RuntimeException("Failed to parse PlayerObject", e);
     *     }
     * }
     * 
     * 그리고 static 블록에서:
     * registerParser("PlayerObject", GameObjectParser::parsePlayerObject);
     */
}

