package com.cas.common.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Project      비대면대응개발
 * @Description  HTTP 통신 핸들러
 */
@Component
public class HttpHandler {

    private final Logger logger = LoggerFactory.getLogger(HttpHandler.class);

    @Value("${mci.envr:}")
    private String mciEnvr;

    @Value("${mci.channel.name:}")
    private String mciChNm;

    private String mciChIp;

    @Value("${mci.serverUrl:}")
    private String mciServerUrl;

    @PostConstruct
    public void setIP() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (!iface.isLoopback() && iface.isUp() && !iface.isPointToPoint()) {
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        mciChIp = address.getHostAddress();
                        System.out.println(iface.getName() + ": " + address.getHostAddress());
                    }
                }
            }
        }
    }

    public String getClientIp() {
        String clientIp = "";

        if (Objects.nonNull(RequestContextHolder.getRequestAttributes())) {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            clientIp = req.getRemoteAddr();
        }

        return clientIp;
    }

    private RestTemplate getRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
        
        return restTemplate;
    }

    private HashMap<String, Object> makeHashMap(String json) {
        HashMap<String, Object> result = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            result = mapper.readValue(json, HashMap.class);
        } catch (IOException e) {
            logger.error("JSON 파싱 오류: {}", e.getMessage());
        }
        return result;
    }

    /******************************************************************************
     * @Project      비대면대응개발
     * @Description  MCI POST OBJECT 전송
     * @param        param
     * @param        uri         : URL
     * @throws       KinfaRunException
     * @Source       ADD
     ******************************************************************************/
    public HashMap<String, Object> postForObject(HashMap<String, Object> param, String uri) {
        logger.info("=======================================================================");
        logger.info("■ START CLASS                          HttpHandler");
        logger.info("■ Method                               postForObject");
        logger.info("=======================================================================");
        
        RestTemplate restTemplate = getRestTemplate();
        String json = restTemplate.postForObject(uri, param, String.class);
        HashMap<String, Object> result = makeHashMap(json);
        
        logger.info("=======================================================================");
        logger.info("■ postForObject");
        logger.info("=======================================================================");
        logger.info("       Json Data                       : " + result);
        logger.info("=======================================================================");
        
        return result;
    }

    /******************************************************************************
     * @Project      비대면대응개발
     * @Description  MCI POST STRING 전송
     * @param        param
     * @param        uri         : URL
     * @throws       KinfaRunException
     * @Source       ADD
     ******************************************************************************/
    public String postForString(HashMap<String, Object> param, String uri) {
        RestTemplate restTemplate = getRestTemplate();
        String json = restTemplate.postForObject(uri, param, String.class);
        
        logger.info("postForString param : {}", param);
        logger.info("postForString uri : {}", uri);
        logger.info("postForString result Json : {}", json);

        return json;
    }

    /******************************************************************************
     * @Project      비대면대응개발
     * @Description  MCI POST 권한 전송
     * @param        param
     * @param        ifId        : 인터페이스ID
     * @param        screenId    : 화면ID
     * @throws       KinfaRunException
     * @Source       ADD
     ******************************************************************************/
    public HashMap<String, Object> postToMCI(HashMap<String, Object> param, String ifId, String screenId) throws KinfaRunException {
        logger.info("postToMCI param : {}", param);
        logger.info("postToMCI ifId : {}", ifId);
        logger.info("postToMCI screenId : {}", screenId);

        if (ifId == null || "".equals(ifId) || ifId.length() < 11) {
            logger.error("MCI Interface ID 가 잘못 되었습니다.");
            throw new KinfaRunException("MCI Interface ID 가 잘 못 되었습니다.");
        }

        // MCI Param
        HashMap<String, Object> mciParam = makeMCIParamMap(param, ifId, screenId);
        logger.info("postToMCI mciParam : {} ", mciParam);

        String uri = mciServerUrl;
        RestTemplate restTemplate = getRestTemplate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        logger.info("       uri                             : " + uri);
        logger.info("       ifId                            : " + ifId);
        logger.info("       screenId                        : " + screenId);
        logger.info("       makeMCI Param Map Data          : " + mciParam);
        logger.info("■ Request MCI");
        logger.info("       System Header                   : " 
                + (HashMap<String, Object>) mciParam.get("systemHeader"));
        logger.info("       Transaction Header              : " 
                + (HashMap<String, Object>) mciParam.get("transactionHeader"));

        long timecall = System.currentTimeMillis();
        logger.info("■ MCI Call Start Time                  : " + dateFormat.format(new Date(timecall)));
        logger.info("■ MCI PostForObject");
        
        String jsonData = restTemplate.postForObject(uri, mciParam, String.class);
        logger.info("postToMCI jsonData : {}", jsonData);

        long timeres = System.currentTimeMillis();
        logger.info("■ MCI Call End Time                    : " + dateFormat.format(new Date(timeres)));
        logger.info("■ MCI Call End Sum Time                : " + (timeres - timecall) + "ms");
        logger.info("■ MCI Call Make HashMap");

        HashMap<String, Object> result = makeHashMap(jsonData);
        logger.info("postToMCI makeHashMap result : {}" , result);

        HashMap<String, Object> resultData = null;

        if (result != null) {
            if (result.get("systemHeader") != null) {
                HashMap<String, Object> sysMap = (HashMap<String, Object>) result.get("systemHeader");

                if (sysMap != null
                        && sysMap.get("TMSG_PRCS_RSLT_SECD") != null
                        && "0".equals(sysMap.get("TMSG_PRCS_RSLT_SECD"))) {

                    // 응답정보
                    if (result.get("response") != null) {
                        resultData = (HashMap<String, Object>) result.get("response");

                        // 메세지 정보
                        HashMap<String, Object> msgMap = (HashMap<String, Object>) result.get("messageHeader");

                        if (msgMap != null) {
                            if (msgMap.get("MSG_KDCD").equals("NM")) {
                                msgMap.put("MSG_PRCS_RSLT_CD", "0");
                            } else {
                                msgMap.put("MSG_PRCS_RSLT_CD", "1");
                            }

                            // 메세지정보
                            List<Map<String, String>> msgList 
                                    = (List<Map<String, String>>) msgMap.get("messageData");

                            String messageCd = (String) msgMap.get("MSGCD");
                            String errMessage = (String) msgMap.get("MSG_CTNS");

                            if (msgList != null && msgList.size() > 0) {
                                msgList.get(0).put("MSG_CD", messageCd);
                                msgList.get(0).put("MSG_CTNS", errMessage);
                            } else {
                                Map<String, String> item = new HashMap<String, String>();
                                item.put("MSG_CD", messageCd);
                                item.put("MSG_CTNS", errMessage);
                                List<Map<String, String>> lstMessageData = new ArrayList<Map<String, String>>();
                                lstMessageData.add(item);
                                msgMap.put("messageData", lstMessageData);
                            }

                            resultData.put("msgHeader", msgMap);
                        }
                    }
                } else {
                    logger.error("[MCI ERROR] MCI 결과가 존재하지 않습니다.");
                    throw new KinfaRunException("[MCI ERROR] MCI 결과가 존재하지 않습니다.");
                }
                // 처리결과구분코드(TMSG_PRCS_RSLT_SECD) : 1=시스템오류, 2=Timeout
            } else {
                resultData = (HashMap<String, Object>) result.get("response");
                HashMap<String, Object> msgMap 
                        = (HashMap<String, Object>) result.get("messageHeader");

                if (msgMap != null && msgMap.get("MSG_CTNS") != null) {
                    if (msgMap != null) {
                        if (msgMap.get("MSG_KDCD").equals("NM")) {
                            msgMap.put("MSG_PRCS_RSLT_CD", "0");
                        } else {
                            msgMap.put("MSG_PRCS_RSLT_CD", "1");
                        }

                        List<Map<String, String>> msgList 
                                = (List<Map<String, String>>) msgMap.get("messageData");

                        String messageCd = (String) msgMap.get("MSGCD");
                        String errMessage = (String) msgMap.get("MSG_CTNS");

                        if (msgList != null && msgList.size() > 0) {
                            msgList.get(0).put("MSG_CD", messageCd);
                            msgList.get(0).put("MSG_CTNS", errMessage);
                        } else {
                            Map<String, String> item = new HashMap<String, String>();
                            item.put("MSG_CD", messageCd);
                            item.put("MSG_CTNS", errMessage);
                            List<Map<String, String>> lstMessageData = new ArrayList<Map<String, String>>();
                            lstMessageData.add(item);
                            msgMap.put("messageData", lstMessageData);
                        }

                        resultData.put("msgHeader", msgMap);
                    }
                }
            }
        }

        return resultData;
    }

    /**
     * MCI 파라미터 맵 생성
     */
    private HashMap<String, Object> makeMCIParamMap(HashMap<String, Object> param, String ifId, String screenId) {
        HashMap<String, Object> mciParam = new HashMap<>();
        
        // System Header 생성
        HashMap<String, Object> systemHeader = new HashMap<>();
        systemHeader.put("CHNL_NM", mciChNm);
        systemHeader.put("CHNL_IP", mciChIp);
        systemHeader.put("IF_ID", ifId);
        systemHeader.put("ENVR_DVCD", mciEnvr);
        systemHeader.put("SCRN_ID", screenId);
        
        // Transaction Header 생성
        HashMap<String, Object> transactionHeader = new HashMap<>();
        transactionHeader.put("TX_DT", new SimpleDateFormat("yyyyMMdd").format(new Date()));
        transactionHeader.put("TX_TM", new SimpleDateFormat("HHmmssSSS").format(new Date()));
        
        mciParam.put("systemHeader", systemHeader);
        mciParam.put("transactionHeader", transactionHeader);
        mciParam.put("request", param);
        
        return mciParam;
    }
}

