package jo.jung.clientmodule.service;

import jo.jung.common.apiclient.ApiUtil;
import jo.jung.common.dateutil.TimeUtil;
import jo.jung.common.graph.GraphUtil;
import jo.jung.domain.api.*;
import jo.jung.domain.candle.Candle;
import jo.jung.domain.graph.BollingerBands;
import jo.jung.domain.stock.Stock;
import jo.jung.domain.trade.HoTrade;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KISClientTest {

    @Autowired
    private CommonApiClient commonApiClient; // callStockDailyPrice 가 있는 클래스

    private final String SAMPLE_STOCK_CODE = "005930";
    private final String SAMPLE_DATE = "20250801";
    private final String SAMPLE_DATE2 = "20250731";
    private final String SAMPLE_TIME = "170000";

    @Value("${my.app-key}")
    private String appKey;

    @Value("${my.app-secret}")
    private String appSecret;

    @Value("${my.account-front}")
    private String accountFront;

    @Value("${my.account-back}")
    private String accountBack;



    @BeforeEach
    void beforeEach(TestInfo testInfo) {
        System.out.println("@@ 테스트 실행: "+ testInfo.getDisplayName());
    }

    @Test
    void callMarketDayInfo() {
        // when
        Mono<JSONObject> resultMono = commonApiClient.callMarketDayInfo();

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callOAuth() {
        // when
        Mono<ResponseEntity<?>> resultMono = commonApiClient.callOAuth();

        // then
        ResponseEntity<?> response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("Response JSON: " + response.getBody());
    }

    @Test
    void callStockNowPrice() {
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE); // 삼성전자 예시

        // when
        Mono<JSONObject> resultMono = commonApiClient.callStockNowPrice(stock);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callStockDailyPrice() {
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE); // 삼성전자 예시

        // when
        Mono<JSONObject> resultMono = commonApiClient.callStockDailyPrice(stock);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callStockBuyPerson() {
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE); // 삼성전자 예시

        // when
        Mono<JSONObject> resultMono = commonApiClient.callStockBuyPerson(stock);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callStockDailyMinuteCandle() {
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE); // 삼성전자 예시
        StockDailyMinuteCandleReqDTO reqDTO = StockDailyMinuteCandleReqDTO.builder()
                .stock(stock)
                .FID_INPUT_DATE_1(SAMPLE_DATE)
                .FID_INPUT_HOUR_1(SAMPLE_TIME)
                .build();

        // when
        Mono<JSONObject> resultMono = commonApiClient.callStockDailyMinuteCandle(reqDTO);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callStockTodayMinuteCandle() {
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE); // 삼성전자 예시
        StockTodayMinuteCandleReqDTO reqDTO = StockTodayMinuteCandleReqDTO.builder()
                .stock(stock)
                .FID_INPUT_HOUR_1(SAMPLE_TIME)
                .build();

        // when
        Mono<JSONObject> resultMono = commonApiClient.callStockTodayMinuteCandle(reqDTO);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callStockHoPrice() {
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE); // 삼성전자 예시

        // when
        Mono<JSONObject> resultMono = commonApiClient.callStockHoPrice(stock);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callStockTodayTradeAmount() {
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE); // 삼성전자 예시

        // when
        Mono<JSONObject> resultMono = commonApiClient.callStockTodayTradeAmount(stock);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callStockTodayTimeTradeAmount() {
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE); // 삼성전자 예시
        StockTodayTimeTradeAmountReqDTO reqDTO = StockTodayTimeTradeAmountReqDTO.builder()
                .stock(stock)
                .FID_INPUT_HOUR_1(SAMPLE_TIME)
                .build();

        // when
        Mono<JSONObject> resultMono = commonApiClient.callStockTodayTimeTradeAmount(reqDTO);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callGetMyBox(){

        // when
        Mono<JSONObject> resultMono = commonApiClient.callGetMyBox();

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }


    @Test
    void callGetMyMoney(){

        // when
        Mono<JSONObject> resultMono = commonApiClient.callGetMyMoney();

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callOrderBuy(){
        //given
        OrderReqDTO input = OrderReqDTO.builder()
                .CANO("43115686")
                .ACNT_PRDT_CD("01")
                .PDNO(SAMPLE_STOCK_CODE)
                //.SLL_TYPE()
                .ORD_DVSN("01")//시장가
                .ORD_QTY("1")
                .ORD_UNPR("0")
//                .CNDT_PRIC()
                .EXCG_ID_DVSN_CD("KRX")
                .build();


        // when
        Mono<JSONObject> resultMono = commonApiClient.callMakeBuyOrder(input);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callOrderSell(){
        //given
        OrderReqDTO input = OrderReqDTO.builder()
                .CANO("43115686")
                .ACNT_PRDT_CD("01")
                .PDNO(SAMPLE_STOCK_CODE)
                .SLL_TYPE("01")
                .ORD_DVSN("01")//시장가
                .ORD_QTY("1")
                .ORD_UNPR("0")
//                .CNDT_PRIC()
                .EXCG_ID_DVSN_CD("KRX")
                .build();


        // when
        Mono<JSONObject> resultMono = commonApiClient.callMakeSellOrder(input);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }


    @Test
    void callGetStockInfo() {
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE); // 삼성전자 예시

        // when
        Mono<ResponseEntity<?>> resultMono = commonApiClient.callGetStockInfo(stock);

        // then
        ResponseEntity<?> response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("Response JSON: " + response.getBody());
    }

    @Test
    void callGetStockTradeRankInfo() {
        // given
        StockTradeRankInfoReqDTO reqDTO = StockTradeRankInfoReqDTO.builder()
                .FID_BLNG_CLS_CODE("0")
                .FID_INPUT_PRICE_1("")
                .FID_INPUT_PRICE_2("")
                .FID_VOL_CNT("")
                .FID_INPUT_DATE_1("20250714")
                .build();

        // when
        Mono<JSONObject> resultMono = commonApiClient.callGetStockTradeRankInfo(reqDTO);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void callGetStockUpDownTradeRankInfo() {
        // given
        StockTradeRankInfoReqDTO reqDTO = StockTradeRankInfoReqDTO.builder()
                .FID_RSFL_RATE2("")
                .FID_COND_MRKT_DIV_CODE("J")
//                .FID_COND_MRKT_DIV_CODE("NX")
                .FID_COND_SCR_DIV_CODE("20170")
                .FID_INPUT_ISCD("0000")
                .FID_RANK_SORT_CLS_CODE("0")
                .FID_INPUT_CNT_1("0")
                .FID_PRC_CLS_CODE("1")
                .FID_VOL_CNT("")
                .FID_TRGT_CLS_CODE("0")
                .FID_TRGT_EXLS_CLS_CODE("0")
                .FID_DIV_CLS_CODE("0")
                .FID_RSFL_RATE1("")
                .FID_INPUT_PRICE_1("1000")
                .FID_INPUT_PRICE_2("")
                .build();

        // when
        Mono<JSONObject> resultMono = commonApiClient.callGetStockUpDownRankInfo(reqDTO);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

    @Test
    void timeTest(){
        System.out.println(TimeUtil.calculateTimeDifferenceInMinutes("20250628233100", "20250628230000"));
    }

    @Test
    void tradeGraphTest(){
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE); // 삼성전자 예시

        Mono<Flux<Double>> vwapFluxMono = commonApiClient.callStockTodayTradeAmount(stock)
                .map(json -> {
                    System.out.println(json);
                    ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output");
                    return Flux.fromIterable(outputArray)
                            .map(HoTrade::jsonToHoTradeAmount)
                            .scan(new double[]{0.0, 0.0}, (acc, trade) -> {
                                long price = trade.getTradePrice();
                                long volume = trade.getTradeAmount();
                                acc[0] += price * volume;
                                acc[1] += volume;
                                return acc;
                            })
                            .skip(1)
                            .map(acc -> acc[1] == 0.0 ? 0.0 : acc[0] / acc[1]);
                });

        Flux<Double> vwapFlux = vwapFluxMono.block();

        if (vwapFlux != null) {
            // Flux를 동기적으로 순회하며 출력
            for (Double vwap : vwapFlux.toIterable()) {
                System.out.println("VWAP 흐름: " + vwap);
            }
        }

    }

    @Test
    void bollrinTest(){
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE);
        StockTodayMinuteCandleReqDTO reqDTO = StockTodayMinuteCandleReqDTO.builder()
                .stock(stock)
                .FID_INPUT_HOUR_1(SAMPLE_TIME)
                .build();

        Flux<Candle> candleFlux = commonApiClient.callStockTodayMinuteCandle(reqDTO)
                .flatMapMany(json -> {
                    ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output2");
                    return Flux.fromIterable(outputArray)
                            .map(Candle::minuteJsonToCandle);
                });

        Flux<Candle> preCandleFlux = candleFlux
                .skip(1) // 첫 번째 요소 제외
                .take(20) // 이후 20개만 추출
                .collectList()
                .flatMapMany(list -> {
                    Collections.reverse(list); // 역순 정렬
                    System.out.println("== 볼린저밴드 계산에 사용된 Candle 시간 목록 ==");
                    list.forEach(c -> System.out.println(c.getTimeAt())); // 🔥 timeAt 출력
                    return Flux.fromIterable(list);
                });

        Mono<BollingerBands> bandsMono = GraphUtil.calculateBollingerBands(preCandleFlux);

        // then
        StepVerifier.create(bandsMono)
                .expectNextMatches(band -> {
                    System.out.println("밴드 결과: " + band);
                    return band != null;
                })
                .verifyComplete();
    }

    @Test
    void callStockDailyCandle() {
        // given
        Stock stock = new Stock();
        stock.setStockShortCode(SAMPLE_STOCK_CODE); // 삼성전자 예시

        String startDay = TimeUtil.getTodayAddDays(-2);
        String endDay = TimeUtil.getTodayAddDays(-1);
        System.out.println("start Day = "+startDay);
        StockDailyCandleReqDTO  reqDTO = StockDailyCandleReqDTO.builder()
                .stock(stock)
                .FID_INPUT_DATE_1(startDay)
                .FID_INPUT_DATE_2(endDay)
                .build();
        // when
        Mono<JSONObject> resultMono = commonApiClient.callStockDailyCandle(reqDTO);

        // then
        JSONObject response = resultMono.block(); // 실제 호출

        assertNotNull(response);
        System.out.println("Response JSON: " + response);
    }

}