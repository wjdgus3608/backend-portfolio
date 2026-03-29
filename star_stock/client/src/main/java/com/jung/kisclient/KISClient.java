package com.jung.kisclient;

import com.jung.domain.order.OrderStockReqDTO;
import com.jung.domain.order.OrderType;
import com.jung.domain.stock.Stock;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

@Service("commonApiClient")
@Slf4j
public class KISClient implements CommonApiClient{

    private final String BASE_DOMAIN_URL = "https://openapi.koreainvestment.com:9443";
    private final String HOLIDAY_CHECK_URL = "/uapi/domestic-stock/v1/quotations/chk-holiday";
    private final String NOW_PRICE_URL = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private final String DAILY_PRICE_URL = "/uapi/domestic-stock/v1/quotations/inquire-daily-price";
    private final String DAILY_PRICE_OVER30_URL = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
    private final String BUY_PERSON_URL = "/uapi/domestic-stock/v1/quotations/inquire-investor";
    private final String MY_BOX_URL = "/uapi/domestic-stock/v1/trading/inquire-balance";
    private final String ORDER_STOCK_URL = "/uapi/domestic-stock/v1/trading/order-cash";
    private final String SELL_POSSIBLE_STOCK_URL = "/uapi/domestic-stock/v1/trading/inquire-psbl-sell";
    private final int API_CALL_PER_SECOND = 15;



    @Value("${my.app-key}")
    private String appKey;

    @Value("${my.app-secret}")
    private String appSecret;

    //발급토큰
    private String accessToken = "";
    //발급토큰 만료시간
    private String accessTokenExpiredAt = "";
    //API 호출 수
    private int callCnt = 0;



    @Override
    public ResponseEntity<?> callMarketDayInfo() {

        callOAuth();

        WebClient webClient = WebClient.builder()
                .baseUrl(BASE_DOMAIN_URL)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set("content-type","application/json; charset=utf-8");
                    httpHeaders.set("authorization","Bearer "+accessToken);
                    httpHeaders.set("appkey",appKey);
                    httpHeaders.set("appsecret",appSecret);
                    httpHeaders.set("tr_id","CTCA0903R");
                    httpHeaders.set("custtype","P");
                    httpHeaders.set(HttpHeaders.CONNECTION, "close");
                })
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(60))))
                .build();

        Date date = new Date();
        SimpleDateFormat f1 = new SimpleDateFormat("yyyyMMdd");
        String nowDate = f1.format(date);

        JSONObject response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(HOLIDAY_CHECK_URL)
                        .queryParam("BASS_DT", nowDate)
                        .queryParam("CTX_AREA_NK", "")
                        .queryParam("CTX_AREA_FK", "")
                        .build())
                .retrieve()
                .bodyToMono(JSONObject.class)
                .block();

        increaseCallCnt();

