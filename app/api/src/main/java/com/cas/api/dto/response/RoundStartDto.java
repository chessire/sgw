package com.cas.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 라운드 시작 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundStartDto {
    
    /**
     * 뉴스 (주식 3개 + 경제 1개 + 운세 1개)
     */
    private NewsDto news;
    
    /**
     * 경제 팝업 (특정 라운드에만 존재)
     */
    private PopupDto economicPopup;
    
    /**
     * 휴대폰 메시지 (2개)
     */
    private List<MessageDto> phoneMessages;
    
    /**
     * 기업 검색 데이터
     */
    private List<BrowserDataDto> browserData;
    
    /**
     * 시장 변동 결과
     */
    private MarketMovementDto marketMovement;
    
    /**
     * 인생이벤트 (발생 시에만 존재)
     */
    private LifeEventDto lifeEvent;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsDto {
        private List<NewsItemDto> stockNews;      // 주식 뉴스 3개
        private NewsItemDto economicNews;          // 경제 뉴스 1개
        private NewsItemDto todayFortune;          // 오늘의 운세 1개
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsItemDto {
        private String newsKey;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopupDto {
        private String popupKey;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageDto {
        private String messageKey;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrowserDataDto {
        private String companyKey;
        private String pattern;  // "UP" or "DOWN"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketMovementDto {
        private BigDecimal baseRate;       // 현재 기준금리
        private BigDecimal baseRateChange; // 변화량
        private List<StockPriceChangeDto> stockPriceChange;
        private List<FundNavChangeDto> fundNavChange;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockPriceChangeDto {
        private String stockId;
        private BigDecimal changeRate;  // 변동률 (0.04 = 4%)
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundNavChangeDto {
        private String fundId;
        private BigDecimal changeRate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LifeEventDto {
        private String eventKey;        // 이벤트 키 (프론트에서 내용 조회용)
        private String eventType;       // INCOME 또는 EXPENSE
        private Long amount;            // 이벤트 금액
        private Boolean insurableEvent; // 보험 처리 가능 여부
    }
}

