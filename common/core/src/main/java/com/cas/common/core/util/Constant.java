package com.cas.common.core.util;

import org.springframework.stereotype.Component;

/**
 * 공통 상수 정의 클래스
 */
@Component
public class Constant {

    // MCI 관련 상수
    public static final String MCI_SUCCESS_CODE = "0";
    public static final String MCI_ERROR_CODE = "1";
    
    // 메시지 관련 키
    public static final String KEY_SYSTEM_HEADER = "systemHeader";
    public static final String KEY_TRANSACTION_HEADER = "transactionHeader";
    public static final String KEY_RESPONSE = "response";
    public static final String KEY_MESSAGE_HEADER = "messageHeader";
    public static final String KEY_MESSAGE_DATA = "messageData";
    
    // 결과 코드 관련
    public static final String TMSG_PRCS_RSLT_SECD = "TMSG_PRCS_RSLT_SECD";
    public static final String MSG_KDCD = "MSG_KDCD";
    public static final String MSG_PRCS_RSLT_CD = "MSG_PRCS_RSLT_CD";
    public static final String MSGCD = "MSGCD";
    public static final String MSG_CTNS = "MSG_CTNS";
    public static final String MSG_CD = "MSG_CD";
}