//        log.info(response.toString());

        return ResponseEntity.ok(response);
    }

    //토큰 발급함수
    @Override
    public ResponseEntity<?> callOAuth() {
        if(isNeedToNewOauth()) {
            try {
                log.info("accessTokenExpired!! : " + accessTokenExpiredAt);
                WebClient webClient = WebClient.builder()
                        .baseUrl(BASE_DOMAIN_URL)
                        .defaultHeader("content-type", "application/json; charset=utf-8")
                        .build();
                JSONObject json = new JSONObject();
                json.put("grant_type", "client_credentials");
                json.put("appkey", appKey);
                json.put("appsecret", appSecret);


                JSONObject response = webClient.post()
                        .uri("/oauth2/tokenP")
                        .body(BodyInserters.fromValue(json))
                        .retrieve()
                        .bodyToMono(JSONObject.class)
                        .block();

                accessToken = (String) response.get("access_token");
                accessTokenExpiredAt = (String) response.get("access_token_token_expired");

                increaseCallCnt();
            }
            catch (Exception e){
                log.info("토큰 발급 에러" + e.toString());
            }
        }
        return ResponseEntity.ok().build();
    }

    //토큰 만료시간 지났거나 토큰 받지 않았는지 판단하는 함수
    private boolean isNeedToNewOauth(){
        if(accessTokenExpiredAt.isBlank() || isTimeNearSixHour())
            return true;
        return false;
    }

    private boolean isTimeNearSixHour(){
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date saved = null;

        try {
            saved = sdf.parse(accessTokenExpiredAt);
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(now);
            cal1.add(Calendar.HOUR , 6);
            now = new Date(cal1.getTimeInMillis());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return now.after(saved);
    }

    //주식현재가 시세 API
    @Override
    public ResponseEntity<?> callStockNowPrice(Stock input) {

        callOAuth();

        try {
            WebClient webClient = generateDefaultWebClient(accessToken, "FHKST01010100");

            JSONObject response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(NOW_PRICE_URL)
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                            .build())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();
            increaseCallCnt();
            return ResponseEntity.ok(response);
        }
        catch(Exception e){
            try {
                log.warn("오류발생 :"+e);
                log.info("재시도");
                WebClient webClient = generateDefaultWebClient(accessToken, "FHKST01010100");

                JSONObject response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(NOW_PRICE_URL)
                                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                                .build())
                        .retrieve()
                        .bodyToMono(JSONObject.class)
                        .block();
                increaseCallCnt();
                return ResponseEntity.ok(response);
            }
            catch (Exception e2){
                log.error(input.getStockName()+" 오류로 패스처리");
                return ResponseEntity.internalServerError().build();
            }
        }
    }

    //주식현재가 일자별 API
    @Override
    public ResponseEntity<?> callStockDailyPrice(Stock input) {
        callOAuth();

        try {
            WebClient webClient = generateDefaultWebClient(accessToken, "FHKST01010400");

            JSONObject response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(DAILY_PRICE_URL)
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_ORG_ADJ_PRC", "1")
                            .build())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();

            increaseCallCnt();

            return ResponseEntity.ok(response);
        }
        catch (Exception e){
            log.warn("오류발생 :"+e);
            log.info("재시도");
            WebClient webClient = generateDefaultWebClient(accessToken, "FHKST01010400");

            JSONObject response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(DAILY_PRICE_URL)
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_ORG_ADJ_PRC", "1")
                            .build())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();

            increaseCallCnt();

            return ResponseEntity.ok(response);
        }
    }

    //주식현재가 투자자 API
    @Override
    public ResponseEntity<?> callStockBuyPerson(Stock input) {
        callOAuth();


        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(BASE_DOMAIN_URL)
                    .defaultHeaders(httpHeaders -> {
                        httpHeaders.set("content-type","application/json; charset=utf-8");
                        httpHeaders.set("authorization","Bearer "+accessToken);
                        httpHeaders.set("appkey",appKey);
                        httpHeaders.set("appsecret",appSecret);
                        httpHeaders.set("tr_id","FHKST01010900");
                        httpHeaders.set(HttpHeaders.CONNECTION, "close");
                    })
                    .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                            .responseTimeout(Duration.ofSeconds(60))))
                    .build();

            JSONObject response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(BUY_PERSON_URL)
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                            .build())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();

            increaseCallCnt();
//            log.info(response.toString());
            return ResponseEntity.ok(response);
        }
        catch (Exception e){
            log.warn("오류발생 :"+e);
            log.info("재시도");
            WebClient webClient = WebClient.builder()
                    .baseUrl(BASE_DOMAIN_URL)
                    .defaultHeaders(httpHeaders -> {
                        httpHeaders.set("content-type","application/json; charset=utf-8");
                        httpHeaders.set("authorization","Bearer "+accessToken);
                        httpHeaders.set("appkey",appKey);
                        httpHeaders.set("appsecret",appSecret);
                        httpHeaders.set("tr_id","FHKST01010900");
                        httpHeaders.set(HttpHeaders.CONNECTION, "close");
                    })
                    .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                            .responseTimeout(Duration.ofSeconds(60))))
                    .build();

            JSONObject response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(BUY_PERSON_URL)
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                            .build())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();

            increaseCallCnt();
