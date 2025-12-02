package com.cas.api.dto.request;

import lombok.Data;

/**
 * NPC 선택/배정 Request
 */
@Data
public class NpcRequest {
    
    /**
     * NPC 타입 (문자열)
     * 예: "POYONGI", "CHAEWOOMI" 등
     */
    private String npcType;
}

