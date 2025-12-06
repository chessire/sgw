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
 * - 외부 인프라(DB)와 통신하여 게임 데이터를 저장/조회
 * - 모든 조회는 MBR_SNO(회원일련번호)를 기준으로 하며, KMHAD055M과 JOIN하여 NINAM_SNO를 매핑
 * 
 * 환경 설정:
 * - app.use-external-db=true: Production 환경, 실제 DB 통신
 * - app.use-external-db=false: Development 환경, Mock 데이터 반환
 * 
 * 제외된 테이블 (별도 인증 채널에서 관리):
 * - KMHAD052M (고객인증)
 * - KMHAD053M (고객인증여부이력)
 * - KMHAD056M (고객세션) - JWT 토큰 미사용
 * - KMHAD064M (버전관리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final HttpHandler httpHandler;
    
    @Value("${app.use-external-db:false}")
    private boolean useExternalDb;
    
    /**
     * 외부 DB 사용 여부 확인
     */
    public boolean isUseExternalDb() {
        return useExternalDb;
    }
    
    /**
     * 외부 DB 호출 wrapper
     * - useExternalDb=true: 실제 HTTP 호출
     * - useExternalDb=false: Mock 응답 반환
     */
    private HashMap<String, Object> executeRequest(HashMap<String, Object> param, String url) throws KinfaRunException {
        if (!useExternalDb) {
            log.debug("[MOCK MODE] Skipping external DB call: {} with params: {}", url, param);
            return createMockResponse(url, param);
        }
        return httpHandler.postForObject(param, url);
    }
    
    /**
     * Mock 응답 생성
     * - Development 환경에서 사용
     */
    private HashMap<String, Object> createMockResponse(String url, HashMap<String, Object> param) {
        HashMap<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("success", true);
        mockResponse.put("mock", true);
        mockResponse.put("url", url);
        mockResponse.put("requestParams", param);
        
        // URL별 Mock 데이터 커스터마이징
        if (url.contains("/select") || url.contains("/get") || url.contains("/list")) {
            mockResponse.put("data", new HashMap<String, Object>());
        } else if (url.contains("/insert") || url.contains("/create")) {
            mockResponse.put("insertedId", System.currentTimeMillis());
        } else if (url.contains("/update")) {
            mockResponse.put("updatedCount", 1);
        } else if (url.contains("/delete")) {
            mockResponse.put("deletedCount", 1);
        }
        
        log.info("[MOCK] Generated mock response for {}: {}", url, mockResponse);
        return mockResponse;
    }

    // ========================================
    // 1. 고객 로그인 (KMHAD054M)
    // ========================================

    /**
     * 로그인 기록 생성
     * 
     * Oracle SQL:
     * INSERT INTO KMHAD054M (
     *     MBR_SNO, LGN_DT, IDVRF_MTCD, LGN_SECD, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     :mbrSno, SYSDATE, :idvrfMtcd, :lgnSecd, 'N',
     *     SYS_GUID(), :userId, SYSDATE
     * )
     */
    public HashMap<String, Object> createLoginLog(Long mbrSno, String idvrfMtcd, 
                                                   String lgnSecd) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("IDVRF_MTCD", idvrfMtcd);
        param.put("LGN_SECD", lgnSecd);
        return executeRequest(param, "/KMHAD054M/insert");
    }

    /**
     * 로그아웃 처리
     * 
     * Oracle SQL:
     * UPDATE KMHAD054M
     * SET LOUT_DT = SYSDATE,
     *     LAST_CHG_GUID = SYS_GUID(),
     *     LAST_CHG_USR_ID = :userId,
     *     LAST_CHG_DT = SYSDATE
     * WHERE MBR_SNO = :mbrSno
     *   AND LGN_DT = TO_DATE(:lgnDt, 'YYYY-MM-DD HH24:MI:SS')
     *   AND DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> updateLogout(Long mbrSno, String lgnDt) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("LGN_DT", lgnDt);
        return executeRequest(param, "/KMHAD054M/updateLogout");
    }

    /**
     * 최근 로그인 정보 조회
     * 
     * Oracle SQL:
     * SELECT MBR_SNO, LGN_DT, IDVRF_MTCD, LGN_SECD, LOUT_DT
     * FROM KMHAD054M
     * WHERE MBR_SNO = :mbrSno
     *   AND DTA_DEL_YN = 'N'
     * ORDER BY LGN_DT DESC
     * FETCH FIRST 1 ROWS ONLY
     */
    public HashMap<String, Object> getLastLogin(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, "/KMHAD054M/selectLast");
    }

    // ========================================
    // 2. 게임 기본 정보 (KMHAD055M) - 닉네임, 업적
    // ========================================

    /**
     * 게임 기본 정보 조회 (회원번호로 조회)
     * 
     * Oracle SQL:
     * SELECT NINAM_SNO, MBR_SNO, NINAM_NM, ACHM_NO
     * FROM KMHAD055M
     * WHERE MBR_SNO = :mbrSno
     *   AND DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> getGameBasicInfo(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, "/KMHAD055M/select");
    }

    /**
     * 게임 기본 정보 생성 (닉네임 생성)
     * 
     * Oracle SQL:
     * INSERT INTO KMHAD055M (
     *     NINAM_SNO, MBR_SNO, NINAM_NM, ACHM_NO, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     KMHAD055M_SEQ.NEXTVAL, :mbrSno, :ninamNm, 0, 'N',
     *     SYS_GUID(), :userId, SYSDATE
     * )
     */
    public HashMap<String, Object> createGameBasicInfo(Long mbrSno, String ninamNm) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("NINAM_NM", ninamNm);
        param.put("ACHM_NO", 0);
        return executeRequest(param, "/KMHAD055M/insert");
    }

    /**
     * 업적 업데이트
     * 
     * Oracle SQL:
     * UPDATE KMHAD055M
     * SET ACHM_NO = :achmNo,
     *     LAST_CHG_GUID = SYS_GUID(),
     *     LAST_CHG_USR_ID = :userId,
     *     LAST_CHG_DT = SYSDATE
     * WHERE MBR_SNO = :mbrSno
     *   AND DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> updateAchievement(Long mbrSno, Long achmNo) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("ACHM_NO", achmNo);
        return executeRequest(param, "/KMHAD055M/updateAchievement");
    }

    // ========================================
    // 3. 튜토리얼 정보 (KMHAD057M)
    // ========================================

    /**
     * 튜토리얼 시작 (정보 생성)
     * 
     * Oracle SQL:
     * INSERT INTO KMHAD057M (
     *     TTRL_SNO, NINAM_SNO, FIPAT_NM, RNDM_NO, MM_AVRG_INCME_AMT, 
     *     FIX_EXPND_AMT, NPC_NO, LOG_CTNS, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     KMHAD057M_SEQ.NEXTVAL, 
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = :mbrSno AND DTA_DEL_YN = 'N'),
     *     :fipatNm, :rndmNo, :mmAvrgIncmeAmt,
     *     :fixExpndAmt, :npcNo, :logCtns, 'N',
     *     SYS_GUID(), :userId, SYSDATE
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
        return executeRequest(param, "/KMHAD057M/insert");
    }

    /**
     * 튜토리얼 정보 조회
     * 
     * Oracle SQL:
     * SELECT T.TTRL_SNO, T.NINAM_SNO, G.MBR_SNO, T.FIPAT_NM, T.RNDM_NO, 
     *        T.MM_AVRG_INCME_AMT, T.FIX_EXPND_AMT, T.NPC_NO, T.LOG_CTNS
     * FROM KMHAD057M T
     * INNER JOIN KMHAD055M G ON T.NINAM_SNO = G.NINAM_SNO
     * WHERE T.TTRL_SNO = :ttrlSno
     *   AND T.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> getTutorial(Long ttrlSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        return executeRequest(param, "/KMHAD057M/select");
    }

    /**
     * 사용자의 튜토리얼 목록 조회 (회원번호로 조회)
     * 
     * Oracle SQL:
     * SELECT T.TTRL_SNO, T.NINAM_SNO, G.MBR_SNO, T.FIPAT_NM, T.RNDM_NO, 
     *        T.MM_AVRG_INCME_AMT, T.FIX_EXPND_AMT, T.NPC_NO, T.FIRST_CRT_DT
     * FROM KMHAD057M T
     * INNER JOIN KMHAD055M G ON T.NINAM_SNO = G.NINAM_SNO
     * WHERE G.MBR_SNO = :mbrSno
     *   AND T.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY T.FIRST_CRT_DT DESC
     */
    public HashMap<String, Object> getTutorialListByUser(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, "/KMHAD057M/selectList");
    }

    // ========================================
    // 4. 튜토리얼 진행 정보 (KMHAD058M) - 라운드별
    // ========================================

    /**
     * 튜토리얼 라운드 시작 (진행 정보 생성)
     * 
     * Oracle SQL:
     * INSERT INTO KMHAD058M (
     *     TTRL_SNO, TTRL_RND_NO, MBR_SNO, LGN_DT, NINAM_SNO,
     *     TOT_ASST_AMT, LON_AMT, CASH_PTFLO_AMT, DPSIT_PTFLO_AMT, ISV_PTFLO_AMT,
     *     ANNTY_PTFLO_AMT, STOCK_PTFLO_AMT, FUND_PTFLO_AMT, BOND_PTFLO_AMT,
     *     CASH_PTFLO_EVL_AMT, DPSIT_PTFLO_EVL_AMT, ISV_PTFLO_EVL_AMT,
     *     ANNTY_PTFLO_EVL_AMT, STOCK_PTFLO_EVL_AMT, FUND_PTFLO_EVL_AMT, BOND_PTFLO_EVL_AMT,
     *     PTFLO_CPST_CTNS, LOG_CTNS, ACTI_TIME, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     :ttrlSno, :ttrlRndNo, :mbrSno, TO_DATE(:lgnDt, 'YYYY-MM-DD HH24:MI:SS'),
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = :mbrSno AND DTA_DEL_YN = 'N'),
     *     :totAsstAmt, :lonAmt, :cashPtfloAmt, :dpsitPtfloAmt, :isvPtfloAmt,
     *     :anntyPtfloAmt, :stockPtfloAmt, :fundPtfloAmt, :bondPtfloAmt,
     *     :cashPtfloEvlAmt, :dpsitPtfloEvlAmt, :isvPtfloEvlAmt,
     *     :anntyPtfloEvlAmt, :stockPtfloEvlAmt, :fundPtfloEvlAmt, :bondPtfloEvlAmt,
     *     :ptfloCpstCtns, :logCtns, :actiTime, 'N',
     *     SYS_GUID(), :userId, SYSDATE
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
        return executeRequest(param, "/KMHAD058M/insert");
    }

    /**
     * 튜토리얼 라운드 조회
     * 
     * Oracle SQL:
     * SELECT P.TTRL_SNO, P.TTRL_RND_NO, P.MBR_SNO, P.LGN_DT, P.NINAM_SNO,
     *        P.TOT_ASST_AMT, P.LON_AMT, P.CASH_PTFLO_AMT, P.DPSIT_PTFLO_AMT, P.ISV_PTFLO_AMT,
     *        P.ANNTY_PTFLO_AMT, P.STOCK_PTFLO_AMT, P.FUND_PTFLO_AMT, P.BOND_PTFLO_AMT,
     *        P.CASH_PTFLO_EVL_AMT, P.DPSIT_PTFLO_EVL_AMT, P.ISV_PTFLO_EVL_AMT,
     *        P.ANNTY_PTFLO_EVL_AMT, P.STOCK_PTFLO_EVL_AMT, P.FUND_PTFLO_EVL_AMT, P.BOND_PTFLO_EVL_AMT,
     *        P.PTFLO_CPST_CTNS, P.LOG_CTNS, P.ACTI_TIME
     * FROM KMHAD058M P
     * WHERE P.TTRL_SNO = :ttrlSno
     *   AND P.TTRL_RND_NO = :ttrlRndNo
     *   AND P.DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> getTutorialRound(Long ttrlSno, Integer ttrlRndNo) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        param.put("TTRL_RND_NO", ttrlRndNo);
        return executeRequest(param, "/KMHAD058M/select");
    }

    /**
     * 튜토리얼 전체 라운드 조회
     * 
     * Oracle SQL:
     * SELECT P.TTRL_SNO, P.TTRL_RND_NO, P.MBR_SNO, P.TOT_ASST_AMT, P.LON_AMT,
     *        P.CASH_PTFLO_AMT, P.DPSIT_PTFLO_AMT, P.ISV_PTFLO_AMT,
     *        P.ANNTY_PTFLO_AMT, P.STOCK_PTFLO_AMT, P.FUND_PTFLO_AMT, P.BOND_PTFLO_AMT,
     *        P.CASH_PTFLO_EVL_AMT, P.DPSIT_PTFLO_EVL_AMT, P.ISV_PTFLO_EVL_AMT,
     *        P.ANNTY_PTFLO_EVL_AMT, P.STOCK_PTFLO_EVL_AMT, P.FUND_PTFLO_EVL_AMT, P.BOND_PTFLO_EVL_AMT,
     *        P.PTFLO_CPST_CTNS, P.LOG_CTNS, P.ACTI_TIME
     * FROM KMHAD058M P
     * WHERE P.TTRL_SNO = :ttrlSno
     *   AND P.DTA_DEL_YN = 'N'
     * ORDER BY P.TTRL_RND_NO ASC
     */
    public HashMap<String, Object> getTutorialAllRounds(Long ttrlSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        return executeRequest(param, "/KMHAD058M/selectAll");
    }

    /**
     * 튜토리얼 라운드 포트폴리오 업데이트
     * 
     * Oracle SQL:
     * UPDATE KMHAD058M
     * SET TOT_ASST_AMT = :totAsstAmt,
     *     LON_AMT = :lonAmt,
     *     CASH_PTFLO_AMT = :cashPtfloAmt,
     *     DPSIT_PTFLO_AMT = :dpsitPtfloAmt,
     *     ISV_PTFLO_AMT = :isvPtfloAmt,
     *     ANNTY_PTFLO_AMT = :anntyPtfloAmt,
     *     STOCK_PTFLO_AMT = :stockPtfloAmt,
     *     FUND_PTFLO_AMT = :fundPtfloAmt,
     *     BOND_PTFLO_AMT = :bondPtfloAmt,
     *     CASH_PTFLO_EVL_AMT = :cashPtfloEvlAmt,
     *     DPSIT_PTFLO_EVL_AMT = :dpsitPtfloEvlAmt,
     *     ISV_PTFLO_EVL_AMT = :isvPtfloEvlAmt,
     *     ANNTY_PTFLO_EVL_AMT = :anntyPtfloEvlAmt,
     *     STOCK_PTFLO_EVL_AMT = :stockPtfloEvlAmt,
     *     FUND_PTFLO_EVL_AMT = :fundPtfloEvlAmt,
     *     BOND_PTFLO_EVL_AMT = :bondPtfloEvlAmt,
     *     PTFLO_CPST_CTNS = :ptfloCpstCtns,
     *     LAST_CHG_GUID = SYS_GUID(),
     *     LAST_CHG_USR_ID = :userId,
     *     LAST_CHG_DT = SYSDATE
     * WHERE TTRL_SNO = :ttrlSno
     *   AND TTRL_RND_NO = :ttrlRndNo
     *   AND DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> updateTutorialRoundPortfolio(Long ttrlSno, Integer ttrlRndNo,
                                                                 Map<String, Object> portfolioData) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        param.put("TTRL_RND_NO", ttrlRndNo);
        param.putAll(portfolioData);
        return executeRequest(param, "/KMHAD058M/updatePortfolio");
    }

    /**
     * 튜토리얼 라운드 로그 업데이트
     * 
     * Oracle SQL:
     * UPDATE KMHAD058M
     * SET LOG_CTNS = :logCtns,
     *     ACTI_TIME = :actiTime,
     *     LAST_CHG_GUID = SYS_GUID(),
     *     LAST_CHG_USR_ID = :userId,
     *     LAST_CHG_DT = SYSDATE
     * WHERE TTRL_SNO = :ttrlSno
     *   AND TTRL_RND_NO = :ttrlRndNo
     *   AND DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> updateTutorialRoundLog(Long ttrlSno, Integer ttrlRndNo,
                                                           String logCtns, String actiTime) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        param.put("TTRL_RND_NO", ttrlRndNo);
        param.put("LOG_CTNS", logCtns);
        param.put("ACTI_TIME", actiTime);
        return executeRequest(param, "/KMHAD058M/updateLog");
    }

    // ========================================
    // 5. 튜토리얼 결과 (KMHAD059M)
    // ========================================

    /**
     * 튜토리얼 결과 저장
     * 
     * Oracle SQL:
     * INSERT INTO KMHAD059M (
     *     NINAM_SNO, TTRL_SNO, TTRL_MODE_SCR, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = :mbrSno AND DTA_DEL_YN = 'N'),
     *     :ttrlSno, :ttrlModeScr, 'N',
     *     SYS_GUID(), :userId, SYSDATE
     * )
     */
    public HashMap<String, Object> saveTutorialResult(Long mbrSno, Long ttrlSno, 
                                                       Long ttrlModeScr) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("TTRL_SNO", ttrlSno);
        param.put("TTRL_MODE_SCR", ttrlModeScr);
        return executeRequest(param, "/KMHAD059M/insert");
    }

    /**
     * 튜토리얼 결과 조회
     * 
     * Oracle SQL:
     * SELECT R.NINAM_SNO, G.MBR_SNO, R.TTRL_SNO, R.TTRL_MODE_SCR
     * FROM KMHAD059M R
     * INNER JOIN KMHAD055M G ON R.NINAM_SNO = G.NINAM_SNO
     * WHERE R.TTRL_SNO = :ttrlSno
     *   AND R.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> getTutorialResult(Long ttrlSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        return executeRequest(param, "/KMHAD059M/select");
    }

    /**
     * 사용자 튜토리얼 결과 목록 (회원번호로 조회)
     * 
     * Oracle SQL:
     * SELECT R.NINAM_SNO, G.MBR_SNO, R.TTRL_SNO, R.TTRL_MODE_SCR, R.FIRST_CRT_DT
     * FROM KMHAD059M R
     * INNER JOIN KMHAD055M G ON R.NINAM_SNO = G.NINAM_SNO
     * WHERE G.MBR_SNO = :mbrSno
     *   AND R.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY R.FIRST_CRT_DT DESC
     */
    public HashMap<String, Object> getTutorialResultsByUser(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, "/KMHAD059M/selectList");
    }

    // ========================================
    // 6. 경쟁 정보 (KMHAD060M)
    // ========================================

    /**
     * 경쟁 모드 시작 (정보 생성)
     * 
     * Oracle SQL:
     * INSERT INTO KMHAD060M (
     *     CMPTT_SNO, NINAM_SNO, FIPAT_NM, RNDM_NO, NPC_NO, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     KMHAD060M_SEQ.NEXTVAL, 
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = :mbrSno AND DTA_DEL_YN = 'N'),
     *     :fipatNm, :rndmNo, :npcNo, 'N',
     *     SYS_GUID(), :userId, SYSDATE
     * )
     */
    public HashMap<String, Object> createCompetition(Long mbrSno, String fipatNm,
                                                      Integer rndmNo, Integer npcNo) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        param.put("FIPAT_NM", fipatNm);
        param.put("RNDM_NO", rndmNo);
        param.put("NPC_NO", npcNo);
        return executeRequest(param, "/KMHAD060M/insert");
    }

    /**
     * 경쟁 정보 조회
     * 
     * Oracle SQL:
     * SELECT C.CMPTT_SNO, C.NINAM_SNO, G.MBR_SNO, C.FIPAT_NM, C.RNDM_NO, C.NPC_NO
     * FROM KMHAD060M C
     * INNER JOIN KMHAD055M G ON C.NINAM_SNO = G.NINAM_SNO
     * WHERE C.CMPTT_SNO = :cmpttSno
     *   AND C.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> getCompetition(Long cmpttSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        return executeRequest(param, "/KMHAD060M/select");
    }

    /**
     * 사용자의 경쟁 목록 조회 (회원번호로 조회)
     * 
     * Oracle SQL:
     * SELECT C.CMPTT_SNO, C.NINAM_SNO, G.MBR_SNO, C.FIPAT_NM, C.RNDM_NO, C.NPC_NO, C.FIRST_CRT_DT
     * FROM KMHAD060M C
     * INNER JOIN KMHAD055M G ON C.NINAM_SNO = G.NINAM_SNO
     * WHERE G.MBR_SNO = :mbrSno
     *   AND C.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY C.FIRST_CRT_DT DESC
     */
    public HashMap<String, Object> getCompetitionListByUser(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, "/KMHAD060M/selectList");
    }

    // ========================================
    // 7. 경쟁 진행 정보 (KMHAD061M) - 라운드별
    // ========================================

    /**
     * 경쟁 라운드 시작 (진행 정보 생성)
     * 
     * Oracle SQL:
     * INSERT INTO KMHAD061M (
     *     CMPTT_SNO, CMPTT_RND_NO, MBR_SNO, LGN_DT, NINAM_SNO,
     *     TOT_ASST_AMT, LON_AMT, CASH_PTFLO_AMT, DPSIT_PTFLO_AMT, ISV_PTFLO_AMT,
     *     ANNTY_PTFLO_AMT, STOCK_PTFLO_AMT, FUND_PTFLO_AMT, BOND_PTFLO_AMT,
     *     CASH_PTFLO_EVL_AMT, DPSIT_PTFLO_EVL_AMT, ISV_PTFLO_EVL_AMT,
     *     ANNTY_PTFLO_EVL_AMT, STOCK_PTFLO_EVL_AMT, FUND_PTFLO_EVL_AMT, BOND_PTFLO_EVL_AMT,
     *     PTFLO_CPST_CTNS, LOG_CTNS, ACTI_TIME, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     :cmpttSno, :cmpttRndNo, :mbrSno, TO_DATE(:lgnDt, 'YYYY-MM-DD HH24:MI:SS'),
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = :mbrSno AND DTA_DEL_YN = 'N'),
     *     :totAsstAmt, :lonAmt, :cashPtfloAmt, :dpsitPtfloAmt, :isvPtfloAmt,
     *     :anntyPtfloAmt, :stockPtfloAmt, :fundPtfloAmt, :bondPtfloAmt,
     *     :cashPtfloEvlAmt, :dpsitPtfloEvlAmt, :isvPtfloEvlAmt,
     *     :anntyPtfloEvlAmt, :stockPtfloEvlAmt, :fundPtfloEvlAmt, :bondPtfloEvlAmt,
     *     :ptfloCpstCtns, :logCtns, :actiTime, 'N',
     *     SYS_GUID(), :userId, SYSDATE
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
        return executeRequest(param, "/KMHAD061M/insert");
    }

    /**
     * 경쟁 라운드 조회
     * 
     * Oracle SQL:
     * SELECT P.CMPTT_SNO, P.CMPTT_RND_NO, P.MBR_SNO, P.LGN_DT, P.NINAM_SNO,
     *        P.TOT_ASST_AMT, P.LON_AMT, P.CASH_PTFLO_AMT, P.DPSIT_PTFLO_AMT, P.ISV_PTFLO_AMT,
     *        P.ANNTY_PTFLO_AMT, P.STOCK_PTFLO_AMT, P.FUND_PTFLO_AMT, P.BOND_PTFLO_AMT,
     *        P.CASH_PTFLO_EVL_AMT, P.DPSIT_PTFLO_EVL_AMT, P.ISV_PTFLO_EVL_AMT,
     *        P.ANNTY_PTFLO_EVL_AMT, P.STOCK_PTFLO_EVL_AMT, P.FUND_PTFLO_EVL_AMT, P.BOND_PTFLO_EVL_AMT,
     *        P.PTFLO_CPST_CTNS, P.LOG_CTNS, P.ACTI_TIME
     * FROM KMHAD061M P
     * WHERE P.CMPTT_SNO = :cmpttSno
     *   AND P.CMPTT_RND_NO = :cmpttRndNo
     *   AND P.DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> getCompetitionRound(Long cmpttSno, Integer cmpttRndNo) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        param.put("CMPTT_RND_NO", cmpttRndNo);
        return executeRequest(param, "/KMHAD061M/select");
    }

    /**
     * 경쟁 전체 라운드 조회
     * 
     * Oracle SQL:
     * SELECT P.CMPTT_SNO, P.CMPTT_RND_NO, P.MBR_SNO, P.TOT_ASST_AMT, P.LON_AMT,
     *        P.CASH_PTFLO_AMT, P.DPSIT_PTFLO_AMT, P.ISV_PTFLO_AMT,
     *        P.ANNTY_PTFLO_AMT, P.STOCK_PTFLO_AMT, P.FUND_PTFLO_AMT, P.BOND_PTFLO_AMT,
     *        P.CASH_PTFLO_EVL_AMT, P.DPSIT_PTFLO_EVL_AMT, P.ISV_PTFLO_EVL_AMT,
     *        P.ANNTY_PTFLO_EVL_AMT, P.STOCK_PTFLO_EVL_AMT, P.FUND_PTFLO_EVL_AMT, P.BOND_PTFLO_EVL_AMT,
     *        P.PTFLO_CPST_CTNS, P.LOG_CTNS, P.ACTI_TIME
     * FROM KMHAD061M P
     * WHERE P.CMPTT_SNO = :cmpttSno
     *   AND P.DTA_DEL_YN = 'N'
     * ORDER BY P.CMPTT_RND_NO ASC
     */
    public HashMap<String, Object> getCompetitionAllRounds(Long cmpttSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        return executeRequest(param, "/KMHAD061M/selectAll");
    }

    /**
     * 경쟁 라운드 포트폴리오 업데이트
     * 
     * Oracle SQL:
     * UPDATE KMHAD061M
     * SET TOT_ASST_AMT = :totAsstAmt,
     *     LON_AMT = :lonAmt,
     *     CASH_PTFLO_AMT = :cashPtfloAmt,
     *     DPSIT_PTFLO_AMT = :dpsitPtfloAmt,
     *     ISV_PTFLO_AMT = :isvPtfloAmt,
     *     ANNTY_PTFLO_AMT = :anntyPtfloAmt,
     *     STOCK_PTFLO_AMT = :stockPtfloAmt,
     *     FUND_PTFLO_AMT = :fundPtfloAmt,
     *     BOND_PTFLO_AMT = :bondPtfloAmt,
     *     CASH_PTFLO_EVL_AMT = :cashPtfloEvlAmt,
     *     DPSIT_PTFLO_EVL_AMT = :dpsitPtfloEvlAmt,
     *     ISV_PTFLO_EVL_AMT = :isvPtfloEvlAmt,
     *     ANNTY_PTFLO_EVL_AMT = :anntyPtfloEvlAmt,
     *     STOCK_PTFLO_EVL_AMT = :stockPtfloEvlAmt,
     *     FUND_PTFLO_EVL_AMT = :fundPtfloEvlAmt,
     *     BOND_PTFLO_EVL_AMT = :bondPtfloEvlAmt,
     *     PTFLO_CPST_CTNS = :ptfloCpstCtns,
     *     LAST_CHG_GUID = SYS_GUID(),
     *     LAST_CHG_USR_ID = :userId,
     *     LAST_CHG_DT = SYSDATE
     * WHERE CMPTT_SNO = :cmpttSno
     *   AND CMPTT_RND_NO = :cmpttRndNo
     *   AND DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> updateCompetitionRoundPortfolio(Long cmpttSno, Integer cmpttRndNo,
                                                                    Map<String, Object> portfolioData) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        param.put("CMPTT_RND_NO", cmpttRndNo);
        param.putAll(portfolioData);
        return executeRequest(param, "/KMHAD061M/updatePortfolio");
    }

    /**
     * 경쟁 라운드 로그 업데이트
     * 
     * Oracle SQL:
     * UPDATE KMHAD061M
     * SET LOG_CTNS = :logCtns,
     *     ACTI_TIME = :actiTime,
     *     LAST_CHG_GUID = SYS_GUID(),
     *     LAST_CHG_USR_ID = :userId,
     *     LAST_CHG_DT = SYSDATE
     * WHERE CMPTT_SNO = :cmpttSno
     *   AND CMPTT_RND_NO = :cmpttRndNo
     *   AND DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> updateCompetitionRoundLog(Long cmpttSno, Integer cmpttRndNo,
                                                              String logCtns, String actiTime) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        param.put("CMPTT_RND_NO", cmpttRndNo);
        param.put("LOG_CTNS", logCtns);
        param.put("ACTI_TIME", actiTime);
        return executeRequest(param, "/KMHAD061M/updateLog");
    }

    // ========================================
    // 8. 경쟁 결과 (KMHAD062M)
    // ========================================

    /**
     * 경쟁 결과 저장
     * 
     * Oracle SQL:
     * INSERT INTO KMHAD062M (
     *     NINAM_SNO, CMPTT_SNO, CMPTT_MODE_SCR, FNNR_MNG_SCR, RISK_MNG_SCR, ABSL_YILD_SCR,
     *     DTA_DEL_YN, FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = :mbrSno AND DTA_DEL_YN = 'N'),
     *     :cmpttSno, :cmpttModeScr, :fnnrMngScr, :riskMngScr, :abslYildScr,
     *     'N', SYS_GUID(), :userId, SYSDATE
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
        return executeRequest(param, "/KMHAD062M/insert");
    }

    /**
     * 경쟁 결과 조회
     * 
     * Oracle SQL:
     * SELECT R.NINAM_SNO, G.MBR_SNO, R.CMPTT_SNO, R.CMPTT_MODE_SCR, 
     *        R.FNNR_MNG_SCR, R.RISK_MNG_SCR, R.ABSL_YILD_SCR
     * FROM KMHAD062M R
     * INNER JOIN KMHAD055M G ON R.NINAM_SNO = G.NINAM_SNO
     * WHERE R.CMPTT_SNO = :cmpttSno
     *   AND R.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     */
    public HashMap<String, Object> getCompetitionResult(Long cmpttSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        return executeRequest(param, "/KMHAD062M/select");
    }

    /**
     * 사용자 경쟁 결과 목록 (회원번호로 조회)
     * 
     * Oracle SQL:
     * SELECT R.NINAM_SNO, G.MBR_SNO, R.CMPTT_SNO, R.CMPTT_MODE_SCR, 
     *        R.FNNR_MNG_SCR, R.RISK_MNG_SCR, R.ABSL_YILD_SCR, R.FIRST_CRT_DT
     * FROM KMHAD062M R
     * INNER JOIN KMHAD055M G ON R.NINAM_SNO = G.NINAM_SNO
     * WHERE G.MBR_SNO = :mbrSno
     *   AND R.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY R.FIRST_CRT_DT DESC
     */
    public HashMap<String, Object> getCompetitionResultsByUser(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, "/KMHAD062M/selectList");
    }

    /**
     * 전체 랭킹 조회
     * 
     * Oracle SQL:
     * SELECT R.NINAM_SNO, G.MBR_SNO, G.NINAM_NM, R.CMPTT_MODE_SCR, R.FNNR_MNG_SCR, 
     *        R.RISK_MNG_SCR, R.ABSL_YILD_SCR,
     *        RANK() OVER (ORDER BY R.CMPTT_MODE_SCR DESC) AS RANKING
     * FROM KMHAD062M R
     * INNER JOIN KMHAD055M G ON R.NINAM_SNO = G.NINAM_SNO
     * WHERE R.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY R.CMPTT_MODE_SCR DESC
     * FETCH FIRST :limit ROWS ONLY
     */
    public HashMap<String, Object> getRanking(Integer limit) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("LIMIT", limit);
        return executeRequest(param, "/KMHAD062M/selectRanking");
    }

    // ========================================
    // 9. 학습 정보 (KMHAD063M) - 퀴즈
    // ========================================

    /**
     * 학습 정보 저장 (퀴즈 응답)
     * 
     * Oracle SQL:
     * INSERT INTO KMHAD063M (
     *     NINAM_SNO, TTRL_SNO, CMPTT_SNO, FNPRD_NO, MFILE_FNSH_YN,
     *     QZ_NO, OBJCT_RSPNS_NO, CRAN_YN, ACTI_TIME, DTA_DEL_YN,
     *     FIRST_CRT_GUID, FIRST_CRT_USR_ID, FIRST_CRT_DT
     * ) VALUES (
     *     (SELECT NINAM_SNO FROM KMHAD055M WHERE MBR_SNO = :mbrSno AND DTA_DEL_YN = 'N'),
     *     :ttrlSno, :cmpttSno, :fnprdNo, :mfileFinshYn,
     *     :qzNo, :objctRspnsNo, :cranYn, :actiTime, 'N',
     *     SYS_GUID(), :userId, SYSDATE
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
        return executeRequest(param, "/KMHAD063M/insert");
    }

    /**
     * 학습 정보 조회 (튜토리얼)
     * 
     * Oracle SQL:
     * SELECT L.NINAM_SNO, G.MBR_SNO, L.TTRL_SNO, L.CMPTT_SNO, L.FNPRD_NO, L.MFILE_FNSH_YN,
     *        L.QZ_NO, L.OBJCT_RSPNS_NO, L.CRAN_YN, L.ACTI_TIME
     * FROM KMHAD063M L
     * INNER JOIN KMHAD055M G ON L.NINAM_SNO = G.NINAM_SNO
     * WHERE L.TTRL_SNO = :ttrlSno
     *   AND L.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY L.FIRST_CRT_DT ASC
     */
    public HashMap<String, Object> getLearningInfoByTutorial(Long ttrlSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("TTRL_SNO", ttrlSno);
        return executeRequest(param, "/KMHAD063M/selectByTutorial");
    }

    /**
     * 학습 정보 조회 (경쟁)
     * 
     * Oracle SQL:
     * SELECT L.NINAM_SNO, G.MBR_SNO, L.TTRL_SNO, L.CMPTT_SNO, L.FNPRD_NO, L.MFILE_FNSH_YN,
     *        L.QZ_NO, L.OBJCT_RSPNS_NO, L.CRAN_YN, L.ACTI_TIME
     * FROM KMHAD063M L
     * INNER JOIN KMHAD055M G ON L.NINAM_SNO = G.NINAM_SNO
     * WHERE L.CMPTT_SNO = :cmpttSno
     *   AND L.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * ORDER BY L.FIRST_CRT_DT ASC
     */
    public HashMap<String, Object> getLearningInfoByCompetition(Long cmpttSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("CMPTT_SNO", cmpttSno);
        return executeRequest(param, "/KMHAD063M/selectByCompetition");
    }

    /**
     * 사용자 학습 통계 (회원번호로 조회)
     * 
     * Oracle SQL:
     * SELECT L.FNPRD_NO,
     *        COUNT(*) AS TOTAL_CNT,
     *        SUM(CASE WHEN L.CRAN_YN = 1 THEN 1 ELSE 0 END) AS CORRECT_CNT,
     *        ROUND(SUM(CASE WHEN L.CRAN_YN = 1 THEN 1 ELSE 0 END) / COUNT(*) * 100, 2) AS CORRECT_RATE
     * FROM KMHAD063M L
     * INNER JOIN KMHAD055M G ON L.NINAM_SNO = G.NINAM_SNO
     * WHERE G.MBR_SNO = :mbrSno
     *   AND L.DTA_DEL_YN = 'N'
     *   AND G.DTA_DEL_YN = 'N'
     * GROUP BY L.FNPRD_NO
     * ORDER BY L.FNPRD_NO
     */
    public HashMap<String, Object> getLearningStats(Long mbrSno) throws KinfaRunException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("MBR_SNO", mbrSno);
        return executeRequest(param, "/KMHAD063M/selectStats");
    }
}