//            log.info(response.toString());
            return ResponseEntity.ok(response);
        }



    }

    @Override
    public ResponseEntity<?> callStockNowPriceOver30(Stock input) {
        callOAuth();

        try {
            WebClient webClient = generateDefaultWebClient(accessToken, "FHKST03010100");

            String[] todayAnd100DaysAgo = getTodayAnd100DaysAgo();

            JSONObject response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(DAILY_PRICE_OVER30_URL)
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                            .queryParam("FID_INPUT_DATE_1", todayAnd100DaysAgo[1])
                            .queryParam("FID_INPUT_DATE_2", todayAnd100DaysAgo[0])
                            .queryParam("FID_PERIOD_DIV_CODE","D")
                            .queryParam("FID_ORG_ADJ_PRC", "1")
                            .build())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();
            increaseCallCnt();
            return ResponseEntity.ok(response);
        }
        catch(Exception e){
            log.warn("오류발생 :"+e);
            log.info("재시도");
            WebClient webClient = generateDefaultWebClient(accessToken, "FHKST03010100");
            String[] todayAnd100DaysAgo = getTodayAnd100DaysAgo();
            JSONObject response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(DAILY_PRICE_OVER30_URL)
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                            .queryParam("FID_INPUT_DATE_1", todayAnd100DaysAgo[1])
                            .queryParam("FID_INPUT_DATE_2", todayAnd100DaysAgo[0])
                            .queryParam("FID_PERIOD_DIV_CODE","D")
                            .queryParam("FID_ORG_ADJ_PRC", "1")
                            .build())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();
            increaseCallCnt();
            return ResponseEntity.ok(response);
        }
    }

    @Override
    public ResponseEntity<?> callMyBox(String acno) {
        callOAuth();

        try {
            WebClient webClient = generateDefaultWebClient(accessToken, "TTTC8434R");

            JSONObject response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(MY_BOX_URL)
                            .queryParam("CANO", acno)
                            .queryParam("ACNT_PRDT_CD", "01")
                            .queryParam("AFHR_FLPR_YN", "N")
                            .queryParam("OFL_YN", "")
                            .queryParam("INQR_DVSN", "02")
                            .queryParam("UNPR_DVSN", "01")
                            .queryParam("FUND_STTL_ICLD_YN", "N")
                            .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")
                            .queryParam("PRCS_DVSN", "01")
                            .queryParam("CTX_AREA_FK100", "")
                            .queryParam("CTX_AREA_NK100", "")
                            .build())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();
            increaseCallCnt();
            return ResponseEntity.ok(response);
        }
        catch(Exception e){
            log.error("오류발생 :"+e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<?> callOrderStock(OrderStockReqDTO reqDTO) {
        callOAuth();

        try {
            //type : 0 - 매수, 1 - 매도
            WebClient webClient = generateDefaultWebClient(accessToken, (reqDTO.getOrderType().equals(OrderType.BUY) ? "TTTC0012U" : "TTTC0011U"));

            JSONObject json = new JSONObject();
            json.put("CANO", reqDTO.getAcno());
            json.put("ACNT_PRDT_CD", "01");
            json.put("PDNO", reqDTO.getStockShortCode());
            json.put("SLL_TYPE", "01");
            json.put("ORD_DVSN", "01");
            json.put("ORD_QTY", reqDTO.getAmount());
            json.put("ORD_UNPR", "0");
            json.put("CNDT_PRIC", "");
            json.put("EXCG_ID_DVSN_CD", "KRX");

            JSONObject response = webClient.post()
                    .uri(ORDER_STOCK_URL)
                    .body(BodyInserters.fromValue(json))
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();
            log.info(response.toString());
            increaseCallCnt();
            return ResponseEntity.ok(response);
        }
        catch(Exception e){
            log.error("오류발생 :"+e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<?> callSellPossibleStockAmount(String acno, String stockShortCode) {
        callOAuth();

        try {
            WebClient webClient = generateDefaultWebClient(accessToken, "TTTC8408R");

            JSONObject response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SELL_POSSIBLE_STOCK_URL)
                            .queryParam("CANO", acno)
                            .queryParam("ACNT_PRDT_CD", "01")
                            .queryParam("PDNO", stockShortCode)
                            .build())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();
            increaseCallCnt();
            return ResponseEntity.ok(response);
        }
        catch(Exception e){
            log.error("오류발생 :"+e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String[] getTodayAnd100DaysAgo() {
        // 날짜 포맷터 정의
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        // 오늘 날짜
        LocalDate today = LocalDate.now();
        // 100일 전 날짜
        LocalDate hundredDaysAgo = today.minusDays(100);

        // 포맷에 맞게 문자열로 변환
        String todayStr = today.format(formatter);
        String hundredDaysAgoStr = hundredDaysAgo.format(formatter);

        return new String[]{todayStr, hundredDaysAgoStr};
    }

    //공통 도메인 및 헤더 세팅
    private WebClient generateDefaultWebClient(String accessToken, String trId){
        return WebClient.builder()
                .baseUrl(BASE_DOMAIN_URL)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set("content-type","application/json; charset=utf-8");
                    httpHeaders.set("authorization","Bearer "+accessToken);
                    httpHeaders.set("appkey",appKey);
                    httpHeaders.set("appsecret",appSecret);
                    httpHeaders.set("tr_id",trId);
                    httpHeaders.set("custtype","P");
                    httpHeaders.set(HttpHeaders.CONNECTION, "close");
                })
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(60))))
                .build();
    }

    //API 호출량 제어 함수
    private void increaseCallCnt(){
        callCnt++;

        if(callCnt>=API_CALL_PER_SECOND){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            callCnt=0;
        }

    }
}
