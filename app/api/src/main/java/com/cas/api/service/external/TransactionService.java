package com.cas.api.service.external;

import com.cas.common.core.util.HttpHandler;
import com.cas.common.core.util.KinfaRunException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 게임 데이터 트랜잭션 서비스
 * - 외부 인프라(DB)와 MCI 통신하여 게임 데이터를 저장/조회
 * - 모든 조회는 MBR_SNO(회원일련번호)를 기준으로 하며, KMHAD055M과 JOIN하여 NINAM_SNO를 매핑
 * 
 * MCI 인터페이스 호출 방식:
 * - httpHandler.postToMCI(param, "인터페이스ID", "화면ID")
 * - 응답에서 resData 추출
 * 
 * 환경 설정:
 * - app.use-external-db=true: Production 환경, 실제 MCI 통신
 * - app.use-external-db=false: Development 환경, Mock 데이터 반환
 * 
 * 제외된 테이블 (별도 인증 채널에서 관리):
 * - KMHAD052M (고객인증)
 * - KMHAD053M (고객인증여부이력)
 * - KMHAD056M (고객세션) - JWT 토큰 미사용
 * - KMHAD064M (버전관리)
 * 
 * 인터페이스 ID 네이밍 규칙:
 * - CKC_M_MCO_S_MCO00054 : KMHAD054M (로그인)
 * - CKC_M_MCO_S_MCO00055 : KMHAD055M (게임기본정보)
 * - CKC_M_MCO_S_MCO00057 : KMHAD057M (튜토리얼정보)
 * - CKC_M_MCO_S_MCO00058 : KMHAD058M (튜토리얼진행정보)
 * - CKC_M_MCO_S_MCO00059 : KMHAD059M (튜토리얼결과)
 * - CKC_M_MCO_S_MCO00060 : KMHAD060M (경쟁정보)
 * - CKC_M_MCO_S_MCO00061 : KMHAD061M (경쟁진행정보)
 * - CKC_M_MCO_S_MCO00062 : KMHAD062M (경쟁결과)
 * - CKC_M_MCO_S_MCO00063 : KMHAD063M (학습정보)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final HttpHandler httpHandler;
    
    @Value("${app.use-external-db:false}")
    private boolean useExternalDb;
    
    /*
     * ================================================================================
     * MCI 인터페이스 ID 상수 정의
     * ================================================================================
     * 
     * ※ 인터페이스 ID는 MCI 시스템에서 발급받아야 합니다.
     * ※ 발급 후 "TODO_XXX_XXX" 값을 실제 인터페이스 ID로 교체하세요.
     * 
     * 명명 규칙: IF_{테이블번호}_{작업타입}
     * - 테이블번호: KMHAD0XXM의 XX 부분 (054, 055, 057 등)
     * - 작업타입: INSERT, UPDATE, SELECT, LIST, RANKING 등
     * 
     * 예시: CKC_M_MCO_S_MCO00054 형태로 발급됨
     * ================================================================================
     */
    
    // ─────────────────────────────────────────────────────────────────────────────
    // [KMHAD054M] 고객 로그인 테이블
    // ─────────────────────────────────────────────────────────────────────────────
    /** 로그인 기록 생성 - AuthService.login() */
    private static final String IF_054_INSERT = "CKC_M_MCO_S_MCO00055";
    /** 로그아웃 처리 - AuthService.logout() */
    private static final String IF_054_UPDATE = "CKC_M_MCO_S_MCO00058";
    // private static final String IF_054_SELECT = "TODO_054_SELECT";  // 최근 로그인 정보 조회
    
    
    // ─────────────────────────────────────────────────────────────────────────────
    // [KMHAD055M] 게임 기본 정보 테이블 (닉네임, 업적)
    // ─────────────────────────────────────────────────────────────────────────────
    /** 게임 기본 정보 생성 - UserService.createUser() */
    private static final String IF_055_INSERT = "CKC_M_MCO_S_MCO00059";
    /** 업적 업데이트 - GameLoadService.saveAchievement() */
    private static final String IF_055_UPDATE = "CKC_M_MCO_S_MCO00060";
    /** 게임 기본 정보 조회 - GameLoadService, UserService */
    private static final String IF_055_SELECT = "CKC_M_MCO_S_MCO00061";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // [KMHAD057M] 튜토리얼 정보 테이블
    // ─────────────────────────────────────────────────────────────────────────────
    /** 튜토리얼 시작 - GameLoadService.startTutorial() */
    private static final String IF_057_INSERT = "CKC_M_MCO_S_MCO00062";
    // private static final String IF_057_SELECT = "TODO_057_SELECT";  // 튜토리얼 정보 조회 (단건)
    /** 튜토리얼 목록 조회 - GameLoadService.loadTutorialData() */
    private static final String IF_057_LIST = "CKC_M_MCO_S_MCO00063";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // [KMHAD058M] 튜토리얼 진행 정보 테이블 (라운드별)
    // ─────────────────────────────────────────────────────────────────────────────
    /** 튜토리얼 라운드 저장 - GameLoadService.saveRoundProgress() */
    private static final String IF_058_INSERT = "CKC_M_MCO_S_MCO00064";
    // private static final String IF_058_UPDATE_PORTFOLIO = "TODO_058_UP";  // 포트폴리오 업데이트 (라운드 중간 저장용)
    // private static final String IF_058_UPDATE_LOG = "TODO_058_UL";        // 로그 업데이트
    // private static final String IF_058_SELECT = "TODO_058_SELECT";        // 특정 라운드 조회 (단건)
    /** 튜토리얼 전체 라운드 조회 - GameLoadService.loadTutorialData() */
    private static final String IF_058_SELECT_ALL = "CKC_M_MCO_S_MCO00065";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // [KMHAD059M] 튜토리얼 결과 테이블
    // ─────────────────────────────────────────────────────────────────────────────
    /** 튜토리얼 결과 저장 - GameLoadService.saveGameResult() */
    private static final String IF_059_INSERT = "CKC_M_MCO_S_MCO00066";
    // private static final String IF_059_SELECT = "TODO_059_SELECT";  // 튜토리얼 결과 조회 (단건)
    // private static final String IF_059_LIST = "TODO_059_LIST";      // 튜토리얼 결과 목록
    
    // ─────────────────────────────────────────────────────────────────────────────
    // [KMHAD060M] 경쟁 정보 테이블
    // ─────────────────────────────────────────────────────────────────────────────
    /** 경쟁 모드 시작 - GameLoadService.startCompetition() */
    private static final String IF_060_INSERT = "CKC_M_MCO_MCO00067";
    // private static final String IF_060_SELECT = "TODO_060_SELECT";  // 경쟁 정보 조회 (단건)
    /** 경쟁 목록 조회 - GameLoadService.loadCompetitionData() */
    private static final String IF_060_LIST = "CKC_M_MCO_S_MCO00068";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // [KMHAD061M] 경쟁 진행 정보 테이블 (라운드별)
    // ─────────────────────────────────────────────────────────────────────────────
    /** 경쟁 라운드 저장 - GameLoadService.saveRoundProgress() */
    private static final String IF_061_INSERT = "CKC_M_MCO_S_MCO00069";
    // private static final String IF_061_UPDATE_PORTFOLIO = "TODO_061_UP";  // 포트폴리오 업데이트 (라운드 중간 저장용)
    // private static final String IF_061_UPDATE_LOG = "TODO_061_UL";        // 로그 업데이트
    // private static final String IF_061_SELECT = "TODO_061_SELECT";        // 특정 라운드 조회 (단건)
    /** 경쟁 전체 라운드 조회 - GameLoadService.loadCompetitionData() */
    private static final String IF_061_SELECT_ALL = "CKC_M_MCO_S_MCO00070";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // [KMHAD062M] 경쟁 결과 테이블 (랭킹)
    // ─────────────────────────────────────────────────────────────────────────────
    /** 경쟁 결과 저장 - GameLoadService.saveGameResult() */
    private static final String IF_062_INSERT = "CKC_M_MCO_S_MCO00071";
    // private static final String IF_062_SELECT = "TODO_062_SELECT";  // 경쟁 결과 조회 (단건)
    // private static final String IF_062_LIST = "TODO_062_LIST";      // 경쟁 결과 목록
    /** 전체 랭킹 조회 - RankingService.getRanking() */
    private static final String IF_062_RANKING = "CKC_M_MCO_S_MCO00072";
    /** 월간 랭킹 조회 - MonthlyRankingService.getMonthlyRanking() */
    private static final String IF_062_MONTHLY_RANKING = "TODO_062_MONTHLY";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // [KMHAD063M] 학습 정보 테이블 (영상/퀴즈)
    // ─────────────────────────────────────────────────────────────────────────────
    /** 학습 정보 저장 - GameLoadService.saveLearningInfo() */
    private static final String IF_063_INSERT = "CKC_M_MCO_MCO00073";
    /** 튜토리얼 학습 조회 - GameLoadService.loadTutorialData() */
    private static final String IF_063_SELECT_TUTORIAL = "CKC_M_MCO_MCO00074";
    /** 경쟁 학습 조회 - GameLoadService.loadCompetitionData() */
    private static final String IF_063_SELECT_COMPETITION = "CKC_M_MCO_S_MCO00075";
    // private static final String IF_063_STATS = "TODO_063_STATS";    // 학습 통계 조회

    // ─────────────────────────────────────────────────────────────────────────────
    // 공통 상수
    // ─────────────────────────────────────────────────────────────────────────────
    /** 화면 ID - MCI 호출 시 전달 (발급 후 설정) */
    private static final String SCREEN_ID = "";
    
    /** Operation 타입: INSERT - Mock 응답 생성용 */
    private static final String OP_INSERT = "INSERT";
    /** Operation 타입: UPDATE - Mock 응답 생성용 */
    private static final String OP_UPDATE = "UPDATE";
    /** Operation 타입: SELECT (단건 조회) - Mock 응답 생성용 */
    private static final String OP_SELECT = "SELECT";
    /** Operation 타입: LIST (목록 조회) - Mock 응답 생성용 */
    private static final String OP_LIST = "LIST";
    
    /**
     * 외부 DB 사용 여부 확인
     */
    public boolean isUseExternalDb() {
        return useExternalDb;
    }
    
    /**
     * MCI 호출 wrapper
     * - useExternalDb=true: 실제 MCI 호출 후 resData 추출
     * - useExternalDb=false: Mock 응답 반환
     * 
     * @param param 요청 파라미터
     * @param ifId MCI 인터페이스 ID
     * @param opType Operation 타입 (INSERT, UPDATE, SELECT, LIST)
     */
    @SuppressWarnings("unchecked")
    private HashMap<String, Object> executeRequest(HashMap<String, Object> param, String ifId, String opType) throws KinfaRunException {
        if (!useExternalDb) {
            log.debug("[MOCK MODE] Skipping MCI call: {} ({}) with params: {}", ifId, opType, param);
            return createMockResponse(ifId, opType, param);
        }
        
        log.info("[MCI] Calling interface: {} with params: {}", ifId, param);
        
        // MCI 호출
        HashMap<String, Object> jsonObj = httpHandler.postToMCI(param, ifId, SCREEN_ID);
        log.info("[MCI] Response jsonObj: {}", jsonObj);
        
        // resData 추출
        HashMap<String, Object> resData = null;
        if (jsonObj != null && jsonObj.get("resData") != null) {
            resData = (HashMap<String, Object>) jsonObj.get("resData");
        } else {
            resData = jsonObj; // resData가 없으면 전체 응답 반환
        }
        
        log.info("[MCI] Extracted resData: {}", resData);
        return resData;
    }
    
    /**
     * Mock 응답 생성
     * - Development 환경에서 사용
     * 
     * @param ifId MCI 인터페이스 ID
     * @param opType Operation 타입 (INSERT, UPDATE, SELECT, LIST)
     * @param param 요청 파라미터
     */
    private HashMap<String, Object> createMockResponse(String ifId, String opType, HashMap<String, Object> param) {
        HashMap<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("success", true);
        mockResponse.put("mock", true);
        mockResponse.put("ifId", ifId);
        mockResponse.put("opType", opType);
        mockResponse.put("requestParams", param);
        
        // Operation 타입별 Mock 데이터 생성
        switch (opType) {
            case OP_INSERT:
                mockResponse.put("insertedId", System.currentTimeMillis());
                mockResponse.put("affectedRows", 1);
                break;
            case OP_UPDATE:
                mockResponse.put("updatedCount", 1);
                mockResponse.put("affectedRows", 1);
                break;
            case OP_SELECT:
                mockResponse.put("data", new HashMap<String, Object>());
                break;
            case OP_LIST:
                mockResponse.put("data", new java.util.ArrayList<>());
                mockResponse.put("totalCount", 0);
                break;
            default:
                mockResponse.put("data", new HashMap<String, Object>());
        }
        
        log.info("[MOCK] Generated mock response for {} ({}): {}", ifId, opType, mockResponse);
        return mockResponse;
    }

    // ========================================
    // 1. 고객 로그인 (KMHAD054M)
    // ========================================

    /**
     * 로그인 기록 생성
     * 
     * MyBatis SQL:
     * INSERT INTO KMHAD054M (
     *     MBR_SNO, LGN_DT, IDVRF_MTCD, LGN_SECD, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT,
     *     LAST_CHG_GUID, LAST_CHG_USR_ID, LAST_CHG_DT
     * ) VALUES (
     *     #{MBR_SNO}, SYSDATE, #{IDVRF_MTCD}, #{LGN_SECD}, 'N',
     *     'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     'KFWEB', SYSDATE,
     *     'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     'KFWEB', SYSDATE
     * )
     */
    public HashMap<String, Object> createLoginLog(Long mbrSno, String idvrfMtcd, 
                                                   String lgnSecd) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("IDVRF_MTCD", idvrfMtcd);
        param.put("LGN_SECD", lgnSecd);
        return executeRequest(param, IF_054_INSERT, OP_INSERT);
    }

    /**
     * 로그아웃 처리
     * 
     * MyBatis SQL:
     * UPDATE KMHAD054M
     * SET LOUT_DT = SYSDATE,
     *     LAST_CHG_GUID = 'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     LAST_CHG_USR_ID = 'KFWEB',
     *     LAST_CHG_DT = SYSDATE
     * WHERE MBR_SNO = #{MBR_SNO}
     *   AND LGN_DT = TO_DATE(#{LGN_DT}, 'YYYY-MM-DD HH24:MI:SS')
     *   AND DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> updateLogout(Long mbrSno, String lgnDt) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("LGN_DT", lgnDt);
        return executeRequest(param, IF_054_UPDATE, OP_UPDATE);
    }

    /*
     * [미사용] 최근 로그인 정보 조회
     * 
     * MyBatis SQL:
     * SELECT MBR_SNO AS "mbrSno", LGN_DT AS "lgnDt", IDVRF_MTCD AS "idvrfMtcd",
     *        LGN_SECD AS "lgnSecd", LOUT_DT AS "loutDt"
     * FROM KMHAD054M WHERE MBR_SNO = #{MBR_SNO} AND DTA_DEL_YN = 'N'
     * ORDER BY LGN_DT DESC FETCH FIRST 1 ROWS ONLY
     *
    public HashMap<String, Object> getLastLogin(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, IF_054_SELECT, OP_SELECT);
    }
    */

    // ========================================
    // 2. 게임 기본 정보 (KMHAD055M) - 닉네임, 업적
    // ========================================

    /**
     * 게임 기본 정보 조회 (회원번호로 조회)
     * 
     * MyBatis SQL:
     * SELECT NINAM_SNO AS "ninamSno",
     *        MBR_SNO AS "mbrSno",
     *        NINAM_NM AS "ninamNm",
     *        ACHM_NO AS "achmNo"
     * FROM KMHAD055M
     * WHERE MBR_SNO = #{MBR_SNO}
     *   AND DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> getGameBasicInfo(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, IF_055_SELECT, OP_SELECT);
    }

    /**
     * 게임 기본 정보 생성 (닉네임 생성)
     * 
     * MyBatis SQL:
     * INSERT INTO KMHAD055M (
     *     NINAM_SNO, MBR_SNO, NINAM_NM, ACHM_NO, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT,
     *     LAST_CHG_GUID, LAST_CHG_USR_ID, LAST_CHG_DT
     * ) VALUES (
     *     KMHAD055M_SEQ.NEXTVAL, #{MBR_SNO}, #{NINAM_NM}, #{ACHM_NO}, 'N',
     *     'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     'KFWEB', SYSDATE,
     *     'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     'KFWEB', SYSDATE
     * )
     */
    public HashMap<String, Object> createGameBasicInfo(Long mbrSno, String ninamNm) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("NINAM_NM", ninamNm);
        param.put("ACHM_NO", 0);
        return executeRequest(param, IF_055_INSERT, OP_INSERT);
    }

    /**
     * 업적 업데이트
     * 
     * MyBatis SQL:
     * UPDATE KMHAD055M
     * SET ACHM_NO = #{ACHM_NO},
     *     LAST_CHG_GUID = 'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     LAST_CHG_USR_ID = 'KFWEB',
     *     LAST_CHG_DT = SYSDATE
     * WHERE MBR_SNO = #{MBR_SNO}
     *   AND DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> updateAchievement(Long mbrSno, Long achmNo) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("ACHM_NO", achmNo);
        return executeRequest(param, IF_055_UPDATE, OP_UPDATE);
    }

    // ========================================
    // 3. 튜토리얼 정보 (KMHAD057M)
    // ========================================

    /**
     * 튜토리얼 시작 (정보 생성)
     * 
     * MyBatis SQL:
     * INSERT INTO KMHAD057M (
     *     TTRL_SNO, NINAM_SNO, FIPAT_NM, RNDM_NO, MM_AVRG_INCME_AMT, 
     *     FIX_EXPND_AMT, NPC_NO, LOG_CTNS, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     KMHAD057M_SEQ.NEXTVAL, 
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = #{MBR_SNO} AND DTA_DEL_YN = 'N'),
     *     #{FIPAT_NM}, #{RNDM_NO}, #{MM_AVRG_INCME_AMT},
     *     #{FIX_EXPND_AMT}, #{NPC_NO}, #{LOG_CTNS}, 'N',
     *     'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     'KFWEB', SYSDATE
     * )
     */
    public HashMap<String, Object> createTutorial(Long mbrSno, String fipatNm, Integer rndmNo,
                                                   Long mmAvrgIncmeAmt, Long fixExpndAmt,
                                                   Integer npcNo, String logCtns) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("FIPAT_NM", fipatNm);
        param.put("RNDM_NO", rndmNo);
        param.put("MM_AVRG_INCME_AMT", mmAvrgIncmeAmt);
        param.put("FIX_EXPND_AMT", fixExpndAmt);
        param.put("NPC_NO", npcNo);
        param.put("LOG_CTNS", logCtns);
        return executeRequest(param, IF_057_INSERT, OP_INSERT);
    }

    /*
     * [미사용] 튜토리얼 정보 조회 (단건)
     *
    public HashMap<String, Object> getTutorial(Long ttrlSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        return executeRequest(param, IF_057_SELECT, OP_SELECT);
    }
    */

    /**
     * 사용자의 튜토리얼 목록 조회 (회원번호로 조회)
     * 
     * MyBatis SQL:
     * SELECT T.TTRL_SNO, T.NINAM_SNO, G.MBR_SNO, T.FIPAT_NM, T.RNDM_NO, 
     *        T.MM_AVRG_INCME_AMT, T.FIX_EXPND_AMT, T.NPC_NO, T.FIRST_CRT_DT
     * FROM KMHAD057M T
     * INNER JOIN KMHAD055M G ON T.NINAM_SNO = G.NINAM_SNO
     * WHERE G.MBR_SNO = #{MBR_SNO}
     *   AND T.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY T.FIRST_CRT_DT DESC
     */
    public HashMap<String, Object> getTutorialListByUser(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, IF_057_LIST, OP_LIST);
    }

    // ========================================
    // 4. 튜토리얼 진행 정보 (KMHAD058M) - 라운드별
    // ========================================

    /**
     * 튜토리얼 라운드 시작 (진행 정보 생성)
     * 
     * MyBatis SQL:
     * INSERT INTO KMHAD058M (
     *     TTRL_SNO, TTRL_RND_NO, MBR_SNO, LGN_DT, NINAM_SNO,
     *     TOT_ASST_AMT, LON_AMT, CASH_PTFLO_AMT, DPSIT_PTFLO_AMT, ISV_PTFLO_AMT,
     *     ANNTY_PTFLO_AMT, STOCK_PTFLO_AMT, FUND_PTFLO_AMT, BOND_PTFLO_AMT,
     *     CASH_PTFLO_EVL_AMT, DPSIT_PTFLO_EVL_AMT, ISV_PTFLO_EVL_AMT,
     *     ANNTY_PTFLO_EVL_AMT, STOCK_PTFLO_EVL_AMT, FUND_PTFLO_EVL_AMT, BOND_PTFLO_EVL_AMT,
     *     PTFLO_CPST_CTNS, LOG_CTNS, ACTI_TIME, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     #{TTRL_SNO}, #{TTRL_RND_NO}, #{MBR_SNO}, TO_DATE(#{LGN_DT}, 'YYYY-MM-DD HH24:MI:SS'),
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = #{MBR_SNO} AND DTA_DEL_YN = 'N'),
     *     #{TOT_ASST_AMT}, #{LON_AMT}, #{CASH_PTFLO_AMT}, #{DPSIT_PTFLO_AMT}, #{ISV_PTFLO_AMT},
     *     #{ANNTY_PTFLO_AMT}, #{STOCK_PTFLO_AMT}, #{FUND_PTFLO_AMT}, #{BOND_PTFLO_AMT},
     *     #{CASH_PTFLO_EVL_AMT}, #{DPSIT_PTFLO_EVL_AMT}, #{ISV_PTFLO_EVL_AMT},
     *     #{ANNTY_PTFLO_EVL_AMT}, #{STOCK_PTFLO_EVL_AMT}, #{FUND_PTFLO_EVL_AMT}, #{BOND_PTFLO_EVL_AMT},
     *     #{PTFLO_CPST_CTNS}, #{LOG_CTNS}, #{ACTI_TIME}, 'N',
     *     'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     'KFWEB', SYSDATE
     * )
     */
    public HashMap<String, Object> createTutorialRound(Long ttrlSno, Integer ttrlRndNo,
                                                        Long mbrSno, String lgnDt,
                                                        Map<String, Object> portfolioData) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        param.put("TTRL_RND_NO", ttrlRndNo);
        param.put("MBR_SNO", mbrSno);
        param.put("LGN_DT", lgnDt);
        param.putAll(portfolioData);
        return executeRequest(param, IF_058_INSERT, OP_INSERT);
    }

    /*
     * [미사용] 튜토리얼 라운드 조회 (단건)
     *
    public HashMap<String, Object> getTutorialRound(Long ttrlSno, Integer ttrlRndNo) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        param.put("TTRL_RND_NO", ttrlRndNo);
        return executeRequest(param, IF_058_SELECT, OP_SELECT);
    }
    */

    /**
     * 튜토리얼 전체 라운드 조회
     * 
     * MyBatis SQL:
     * SELECT P.TTRL_SNO, P.TTRL_RND_NO, P.MBR_SNO, P.TOT_ASST_AMT, P.LON_AMT,
     *        P.CASH_PTFLO_AMT, P.DPSIT_PTFLO_AMT, P.ISV_PTFLO_AMT,
     *        P.ANNTY_PTFLO_AMT, P.STOCK_PTFLO_AMT, P.FUND_PTFLO_AMT, P.BOND_PTFLO_AMT,
     *        P.CASH_PTFLO_EVL_AMT, P.DPSIT_PTFLO_EVL_AMT, P.ISV_PTFLO_EVL_AMT,
     *        P.ANNTY_PTFLO_EVL_AMT, P.STOCK_PTFLO_EVL_AMT, P.FUND_PTFLO_EVL_AMT, P.BOND_PTFLO_EVL_AMT,
     *        P.PTFLO_CPST_CTNS, P.LOG_CTNS, P.ACTI_TIME
     * FROM KMHAD058M P
     * WHERE P.TTRL_SNO = #{TTRL_SNO}
     *   AND P.DTA_DEL_YN = 'N'
     * ORDER BY P.TTRL_RND_NO ASC
     */
    public HashMap<String, Object> getTutorialAllRounds(Long ttrlSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        return executeRequest(param, IF_058_SELECT_ALL, OP_LIST);
    }

    /*
     * [미사용] 튜토리얼 라운드 포트폴리오 업데이트
     *
    public HashMap<String, Object> updateTutorialRoundPortfolio(Long ttrlSno, Integer ttrlRndNo,
                                                                 Map<String, Object> portfolioData) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        param.put("TTRL_RND_NO", ttrlRndNo);
        param.putAll(portfolioData);
        return executeRequest(param, IF_058_UPDATE_PORTFOLIO, OP_UPDATE);
    }
    */

    /*
     * [미사용] 튜토리얼 라운드 로그 업데이트
     *
    public HashMap<String, Object> updateTutorialRoundLog(Long ttrlSno, Integer ttrlRndNo,
                                                           String logCtns, String actiTime) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        param.put("TTRL_RND_NO", ttrlRndNo);
        param.put("LOG_CTNS", logCtns);
        param.put("ACTI_TIME", actiTime);
        return executeRequest(param, IF_058_UPDATE_LOG, OP_UPDATE);
    }
    */

    // ========================================
    // 5. 튜토리얼 결과 (KMHAD059M)
    // ========================================

    /**
     * 튜토리얼 결과 저장
     * 
     * MyBatis SQL:
     * INSERT INTO KMHAD059M (
     *     NINAM_SNO, TTRL_SNO, TTRL_MODE_SCR, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = #{MBR_SNO} AND DTA_DEL_YN = 'N'),
     *     #{TTRL_SNO}, #{TTRL_MODE_SCR}, 'N',
     *     'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     'KFWEB', SYSDATE
     * )
     */
    public HashMap<String, Object> saveTutorialResult(Long mbrSno, Long ttrlSno, 
                                                       Long ttrlModeScr) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("TTRL_SNO", ttrlSno);
        param.put("TTRL_MODE_SCR", ttrlModeScr);
        return executeRequest(param, IF_059_INSERT, OP_INSERT);
    }

    /*
     * [미사용] 튜토리얼 결과 조회 (단건)
     *
    public HashMap<String, Object> getTutorialResult(Long ttrlSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        return executeRequest(param, IF_059_SELECT, OP_SELECT);
    }
    */

    /*
     * [미사용] 사용자 튜토리얼 결과 목록
     *
    public HashMap<String, Object> getTutorialResultsByUser(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, IF_059_LIST, OP_LIST);
    }
    */

    // ========================================
    // 6. 경쟁 정보 (KMHAD060M)
    // ========================================

    /**
     * 경쟁 모드 시작 (정보 생성)
     * 
     * MyBatis SQL:
     * INSERT INTO KMHAD060M (
     *     CMPTT_SNO, NINAM_SNO, FIPAT_NM, RNDM_NO, NPC_NO, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     KMHAD060M_SEQ.NEXTVAL, 
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = #{MBR_SNO} AND DTA_DEL_YN = 'N'),
     *     #{FIPAT_NM}, #{RNDM_NO}, #{NPC_NO}, 'N',
     *     'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     'KFWEB', SYSDATE
     * )
     */
    public HashMap<String, Object> createCompetition(Long mbrSno, String fipatNm,
                                                      Integer rndmNo, Integer npcNo) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("FIPAT_NM", fipatNm);
        param.put("RNDM_NO", rndmNo);
        param.put("NPC_NO", npcNo);
        return executeRequest(param, IF_060_INSERT, OP_INSERT);
    }

    /*
     * [미사용] 경쟁 정보 조회 (단건)
     *
    public HashMap<String, Object> getCompetition(Long cmpttSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        return executeRequest(param, IF_060_SELECT, OP_SELECT);
    }
    */

    /**
     * 사용자의 경쟁 목록 조회 (회원번호로 조회)
     * 
     * MyBatis SQL:
     * SELECT C.CMPTT_SNO, C.NINAM_SNO, G.MBR_SNO, C.FIPAT_NM, C.RNDM_NO, C.NPC_NO, C.FIRST_CRT_DT
     * FROM KMHAD060M C
     * INNER JOIN KMHAD055M G ON C.NINAM_SNO = G.NINAM_SNO
     * WHERE G.MBR_SNO = #{MBR_SNO}
     *   AND C.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY C.FIRST_CRT_DT DESC
     */
    public HashMap<String, Object> getCompetitionListByUser(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, IF_060_LIST, OP_LIST);
    }

    // ========================================
    // 7. 경쟁 진행 정보 (KMHAD061M) - 라운드별
    // ========================================

    /**
     * 경쟁 라운드 시작 (진행 정보 생성)
     * 
     * MyBatis SQL:
     * INSERT INTO KMHAD061M (
     *     CMPTT_SNO, CMPTT_RND_NO, MBR_SNO, LGN_DT, NINAM_SNO,
     *     TOT_ASST_AMT, LON_AMT, CASH_PTFLO_AMT, DPSIT_PTFLO_AMT, ISV_PTFLO_AMT,
     *     ANNTY_PTFLO_AMT, STOCK_PTFLO_AMT, FUND_PTFLO_AMT, BOND_PTFLO_AMT,
     *     CASH_PTFLO_EVL_AMT, DPSIT_PTFLO_EVL_AMT, ISV_PTFLO_EVL_AMT,
     *     ANNTY_PTFLO_EVL_AMT, STOCK_PTFLO_EVL_AMT, FUND_PTFLO_EVL_AMT, BOND_PTFLO_EVL_AMT,
     *     PTFLO_CPST_CTNS, LOG_CTNS, ACTI_TIME, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     #{CMPTT_SNO}, #{CMPTT_RND_NO}, #{MBR_SNO}, TO_DATE(#{LGN_DT}, 'YYYY-MM-DD HH24:MI:SS'),
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = #{MBR_SNO} AND DTA_DEL_YN = 'N'),
     *     #{TOT_ASST_AMT}, #{LON_AMT}, #{CASH_PTFLO_AMT}, #{DPSIT_PTFLO_AMT}, #{ISV_PTFLO_AMT},
     *     #{ANNTY_PTFLO_AMT}, #{STOCK_PTFLO_AMT}, #{FUND_PTFLO_AMT}, #{BOND_PTFLO_AMT},
     *     #{CASH_PTFLO_EVL_AMT}, #{DPSIT_PTFLO_EVL_AMT}, #{ISV_PTFLO_EVL_AMT},
     *     #{ANNTY_PTFLO_EVL_AMT}, #{STOCK_PTFLO_EVL_AMT}, #{FUND_PTFLO_EVL_AMT}, #{BOND_PTFLO_EVL_AMT},
     *     #{PTFLO_CPST_CTNS}, #{LOG_CTNS}, #{ACTI_TIME}, 'N',
     *     'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     'KFWEB', SYSDATE
     * )
     */
    public HashMap<String, Object> createCompetitionRound(Long cmpttSno, Integer cmpttRndNo,
                                                           Long mbrSno, String lgnDt,
                                                           Map<String, Object> portfolioData) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        param.put("CMPTT_RND_NO", cmpttRndNo);
        param.put("MBR_SNO", mbrSno);
        param.put("LGN_DT", lgnDt);
        param.putAll(portfolioData);
        return executeRequest(param, IF_061_INSERT, OP_INSERT);
    }

    /*
     * [미사용] 경쟁 라운드 조회 (단건)
     *
    public HashMap<String, Object> getCompetitionRound(Long cmpttSno, Integer cmpttRndNo) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        param.put("CMPTT_RND_NO", cmpttRndNo);
        return executeRequest(param, IF_061_SELECT, OP_SELECT);
    }
    */

    /**
     * 경쟁 전체 라운드 조회
     * 
     * MyBatis SQL:
     * SELECT P.CMPTT_SNO, P.CMPTT_RND_NO, P.MBR_SNO, P.TOT_ASST_AMT, P.LON_AMT,
     *        P.CASH_PTFLO_AMT, P.DPSIT_PTFLO_AMT, P.ISV_PTFLO_AMT,
     *        P.ANNTY_PTFLO_AMT, P.STOCK_PTFLO_AMT, P.FUND_PTFLO_AMT, P.BOND_PTFLO_AMT,
     *        P.CASH_PTFLO_EVL_AMT, P.DPSIT_PTFLO_EVL_AMT, P.ISV_PTFLO_EVL_AMT,
     *        P.ANNTY_PTFLO_EVL_AMT, P.STOCK_PTFLO_EVL_AMT, P.FUND_PTFLO_EVL_AMT, P.BOND_PTFLO_EVL_AMT,
     *        P.PTFLO_CPST_CTNS, P.LOG_CTNS, P.ACTI_TIME
     * FROM KMHAD061M P
     * WHERE P.CMPTT_SNO = #{CMPTT_SNO}
     *   AND P.DTA_DEL_YN = 'N'
     * ORDER BY P.CMPTT_RND_NO ASC
     */
    public HashMap<String, Object> getCompetitionAllRounds(Long cmpttSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        return executeRequest(param, IF_061_SELECT_ALL, OP_LIST);
    }

    /*
     * [미사용] 경쟁 라운드 포트폴리오 업데이트
     *
    public HashMap<String, Object> updateCompetitionRoundPortfolio(Long cmpttSno, Integer cmpttRndNo,
                                                                    Map<String, Object> portfolioData) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        param.put("CMPTT_RND_NO", cmpttRndNo);
        param.putAll(portfolioData);
        return executeRequest(param, IF_061_UPDATE_PORTFOLIO, OP_UPDATE);
    }
    */

    /*
     * [미사용] 경쟁 라운드 로그 업데이트
     *
    public HashMap<String, Object> updateCompetitionRoundLog(Long cmpttSno, Integer cmpttRndNo,
                                                              String logCtns, String actiTime) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        param.put("CMPTT_RND_NO", cmpttRndNo);
        param.put("LOG_CTNS", logCtns);
        param.put("ACTI_TIME", actiTime);
        return executeRequest(param, IF_061_UPDATE_LOG, OP_UPDATE);
    }
    */

    // ========================================
    // 8. 경쟁 결과 (KMHAD062M)
    // ========================================

    /**
     * 경쟁 결과 저장
     * 
     * MyBatis SQL:
     * INSERT INTO KMHAD062M (
     *     NINAM_SNO, CMPTT_SNO, CMPTT_MODE_SCR, FNNR_MNG_SCR, RISK_MNG_SCR, ABSL_YILD_SCR,
     *     DTA_DEL_YN, FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = #{MBR_SNO} AND DTA_DEL_YN = 'N'),
     *     #{CMPTT_SNO}, #{CMPTT_MODE_SCR}, #{FNNR_MNG_SCR}, #{RISK_MNG_SCR}, #{ABSL_YILD_SCR},
     *     'N', 'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     'KFWEB', SYSDATE
     * )
     */
    public HashMap<String, Object> saveCompetitionResult(Long mbrSno, Long cmpttSno, Long cmpttModeScr,
                                                          Long fnnrMngScr, Long riskMngScr, 
                                                          Long abslYildScr) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("CMPTT_SNO", cmpttSno);
        param.put("CMPTT_MODE_SCR", cmpttModeScr);
        param.put("FNNR_MNG_SCR", fnnrMngScr);
        param.put("RISK_MNG_SCR", riskMngScr);
        param.put("ABSL_YILD_SCR", abslYildScr);
        return executeRequest(param, IF_062_INSERT, OP_INSERT);
    }

    /*
     * [미사용] 경쟁 결과 조회 (단건)
     *
    public HashMap<String, Object> getCompetitionResult(Long cmpttSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        return executeRequest(param, IF_062_SELECT, OP_SELECT);
    }
    */

    /*
     * [미사용] 사용자 경쟁 결과 목록
     *
    public HashMap<String, Object> getCompetitionResultsByUser(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, IF_062_LIST, OP_LIST);
    }
    */

    /**
     * 전체 랭킹 조회
     * 
     * MyBatis SQL:
     * SELECT R.NINAM_SNO, G.MBR_SNO, G.NINAM_NM, R.CMPTT_MODE_SCR, R.FNNR_MNG_SCR, 
     *        R.RISK_MNG_SCR, R.ABSL_YILD_SCR,
     *        RANK() OVER (ORDER BY R.CMPTT_MODE_SCR DESC) AS RANKING
     * FROM KMHAD062M R
     * INNER JOIN KMHAD055M G ON R.NINAM_SNO = G.NINAM_SNO
     * WHERE R.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY R.CMPTT_MODE_SCR DESC
     * FETCH FIRST #{LIMIT} ROWS ONLY
     */
    public HashMap<String, Object> getRanking(Integer limit) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("LIMIT", limit);
        return executeRequest(param, IF_062_RANKING, OP_LIST);
    }

    /**
     * 월간 랭킹 조회
     * 
     * MyBatis SQL:
     * SELECT R.NINAM_SNO AS "ninamSno",
     *        G.MBR_SNO AS "mbrSno",
     *        G.NINAM_NM AS "ninamNm",
     *        R.CMPTT_MODE_SCR AS "cmpttModeScr",
     *        R.FNNR_MNG_SCR AS "fnnrMngScr",
     *        R.RISK_MNG_SCR AS "riskMngScr",
     *        R.ABSL_YILD_SCR AS "abslYildScr",
     *        R.FIRST_CRT_DT AS "firstCrtDt",
     *        RANK() OVER (ORDER BY R.CMPTT_MODE_SCR DESC) AS "ranking"
     * FROM KMHAD062M R
     * INNER JOIN KMHAD055M G ON R.NINAM_SNO = G.NINAM_SNO
     * INNER JOIN KMHAD060M C ON R.CMPTT_SNO = C.CMPTT_SNO
     * WHERE R.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     *   AND C.DTA_DEL_YN = 'N'
     *   AND R.FIRST_CRT_DT >= TO_DATE(#{YEAR_MONTH} || '01', 'YYYYMMDD')
     *   AND R.FIRST_CRT_DT < ADD_MONTHS(TO_DATE(#{YEAR_MONTH} || '01', 'YYYYMMDD'), 1)
     * ORDER BY R.CMPTT_MODE_SCR DESC
     * FETCH FIRST #{LIMIT} ROWS ONLY
     * 
     * @param yearMonth 년월 (형식: YYYYMM, 예: 202512)
     * @param limit 조회 건수
     */
    public HashMap<String, Object> getMonthlyRanking(String yearMonth, Integer limit) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("YEAR_MONTH", yearMonth);
        param.put("LIMIT", limit);
        return executeRequest(param, IF_062_MONTHLY_RANKING, OP_LIST);
    }

    /**
     * 사용자의 월간 순위 조회
     * 
     * MyBatis SQL:
     * SELECT "ranking", "ninamSno", "mbrSno", "ninamNm", "cmpttModeScr"
     * FROM (
     *     SELECT R.NINAM_SNO AS "ninamSno",
     *            G.MBR_SNO AS "mbrSno",
     *            G.NINAM_NM AS "ninamNm",
     *            R.CMPTT_MODE_SCR AS "cmpttModeScr",
     *            RANK() OVER (ORDER BY R.CMPTT_MODE_SCR DESC) AS "ranking"
     *     FROM KMHAD062M R
     *     INNER JOIN KMHAD055M G ON R.NINAM_SNO = G.NINAM_SNO
     *     WHERE R.DTA_DEL_YN = 'N'
     *       AND G.DTA_DEL_YN = 'N'
     *       AND R.FIRST_CRT_DT >= TO_DATE(#{YEAR_MONTH} || '01', 'YYYYMMDD')
     *       AND R.FIRST_CRT_DT < ADD_MONTHS(TO_DATE(#{YEAR_MONTH} || '01', 'YYYYMMDD'), 1)
     * )
     * WHERE "mbrSno" = #{MBR_SNO}
     * 
     * @param mbrSno 회원일련번호
     * @param yearMonth 년월 (형식: YYYYMM)
     */
    public HashMap<String, Object> getMyMonthlyRanking(Long mbrSno, String yearMonth) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("YEAR_MONTH", yearMonth);
        return executeRequest(param, IF_062_MONTHLY_RANKING, OP_SELECT);
    }

    // ========================================
    // 9. 학습 정보 (KMHAD063M) - 퀴즈
    // ========================================

    /**
     * 학습 정보 저장 (퀴즈 응답)
     * 
     * MyBatis SQL:
     * INSERT INTO KMHAD063M (
     *     NINAM_SNO, TTRL_SNO, CMPTT_SNO, FNPRD_NO, MFILE_FNSH_YN,
     *     QZ_NO, OBJCT_RSPNS_NO, CRAN_YN, ACTI_TIME, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = #{MBR_SNO} AND DTA_DEL_YN = 'N'),
     *     #{TTRL_SNO}, #{CMPTT_SNO}, #{FNPRD_NO}, #{MFILE_FNSH_YN},
     *     #{QZ_NO}, #{OBJCT_RSPNS_NO}, #{CRAN_YN}, #{ACTI_TIME}, 'N',
     *     'CAS'||TO_CHAR(SYSTIMESTAMP,'YYYYMMDDHH24MISSFF3')||'C_WEB_SQL_000001',
     *     'KFWEB', SYSDATE
     * )
     */
    public HashMap<String, Object> saveLearningInfo(Long mbrSno, Long ttrlSno, Long cmpttSno,
                                                     Integer fnprdNo, Integer mfileFinshYn,
                                                     String qzNo, String objctRspnsNo,
                                                     Integer cranYn, String actiTime) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("TTRL_SNO", ttrlSno);
        param.put("CMPTT_SNO", cmpttSno);
        param.put("FNPRD_NO", fnprdNo);
        param.put("MFILE_FNSH_YN", mfileFinshYn);
        param.put("QZ_NO", qzNo);
        param.put("OBJCT_RSPNS_NO", objctRspnsNo);
        param.put("CRAN_YN", cranYn);
        param.put("ACTI_TIME", actiTime);
        return executeRequest(param, IF_063_INSERT, OP_INSERT);
    }

    /**
     * 학습 정보 조회 (튜토리얼)
     * 
     * MyBatis SQL:
     * SELECT L.NINAM_SNO, G.MBR_SNO, L.TTRL_SNO, L.CMPTT_SNO, L.FNPRD_NO, L.MFILE_FNSH_YN,
     *        L.QZ_NO, L.OBJCT_RSPNS_NO, L.CRAN_YN, L.ACTI_TIME
     * FROM KMHAD063M L
     * INNER JOIN KMHAD055M G ON L.NINAM_SNO = G.NINAM_SNO
     * WHERE L.TTRL_SNO = #{TTRL_SNO}
     *   AND L.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY L.FIRST_CRT_DT ASC
     */
    public HashMap<String, Object> getLearningInfoByTutorial(Long ttrlSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        return executeRequest(param, IF_063_SELECT_TUTORIAL, OP_LIST);
    }

    /**
     * 학습 정보 조회 (경쟁)
     * 
     * MyBatis SQL:
     * SELECT L.NINAM_SNO, G.MBR_SNO, L.TTRL_SNO, L.CMPTT_SNO, L.FNPRD_NO, L.MFILE_FNSH_YN,
     *        L.QZ_NO, L.OBJCT_RSPNS_NO, L.CRAN_YN, L.ACTI_TIME
     * FROM KMHAD063M L
     * INNER JOIN KMHAD055M G ON L.NINAM_SNO = G.NINAM_SNO
     * WHERE L.CMPTT_SNO = #{CMPTT_SNO}
     *   AND L.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY L.FIRST_CRT_DT ASC
     */
    public HashMap<String, Object> getLearningInfoByCompetition(Long cmpttSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        return executeRequest(param, IF_063_SELECT_COMPETITION, OP_LIST);
    }

    /*
     * [미사용] 사용자 학습 통계
     *
    public HashMap<String, Object> getLearningStats(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, IF_063_STATS, OP_LIST);
    }
    */
}
