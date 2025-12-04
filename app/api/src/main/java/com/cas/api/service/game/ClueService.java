package com.cas.api.service.game;

import com.cas.api.constant.GameConstants;
import com.cas.api.dto.domain.GameSessionDto;
import com.cas.api.dto.domain.PortfolioDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 단서 시스템 Service
 * - 심화정보 구매
 * - 라운드별 단서 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClueService {
    
    private static final long ADDITIONAL_INFO_COST = 100000L; // 심화정보 비용: 10만원
    
    /**
     * 심화정보 구매
     * 
     * @param session 게임 세션
     * @param portfolio 포트폴리오
     * @param infoKey 정보 키 (프론트엔드에서 game_data.json의 키 전달)
     * @return 구매 성공 여부
     */
    public boolean buyAdditionalInfo(GameSessionDto session, PortfolioDto portfolio, String infoKey) {
        log.info("Buying additional info: uid={}, infoKey={}", session.getUid(), infoKey);
        
        // 현금 확인
        if (portfolio.getCash() < ADDITIONAL_INFO_COST) {
            log.warn("Insufficient cash for additional info: cash={}", portfolio.getCash());
            return false;
        }
        
        // 라운드당 1개 제한 (간단한 구현)
        // TODO: 실제로는 구매한 정보 키를 세션에 저장하고 중복 방지
        
        // 현금 차감
        portfolio.setCash(portfolio.getCash() - ADDITIONAL_INFO_COST);
        
        log.info("Additional info purchased: infoKey={}, cost={}", infoKey, ADDITIONAL_INFO_COST);
        return true;
    }
    
    /**
     * 라운드별 제공되는 단서 키 목록 반환
     * (실제 단서 내용은 프론트엔드의 game_data.json에서 키로 조회)
     * 
     * @param currentRound 현재 라운드
     * @return 단서 키 목록
     */
    public java.util.List<String> getRoundClueKeys(int currentRound) {
        // TODO: 실제 게임 데이터 기반 구현
        // 현재는 더미 키 반환
        return java.util.List.of(
            "CLUE_PHONE_R" + currentRound + "_01",
            "CLUE_PHONE_R" + currentRound + "_02"
        );
    }
    
    /**
     * 라운드별 제공되는 뉴스 키 목록 반환
     * 
     * @param currentRound 현재 라운드
     * @return 뉴스 키 목록
     */
    public java.util.List<String> getRoundNewsKeys(int currentRound) {
        return java.util.List.of(
            "NEWS_STOCK_01_R" + currentRound,
            "NEWS_STOCK_02_R" + currentRound,
            "NEWS_STOCK_03_R" + currentRound,
            "NEWS_ECONOMY_R" + currentRound,
            "NEWS_FORTUNE_R" + currentRound
        );
    }
    
    /**
     * 라운드별 구매 가능한 심화정보 키 목록 반환
     * (프론트엔드에서 buy-additional-info 호출 시 이 키 사용)
     * 
     * @param currentRound 현재 라운드
     * @param isTutorial 튜토리얼 모드 여부
     * @return 심화정보 키 목록
     */
    public java.util.List<String> getAvailableAdditionalInfoKeys(int currentRound, boolean isTutorial) {
        // 라운드별로 2~3개의 심화정보 제공
        String prefix = isTutorial ? "DEEP_INFO_TUTORIAL_R" : "DEEP_INFO_COMPETITION_R";
        
        java.util.List<String> keys = new java.util.ArrayList<>();
        keys.add(prefix + currentRound + "_01");
        keys.add(prefix + currentRound + "_02");
        
        // 짝수 라운드에는 추가 심화정보
        if (currentRound % 2 == 0) {
            keys.add(prefix + currentRound + "_03");
        }
        
        return keys;
    }
}

