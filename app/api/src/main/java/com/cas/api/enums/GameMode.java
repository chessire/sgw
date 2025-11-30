package com.cas.api.enums;

import lombok.Getter;

/**
 * 게임 모드
 * - TUTORIAL: 튜토리얼 모드 (6라운드)
 * - COMPETITION: 경쟁 모드 (12라운드)
 */
@Getter
public enum GameMode {
    TUTORIAL(6, "tutorial", "튜토리얼"),
    COMPETITION(12, "competition", "경쟁모드");
    
    private final int maxRounds;
    private final String code;
    private final String displayName;
    
    GameMode(int maxRounds, String code, String displayName) {
        this.maxRounds = maxRounds;
        this.code = code;
        this.displayName = displayName;
    }
    
    /**
     * code로 GameMode 찾기
     */
    public static GameMode fromCode(String code) {
        for (GameMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid game mode code: " + code);
    }
    
    /**
     * 라운드 번호가 유효한지 체크
     */
    public boolean isValidRound(int roundNo) {
        return roundNo >= 1 && roundNo <= maxRounds;
    }
}

