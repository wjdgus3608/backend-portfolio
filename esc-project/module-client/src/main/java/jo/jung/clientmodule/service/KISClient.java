package jo.jung.clientmodule.service;

import jo.jung.domain.api.*;
import jo.jung.domain.candle.Candle;
import jo.jung.domain.stock.Stock;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;

@Service
@Slf4j
public class KISClient implements CommonApiClient{

    private final String BASE_DOMAIN_URL = "https://openapi.koreainvestment.com:9443";

    @Value("${my.app-key}")
    private String appKey;

    @Value("${my.app-secret}")
    private String appSecret;

    @Value("${my.account-front}")
    private String accountFront;

    @Value("${my.account-back}")
    private String accountBack;

    //발급토큰
    private String accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0b2tlbiIsImF1ZCI6ImEyN2YyMDY3LTU1NDMtNDQ0Ni1iZmI3LWFhMDIwMTZkNTQ4OCIsInByZHRfY2QiOiIiLCJpc3MiOiJ1bm9ndyIsImV4cCI6MTc3MjQ5NjI4NywiaWF0IjoxNzcyNDA5ODg3LCJqdGkiOiJQU2FWSzM5aFNnQllja1JiSjlkTTFuNDZwS0pZdHBxSVMxRDEifQ.Ogb8up2UKie-eNjUxTMMxmcqCoW3uQlEXIwYjQ13O3zzblEuDiiQacnG2xDUhguahjHjgZecvz23SvlsEM9QeA";
    //발급토큰 만료시간
    private String accessTokenExpiredAt = "";

    private ReactiveRateLimiter rateLimiter = new ReactiveRateLimiter(16); // 초당 20건 제한





