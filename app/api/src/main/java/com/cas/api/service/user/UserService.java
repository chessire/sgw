package com.cas.api.service.user;

import com.cas.api.service.external.TransactionService;
import com.cas.common.core.util.KinfaRunException;
import com.cas.common.infra.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 사용자 관리 Service
 * - 사용자 생성/조회
 * - 닉네임 생성
 * - Redis 캐싱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final TransactionService transactionService;
    private final CacheService cacheService;

    // Redis 키 패턴
    private static final String REDIS_KEY_USER = "user:%s";
    private static final String REDIS_KEY_NICKNAME_COUNTER = "nickname:counter:%d:%d"; // adjective:npc
    private static final int USER_CACHE_TTL = 86400; // 24시간
    private static final int MAX_AUTO_NUMBER = 9999;
    private static final int MAX_NICKNAME_ATTEMPTS = 100; // 최대 시도 횟수

    // NPC 코드
    public static final int NPC_POYONGI = 1;  // 포용이
    public static final int NPC_CHAEUMI = 2;  // 채우미

    // NPC 이름 매핑
    private static final Map<Integer, String> NPC_NAMES = Map.of(
        NPC_POYONGI, "포용이",
        NPC_CHAEUMI, "채우미"
    );

    // 형용사 리스트 (500개)
    private static final List<String> ADJECTIVES = Arrays.asList(
        // 1-10
        "밝은", "용감한", "씩씩한", "활발한", "명랑한", "쾌활한", "당당한", "침착한", "차분한", "온화한",
        // 11-20
        "따뜻한", "친절한", "상냥한", "다정한", "순수한", "맑은", "깨끗한", "정직한", "성실한", "부지런한",
        // 21-30
        "꼼꼼한", "신중한", "조심스런", "배려 깊은", "사려 깊은", "지혜로운", "슬기로운", "영리한", "똑똒한", "총명한",
        // 31-40
        "재빠른", "민첩한", "날쌘", "신속한", "경쾌한", "가벼운", "발랄한", "생기 있는", "활기찬", "힘찬",
        // 41-50
        "건강한", "튼튼한", "든든한", "믿음직한", "의젓한", "야무진", "단단한", "굳센", "강인한", "꿋꿋한",
        // 51-60
        "우직한", "묵묵한", "진지한", "열정적인", "적극적인", "능동적인", "자발적인", "주도적인", "창의적인", "독창적인",
        // 61-70
        "참신한", "신선한", "새로운", "특별한", "독특한", "개성 있는", "매력적인", "멋진", "훌륭한", "뛰어난",
        // 71-80
        "탁월한", "우수한", "출중한", "대단한", "놀라운", "신기한", "재미있는", "즐거운", "유쾌한", "흥미로운",
        // 81-90
        "재치 있는", "익살스런", "장난스런", "천진난만한", "솔직한", "담백한", "소탈한", "겸손한", "검소한", "단정한",
        // 91-100
        "깔끔한", "정갈한", "반듯한", "바른", "올곧은", "곧은", "순진한", "순박한", "소박한", "평온한",
        // 101-110
        "안정된", "고요한", "조용한", "잔잔한", "은은한", "은근한", "수수한", "담담한", "덤덤한", "점잖은",
        // 111-120
        "단아한", "우아한", "품위 있는", "기품 있는", "고상한", "세련된", "고결한", "청렴한", "결백한", "청순한",
        // 121-130
        "예의 바른", "명석한", "지적인", "박식한", "통찰력 있는", "예리한", "통찰하는", "섬세한", "세심한", "치밀한",
        // 131-140
        "끈질긴", "철저한", "완벽한", "성숙한", "능숙한", "노련한", "숙련된", "전문적인", "유연한", "부드러운",
        // 141-150
        "매끄러운", "순조로운", "합리적인", "이성적인", "논리적인", "체계적인", "계획적인", "전략적인", "실용적인", "현실적인",
        // 151-160
        "명확한", "분명한", "확실한", "균형 잡힌", "조화로운", "화합하는", "일관된", "끈기 있는", "변함없는", "한결같은",
        // 161-170
        "활동적인", "역동적인", "에너지 넘치는", "생동감 넘치는", "탄력 있는", "힘 있는", "강력한", "힘센", "박력 있는", "당찬",
        // 171-180
        "패기 있는", "털털한", "기백 있는", "기운찬", "혈기 왕성한", "원기 왕성한", "기운 센", "늠름한", "호탕한", "쿨한",
        // 181-190
        "거침없는", "대범한", "호쾌한", "시원한", "후련한", "상쾌한", "생기 넘치는", "시원스런", "직설적인", "간결한",
        // 191-200
        "간명한", "명쾌한", "확연한", "명료한", "명백한", "투철한", "진중한", "심오한", "사색적인", "앞서가는",
        // 201-210
        "진취적인", "개척적인", "도전적인", "모험적인", "대담한", "과감한", "용맹한", "두려움 없는", "겁 없는", "담력 있는",
        // 211-220
        "배짱 있는", "배포 큰", "깡 있는", "패기 만만한", "자신감 넘치는", "자신 있는", "생기발랄한", "독립적인", "자주적인", "주체적인",
        // 221-230
        "자율적인", "자유로운", "자유분방한", "자유스런", "거리낌 없는", "스스럼없는", "자연스런", "편안한", "여유로운", "느긋한",
        // 231-240
        "평안한", "안심되는", "믿음 가는", "신뢰할 만한", "진실한", "진정한", "진심 어린", "성의 있는", "정성스런", "헌신적인",
        // 241-250
        "충실한", "충성스런", "의리 있는", "흔들림 없는", "확신에 찬", "자신만만한", "의연한", "태연한", "초연한", "초탈한",
        // 251-260
        "달관한", "깨달은", "통달한", "정통한", "능통한", "비범한", "범상치 않은", "특출난", "비상한", "걸출한",
        // 261-270
        "우월한", "이색적인", "색다른", "남다른", "특이한", "별난", "별다른", "유별난", "기이한", "흔치 않은",
        // 271-280
        "희귀한", "보기 드문", "신비로운", "신비스런", "몽환적인", "환상적인", "황홀한", "매혹적인", "열광적인", "열렬한",
        // 281-290
        "불타는", "뜨거운", "정열적인", "격정적인", "격렬한", "불같은", "화끈한", "시원시원한", "통쾌한", "청쾌한",
        // 291-300
        "고귀한", "숭고한", "품격 높은", "위엄 있는", "위풍당당한", "기세등등한", "의기양양한", "자랑스런", "영광스런", "영예로운",
        // 301-310
        "명예로운", "명망 높은", "유명한", "인기 있는", "사랑받는", "호감 가는", "친근한", "가까운", "다정다감한", "정 많은",
        // 311-320
        "인정 많은", "인자한", "베풀줄 아는", "자상한", "나긋나긋한", "온유한", "온순한", "얌전한", "평화로운", "화목한",
        // 321-330
        "화평한", "평정심 있는", "유유자적한", "태평스런", "태평한", "낙천적인", "긍정적인", "희망찬", "희망적인", "유망한",
        // 331-340
        "촉망받는", "기대되는", "주목받는", "눈에 띄는", "돋보이는", "빛나는", "반짝이는", "찬란한", "화려한", "눈부신",
        // 341-350
        "빼어난", "아름다운", "고운", "곱상한", "예쁜", "귀여운", "사랑스런", "앙증맞은", "깜찍한", "아기자기한",
        // 351-360
        "산뜻한", "싱그러운", "풋풋한", "상큼한", "청초한", "청아한", "해맑은", "티 없는", "때 묻지 않은", "순정한",
        // 361-370
        "투명한", "정의로운", "공정한", "공평한", "중립적인", "객관적인", "상식적인", "분별력 있는", "판단력 있는", "식견 있는",
        // 371-380
        "안목 있는", "간파하는", "심미안 있는", "감각 있는", "센스 있는", "멋스러운", "스타일리시한", "기상 넘치는", "기세 좋은", "기운 좋은",
        // 381-390
        "활력 있는", "생명력 넘치는", "생생한", "살아 있는", "힘 넘치는", "에너지 가득한", "파워 넘치는", "강렬한", "인상적인", "임팩트 있는",
        // 391-400
        "인상 깊은", "기억에 남는", "잊을 수 없는", "순발력 있는", "매력 넘치는", "사로잡는", "끌리는", "날렵한", "홀리는", "흡인력 있는",
        // 401-410
        "카리스마 있는", "아낌없이 베푸는", "존재감 강한", "개성 만점인", "혁신적인", "심상치 않은", "고유한", "독자적인", "강단 있는", "유니크한",
        // 411-420
        "독보적인", "유일무이한", "하나뿐인", "놀랄 만한", "경이로운", "기묘한", "꿈결 같은", "몽상적인", "낭만적인", "온정적인",
        // 421-430
        "인간적인", "사교적인", "외향적인", "개방적인", "포용력 있는", "너그러운", "포용하는", "넉넉한", "포근한", "푸근한",
        // 431-440
        "훈훈한", "격의 없는", "다사로운", "살뜰한", "알뜰한", "신의 있는", "의협심 강한", "정의감 강한", "책임감 강한", "사명감 강한",
        // 441-450
        "소신 있는", "뚝심 있는", "집념 있는", "집요한", "인내심 강한", "참을성 많은", "여유 있는", "포용력 넘치는", "아량 넘치는", "기백 넘치는",
        // 451-460
        "패기 넘치는", "열의 넘치는", "의욕 넘치는", "열의 가득한", "의욕 가득한", "정열 가득한", "열정 가득한", "희망 가득한", "기쁨 가득한", "행복한",
        // 461-470
        "신명나는", "신나는", "기대에 찬", "기분 좋은", "청량한", "유능한", "활달한", "개운한", "가뿐한", "믿음직스런",
        // 471-480
        "굳건한", "파워풀한", "따스한", "정감 어린", "유대감 있는", "허물없는", "차별화된", "온정 어린", "진정성 있는", "애틋한",
        // 481-490
        "영특한", "착실한", "건설적인", "절실한", "실력 있는", "양심적인", "순수한 성품의", "올바른", "듬직한", "충직한",
        // 491-500 (부족분 채움)
        "현명한", "지혜로운", "슬기로운", "현실적인", "실천적인", "근면한", "성실한", "꾸준한", "끈기있는", "불굴의"
    );

    /**
     * 사용자 존재 여부 확인
     */
    public Map<String, Object> checkUser(Long mbrSno) {
        log.info("■ UserService.checkUser - mbrSno: {}", mbrSno);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Redis 캐시 먼저 확인
            String redisKey = String.format(REDIS_KEY_USER, mbrSno);
            Map<String, Object> cachedUser = cacheService.getObject(redisKey, Map.class);

            if (cachedUser != null) {
                log.info("■ User found in Redis cache: {}", mbrSno);
                result.put("exists", true);
                result.put("user", cachedUser);
                return result;
            }

            // 2. DB 조회
            HashMap<String, Object> dbResult = transactionService.getGameBasicInfo(mbrSno);

            if (dbResult != null && !dbResult.isEmpty()) {
                log.info("■ User found in DB: {}", mbrSno);

                // Redis에 캐싱
                cacheService.setObject(redisKey, dbResult, USER_CACHE_TTL, TimeUnit.SECONDS);

                result.put("exists", true);
                result.put("user", dbResult);
            } else {
                log.info("■ User not found: {}", mbrSno);
                result.put("exists", false);
                result.put("user", null);
            }

        } catch (KinfaRunException e) {
            log.error("■ Error checking user: {}", e.getMessage());
            result.put("exists", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 사용자 생성 (닉네임 자동 생성 - 형용사, NPC 모두 랜덤)
     * 
     * @param mbrSno 회원일련번호
     */
    public Map<String, Object> createUser(Long mbrSno) {
        log.info("■ UserService.createUser - mbrSno: {}", mbrSno);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 기존 사용자 확인
            Map<String, Object> existingUser = checkUser(mbrSno);
            if ((Boolean) existingUser.get("exists")) {
                log.warn("■ User already exists: {}", mbrSno);
                result.put("success", false);
                result.put("message", "이미 등록된 사용자입니다.");
                result.put("user", existingUser.get("user"));
                return result;
            }

            // 2. 고유한 닉네임 생성 (형용사+NPC 모두 랜덤)
            NicknameResult nicknameResult = generateUniqueNickname(null);
            
            if (nicknameResult == null) {
                log.error("■ Failed to generate unique nickname");
                result.put("success", false);
                result.put("message", "닉네임 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
                return result;
            }

            log.info("■ Generated nickname: {} (NINAM_SNO: {})", nicknameResult.nickname, nicknameResult.ninamSno);

            // 3. DB 저장
            HashMap<String, Object> dbResult = transactionService.createGameBasicInfo(mbrSno, nicknameResult.nickname);

            // 4. Redis 캐싱
            String redisKey = String.format(REDIS_KEY_USER, mbrSno);
            Map<String, Object> userData = new HashMap<>();
            userData.put("MBR_SNO", mbrSno);
            userData.put("NINAM_SNO", nicknameResult.ninamSno);
            userData.put("NINAM_NM", nicknameResult.nickname);
            userData.put("NPC_NO", nicknameResult.npcNo);
            userData.put("ACHM_NO", 0);
            userData.put("ADJECTIVE_INDEX", nicknameResult.adjectiveIndex);
            userData.put("AUTO_NUMBER", nicknameResult.autoNumber);

            cacheService.setObject(redisKey, userData, USER_CACHE_TTL, TimeUnit.SECONDS);

            result.put("success", true);
            result.put("message", "사용자가 생성되었습니다.");
            result.put("user", userData);
            result.put("dbResult", dbResult);

        } catch (KinfaRunException e) {
            log.error("■ Error creating user: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "사용자 생성 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 닉네임 랜덤 변경 (형용사 + NPC 모두 랜덤)
     */
    public Map<String, Object> changeNickname(Long mbrSno) {
        log.info("■ UserService.changeNickname - mbrSno: {}", mbrSno);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 고유한 닉네임 생성 (NPC도 랜덤)
            NicknameResult nicknameResult = generateUniqueNickname(null);
            
            if (nicknameResult == null) {
                log.error("■ Failed to generate unique nickname");
                result.put("success", false);
                result.put("message", "닉네임 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
                return result;
            }

            log.info("■ New nickname: {} (NINAM_SNO: {}, NPC: {})", 
                nicknameResult.nickname, nicknameResult.ninamSno, nicknameResult.npcName);

            // 2. DB 업데이트 (별도 함수 필요시 추가)
            // transactionService.updateNickname(mbrSno, nicknameResult.nickname);

            // 3. Redis 업데이트
            String redisKey = String.format(REDIS_KEY_USER, mbrSno);
            Map<String, Object> cachedUser = cacheService.getObject(redisKey, Map.class);

            if (cachedUser != null) {
                cachedUser.put("NINAM_SNO", nicknameResult.ninamSno);
                cachedUser.put("NINAM_NM", nicknameResult.nickname);
                cachedUser.put("NPC_NO", nicknameResult.npcNo);
                cachedUser.put("ADJECTIVE_INDEX", nicknameResult.adjectiveIndex);
                cachedUser.put("AUTO_NUMBER", nicknameResult.autoNumber);
                cacheService.setObject(redisKey, cachedUser, USER_CACHE_TTL, TimeUnit.SECONDS);
            }

            result.put("success", true);
            result.put("nickname", nicknameResult.nickname);
            result.put("ninamSno", nicknameResult.ninamSno);
            result.put("npcNo", nicknameResult.npcNo);
            result.put("npcName", nicknameResult.npcName);

        } catch (Exception e) {
            log.error("■ Error changing nickname: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "닉네임 변경 중 오류가 발생했습니다.");
        }

        return result;
    }

    /**
     * 닉네임 생성 결과 클래스
     */
    private static class NicknameResult {
        int adjectiveIndex;
        int npcNo;
        int autoNumber;
        Long ninamSno;
        String nickname;
        String npcName;
    }

    /**
     * 고유한 닉네임 생성 (형용사+NPC 조합별 카운터 관리)
     * 
     * @param fixedNpcNo 고정 NPC 번호 (null이면 랜덤)
     * @return 닉네임 생성 결과, 실패 시 null
     */
    private NicknameResult generateUniqueNickname(Integer fixedNpcNo) {
        Random random = new Random();
        Set<String> triedCombinations = new HashSet<>();
        
        for (int attempt = 0; attempt < MAX_NICKNAME_ATTEMPTS; attempt++) {
            // 1. 형용사와 NPC 선택
            int adjectiveIndex = random.nextInt(ADJECTIVES.size());
            int npcNo = (fixedNpcNo != null) ? fixedNpcNo : (random.nextInt(2) + 1);
            
            String combinationKey = adjectiveIndex + ":" + npcNo;
            
            // 이미 시도한 조합이면 스킵
            if (triedCombinations.contains(combinationKey)) {
                continue;
            }
            triedCombinations.add(combinationKey);
            
            // 2. 해당 조합의 카운터 확인 및 증가
            String counterKey = String.format(REDIS_KEY_NICKNAME_COUNTER, adjectiveIndex, npcNo);
            Long counter = cacheService.increment(counterKey);
            
            if (counter == null) {
                // Redis 연결 실패 시 랜덤 생성 (fallback)
                log.warn("■ Redis counter failed, using random number");
                counter = (long) (random.nextInt(MAX_AUTO_NUMBER) + 1);
            }
            
            // 3. 9999 초과 시 다른 조합 선택
            if (counter > MAX_AUTO_NUMBER) {
                log.info("■ Nickname combination {}:{} is full (counter={}), trying another...", 
                    adjectiveIndex, npcNo, counter);
                continue;
            }
            
            // 4. 닉네임 생성 성공
            NicknameResult result = new NicknameResult();
            result.adjectiveIndex = adjectiveIndex;
            result.npcNo = npcNo;
            result.autoNumber = counter.intValue();
            result.ninamSno = generateNinamSno(adjectiveIndex, npcNo, result.autoNumber);
            result.npcName = NPC_NAMES.get(npcNo);
            result.nickname = ADJECTIVES.get(adjectiveIndex) + " " + result.npcName;
            
            log.debug("■ Generated unique nickname: {} (attempt {})", result.nickname, attempt + 1);
            return result;
        }
        
        // 모든 시도 실패
        log.error("■ Failed to generate unique nickname after {} attempts", MAX_NICKNAME_ATTEMPTS);
        return null;
    }

    /**
     * NINAM_SNO 생성
     * 형용사3자리(000-499) + NPC2자리(01-02) + 자동4자리(0001-9999)
     * 예: 599010001
     */
    private Long generateNinamSno(int adjectiveIndex, int npcNo, int autoNumber) {
        // 형용사 인덱스: 000-499 (3자리)
        // NPC: 01-02 (2자리)
        // 자동 숫자: 0001-9999 (4자리)
        return (long) adjectiveIndex * 1000000 + (long) npcNo * 10000 + autoNumber;
    }


    /**
     * 형용사 인덱스로 형용사 조회
     */
    public String getAdjectiveByIndex(int index) {
        if (index >= 0 && index < ADJECTIVES.size()) {
            return ADJECTIVES.get(index);
        }
        return ADJECTIVES.get(0);
    }

    /**
     * NPC 번호로 NPC 이름 조회
     */
    public String getNpcName(int npcNo) {
        return NPC_NAMES.getOrDefault(npcNo, "포용이");
    }

    /**
     * Redis에서 사용자 정보 로드
     */
    public Map<String, Object> loadUserFromCache(Long mbrSno) {
        String redisKey = String.format(REDIS_KEY_USER, mbrSno);
        return cacheService.getObject(redisKey, Map.class);
    }

    /**
     * Redis에 사용자 정보 저장
     */
    public void saveUserToCache(Long mbrSno, Map<String, Object> userData) {
        String redisKey = String.format(REDIS_KEY_USER, mbrSno);
        cacheService.setObject(redisKey, userData, USER_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * Redis에서 사용자 정보 삭제
     */
    public void removeUserFromCache(Long mbrSno) {
        String redisKey = String.format(REDIS_KEY_USER, mbrSno);
        cacheService.delete(redisKey);
    }
}