    @Override
    public Mono<JSONObject> callMarketDayInfo() {

        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = WebClient.builder()
                            .baseUrl(UrlInfo.HOLIDAY_CHECK.getBaseUrl())
                            .defaultHeaders(httpHeaders -> {
                                httpHeaders.set("content-type","application/json; charset=utf-8");
                                httpHeaders.set("authorization","Bearer "+accessToken);
                                httpHeaders.set("appkey",appKey);
                                httpHeaders.set("appsecret",appSecret);
                                httpHeaders.set("tr_id",UrlInfo.HOLIDAY_CHECK.getTrId());
                                httpHeaders.set("custtype","P");
                                httpHeaders.set(HttpHeaders.CONNECTION, "close");
                            })
                            .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                                    .responseTimeout(Duration.ofSeconds(60))))
                            .build();

                    Date date = new Date();
                    SimpleDateFormat f1 = new SimpleDateFormat("yyyyMMdd");
                    String nowDate = f1.format(date);

                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.HOLIDAY_CHECK.getEndpoint())
                                    .queryParam("BASS_DT", nowDate)
                                    .queryParam("CTX_AREA_NK", "")
                                    .queryParam("CTX_AREA_FK", "")
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class);

                }));

    }

    //토큰 발급함수
    @Override
    public Mono<ResponseEntity<?>> callOAuth() {
        if(!isNeedToNewOauth()) {
            log.info("Oauth req Pass!!");
            return Mono.just(ResponseEntity.ok().build());
        }

        log.info("Oauth req!!");

        return rateLimiter.acquire()
                .then(Mono.defer(()->{
                    log.info("accessTokenExpired!! : "+accessTokenExpiredAt);
                    WebClient webClient = WebClient.builder()
                            .baseUrl(BASE_DOMAIN_URL)
                            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .build();

                    JSONObject json = new JSONObject();
                    json.put("grant_type", "client_credentials");
                    json.put("appkey", appKey);
                    json.put("appsecret", appSecret);

                    return webClient.post()
                            .uri("/oauth2/tokenP")
                            .body(BodyInserters.fromValue(json))
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            })
                            .doOnNext(response -> {
                                accessToken = (String) response.get("access_token");
                                accessTokenExpiredAt = (String) response.get("access_token_token_expired");
                            })
                            .map(response -> ResponseEntity.ok().build());
                }));
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
    public Mono<JSONObject> callStockNowPrice(Stock input) {

        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.NOW_PRICE.getTrId());


                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.NOW_PRICE.getEndpoint())
                                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                    .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));
    }

    //주식현재가 일자별 API
    @Override
    public Mono<JSONObject> callStockDailyPrice(Stock input) {

        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.DAILY_PRICE_URL.getTrId());

                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.DAILY_PRICE_URL.getEndpoint())
                                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                    .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                                    .queryParam("FID_PERIOD_DIV_CODE", "D")
                                    .queryParam("FID_ORG_ADJ_PRC", "1")
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));
    }

    //주식현재가 투자자 API
    @Override
    public Mono<JSONObject> callStockBuyPerson(Stock input) {

        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = WebClient.builder()
                            .baseUrl(BASE_DOMAIN_URL)
                            .defaultHeaders(httpHeaders -> {
                                httpHeaders.set("content-type","application/json; charset=utf-8");
                                httpHeaders.set("authorization","Bearer "+accessToken);
                                httpHeaders.set("appkey",appKey);
                                httpHeaders.set("appsecret",appSecret);
                                httpHeaders.set("tr_id",UrlInfo.BUY_PERSON_URL.getTrId());
                                httpHeaders.set(HttpHeaders.CONNECTION, "close");
                            })
                            .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                                    .responseTimeout(Duration.ofSeconds(60))))
                            .build();

                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.BUY_PERSON_URL.getEndpoint())
                                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                    .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));

    }



    //공통 도메인 및 헤더 세팅
    private WebClient generateDefaultWebClient(String trId){
        return WebClient.builder()
                .baseUrl(BASE_DOMAIN_URL)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE);
                    httpHeaders.set("authorization","Bearer "+accessToken);
                    httpHeaders.set("appkey",appKey);
                    httpHeaders.set("appsecret",appSecret);
                    httpHeaders.set("tr_id",trId);
                    httpHeaders.set("custtype","P");
                    httpHeaders.set(HttpHeaders.CONNECTION, "close");
                    httpHeaders.set("tr_cont","N");
                })
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(60))))
                .build();
    }

    //주식 일별 분봉조회
    @Override
    public Mono<JSONObject> callStockDailyMinuteCandle(StockDailyMinuteCandleReqDTO input) {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.DAILY_MINUTE_CANDLE_URL.getTrId());
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.DAILY_MINUTE_CANDLE_URL.getEndpoint())
                                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                    .queryParam("FID_INPUT_ISCD", input.getStock().getStockShortCode())
                                    .queryParam("FID_INPUT_HOUR_1", input.getFID_INPUT_HOUR_1())
                                    .queryParam("FID_INPUT_DATE_1", input.getFID_INPUT_DATE_1())
                                    .queryParam("FID_PW_DATA_INCU_YN", "Y")
                                    .queryParam("FID_FAKE_TICK_INCU_YN", "Y")
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));
    }

    @Override
    public Mono<JSONObject> callStockTodayMinuteCandle(StockTodayMinuteCandleReqDTO input) {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.TODAY_MINUTE_CANDLE_URL.getTrId());
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.TODAY_MINUTE_CANDLE_URL.getEndpoint())
                                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                    .queryParam("FID_INPUT_ISCD", input.getStock().getStockShortCode())
                                    .queryParam("FID_INPUT_HOUR_1", input.getFID_INPUT_HOUR_1())
                                    .queryParam("FID_PW_DATA_INCU_YN", "Y")
                                    .queryParam("FID_ETC_CLS_CODE", "Y")
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));
    }

    //주식현재가 호가/예상체결
    @Override
    public Mono<JSONObject> callStockHoPrice(Stock input) {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.TODAY_HO_PRICE_URL.getTrId());
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.TODAY_HO_PRICE_URL.getEndpoint())
                                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                    .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));
    }

    //주식현재가 체결
    @Override
    public Mono<JSONObject> callStockTodayTradeAmount(Stock input) {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.TODAY_TRADE_AMOUNT_URL.getTrId());
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.TODAY_TRADE_AMOUNT_URL.getEndpoint())
                                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                    .queryParam("FID_INPUT_ISCD", input.getStockShortCode())
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));
    }

    //주식현재가 당일시간대별체결
    @Override
    public Mono<JSONObject> callStockTodayTimeTradeAmount(StockTodayTimeTradeAmountReqDTO input) {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.TODAY_TIME_TRADE_AMOUNT_URL.getTrId());
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.TODAY_TIME_TRADE_AMOUNT_URL.getEndpoint())
                                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                    .queryParam("FID_INPUT_ISCD", input.getStock().getStockShortCode())
                                    .queryParam("FID_INPUT_HOUR_1", input.getFID_INPUT_HOUR_1())
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));
    }

    @Override
    public Mono<JSONObject> callGetMyBox() {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(
                        Mono.fromCallable(() ->
                                        KisBalanceSync.inquireBalance( BASE_DOMAIN_URL,
                                                        appKey,
                                                        appSecret,
                                                        accessToken,
                                                        accountFront,
                                                        accountBack))
                                .subscribeOn(Schedulers.boundedElastic()) // 🔥 필수
                );
    }

    @Override
    public Mono<JSONObject> callGetMyMoney() {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.GET_MY_MONEY.getTrId());
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.GET_MY_MONEY.getEndpoint())
                                    .queryParam("CANO", accountFront)
                                    .queryParam("ACNT_PRDT_CD", accountBack)
                                    .queryParam("PDNO", "")
                                    .queryParam("ORD_UNPR", "")
                                    .queryParam("ORD_DVSN", "01")
                                    .queryParam("CMA_EVLU_AMT_ICLD_YN", "N")
                                    .queryParam("OVRS_ICLD_YN", "N")
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));
    }


    @Override
    public Mono<ResponseEntity<?>> callGetStockInfo(Stock input) {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.GET_STOCK_INFO.getTrId())
                            .mutate()
                            .filter(logRequest())
                            .build();
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.GET_STOCK_INFO.getEndpoint())
                                    .queryParam("PRDT_TYPE_CD", "300")
                                    .queryParam("PDNO", input.getStockShortCode())
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            })
                            .map(ResponseEntity::ok);
                }));
    }

    @Override
    public Mono<JSONObject> callGetStockTradeRankInfo(StockTradeRankInfoReqDTO input) {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.GET_STOCK_TRADE_RANK_INFO.getTrId())
                            .mutate()
                            .filter(logRequest())
                            .build();
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.GET_STOCK_TRADE_RANK_INFO.getEndpoint())
                                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                    .queryParam("FID_COND_SCR_DIV_CODE", "20171")
                                    .queryParam("FID_INPUT_ISCD", "0000")
                                    .queryParam("FID_DIV_CLS_CODE", "0")
                                    .queryParam("FID_BLNG_CLS_CODE", input.getFID_BLNG_CLS_CODE())
                                    .queryParam("FID_TRGT_CLS_CODE", "111111111")
                                    .queryParam("FID_TRGT_EXLS_CLS_CODE", "1111101000")
                                    .queryParam("FID_INPUT_PRICE_1", input.getFID_INPUT_PRICE_1())
                                    .queryParam("FID_INPUT_PRICE_2", input.getFID_INPUT_PRICE_2())
                                    .queryParam("FID_VOL_CNT", input.getFID_VOL_CNT())
                                    .queryParam("FID_INPUT_DATE_1", "")
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class);

                }));
    }

    @Override
    public Mono<JSONObject> callGetStockUpDownRankInfo(StockTradeRankInfoReqDTO input) {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.GET_STOCK_UPDOWN_RANK_INFO.getTrId())
                            .mutate()
                            .filter(logRequest())
                            .build();
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.GET_STOCK_UPDOWN_RANK_INFO.getEndpoint())
                                    .queryParam("FID_RSFL_RATE2", input.getFID_RSFL_RATE2())
                                    .queryParam("FID_COND_MRKT_DIV_CODE", input.getFID_COND_MRKT_DIV_CODE())
                                    .queryParam("FID_COND_SCR_DIV_CODE", input.getFID_COND_SCR_DIV_CODE())
                                    .queryParam("FID_INPUT_ISCD", input.getFID_INPUT_ISCD())
                                    .queryParam("FID_RANK_SORT_CLS_CODE", input.getFID_RANK_SORT_CLS_CODE())
                                    .queryParam("FID_INPUT_CNT_1", input.getFID_INPUT_CNT_1())
                                    .queryParam("FID_PRC_CLS_CODE", input.getFID_PRC_CLS_CODE())
                                    .queryParam("FID_INPUT_PRICE_1", input.getFID_INPUT_PRICE_1())
                                    .queryParam("FID_INPUT_PRICE_2", input.getFID_INPUT_PRICE_2())
                                    .queryParam("FID_VOL_CNT", input.getFID_VOL_CNT())
                                    .queryParam("FID_TRGT_CLS_CODE", input.getFID_TRGT_CLS_CODE())
                                    .queryParam("FID_TRGT_EXLS_CLS_CODE", input.getFID_TRGT_EXLS_CLS_CODE())
                                    .queryParam("FID_DIV_CLS_CODE", input.getFID_DIV_CLS_CODE())
                                    .queryParam("FID_RSFL_RATE1", input.getFID_RSFL_RATE1())
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class);

                }));
    }


    @Override
    public Mono<JSONObject> callStockDailyCandle(StockDailyCandleReqDTO input) {

        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    WebClient webClient = generateDefaultWebClient(UrlInfo.GET_DAILY_CANDLE.getTrId());

                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(UrlInfo.GET_DAILY_CANDLE.getEndpoint())
                                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                    .queryParam("FID_INPUT_ISCD", input.getStock().getStockShortCode())
                                    .queryParam("FID_INPUT_DATE_1", input.getFID_INPUT_DATE_1())
                                    .queryParam("FID_INPUT_DATE_2", input.getFID_INPUT_DATE_2())
                                    .queryParam("FID_PERIOD_DIV_CODE", "D")
                                    .queryParam("FID_ORG_ADJ_PRC", "1")
                                    .build())
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));
    }

    @Override
    public Mono<JSONObject> callMakeBuyOrder(OrderReqDTO input) {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    JSONObject json = new JSONObject();
                    json.put("CANO", input.getCANO());
                    json.put("ACNT_PRDT_CD", input.getACNT_PRDT_CD());
                    json.put("PDNO", input.getPDNO());
                    json.put("SLL_TYPE", input.getSLL_TYPE());
                    json.put("ORD_DVSN", input.getORD_DVSN());
                    json.put("ORD_QTY", input.getORD_QTY());
                    json.put("ORD_UNPR", input.getORD_UNPR());
                    json.put("CNDT_PRIC", input.getCNDT_PRIC());
                    json.put("EXCG_ID_DVSN_CD", input.getEXCG_ID_DVSN_CD());


                    WebClient webClient = generateDefaultWebClient(UrlInfo.MAKE_BUY_ORDER.getTrId());

                    return webClient.post()
                            .uri(UrlInfo.MAKE_BUY_ORDER.getEndpoint())
                            .body(BodyInserters.fromValue(json))
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));
    }

    @Override
    public Mono<JSONObject> callMakeSellOrder(OrderReqDTO input) {
        return callOAuth()
                .then(rateLimiter.acquire())
                .then(Mono.defer(()->{
                    JSONObject json = new JSONObject();
                    json.put("CANO", input.getCANO());
                    json.put("ACNT_PRDT_CD", input.getACNT_PRDT_CD());
                    json.put("PDNO", input.getPDNO());
                    json.put("SLL_TYPE", input.getSLL_TYPE());
                    json.put("ORD_DVSN", input.getORD_DVSN());
                    json.put("ORD_QTY", input.getORD_QTY());
                    json.put("ORD_UNPR", input.getORD_UNPR());
                    json.put("CNDT_PRIC", input.getCNDT_PRIC());
                    json.put("EXCG_ID_DVSN_CD", input.getEXCG_ID_DVSN_CD());


                    WebClient webClient = generateDefaultWebClient(UrlInfo.MAKE_SELL_ORDER.getTrId());

                    return webClient.post()
                            .uri(UrlInfo.MAKE_SELL_ORDER.getEndpoint())
                            .body(BodyInserters.fromValue(json))
                            .retrieve()
                            .bodyToMono(JSONObject.class)
                            .onErrorResume(WebClientResponseException.class, ex -> {
                                log.info("Status code: " + ex.getStatusCode());
                                log.info("Response body: " + ex.getResponseBodyAsString());
                                return Mono.error(ex); // or fallback
                            });
                }));
    }


    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info(">>> [WebClient Request] {} {}", clientRequest.method(), clientRequest.url());

            clientRequest.headers()
                    .forEach((name, values) -> values.forEach(value ->
                            log.info(">>> Header: {}={}", name, value)));

            return Mono.just(clientRequest);
        });
    }
}
