package jo.jung.faketrademodule.service.runner;

import jakarta.annotation.PostConstruct;
import jo.jung.common.apiclient.ApiUtil;
import jo.jung.common.dateutil.TimeUtil;
import jo.jung.common.logclient.LogUtil;
import jo.jung.domain.api.StockDailyCandleReqDTO;
import jo.jung.domain.api.StockDailyMinuteCandleReqDTO;
import jo.jung.domain.api.StockTradeRankInfoReqDTO;
import jo.jung.domain.candle.Candle;
import jo.jung.domain.candle.CandleSign;
import jo.jung.domain.nowprice.NowPrice;
import jo.jung.domain.stock.Stock;
import jo.jung.faketrademodule.service.account.MyBoxUtil;
import jo.jung.logic.datastore.TemporaryDataStore;
import jo.jung.logic.service.logics.Logic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunService {
    private final Logic logic;
    private final ApiUtil apiUtil;
    private final LogUtil logUtil;
    private final MyBoxUtil myBoxUtil;
    private final TemporaryDataStore dataStore;

    private static volatile String opndYn = "N";
    private static int N = 5; //TOP 몇개 가져올지 변수
    private static int TRADE_MODE = -1;// 0: 거래시간아님, 1:거래시간
    private static boolean isOnceTrade = false;
    private Flux<Stock> cachedStocks;
    private boolean executedOnce = false;

    @Scheduled(cron = "0 0 6 * * *")
    public void updateOpndYn(){
        log.info("주식 휴장여부 확인");
        apiUtil.get("/api/v1/market-day-info", JSONObject.class).subscribe(jsonObject -> {
            opndYn = "N";
            //opnd_yn(주식 개장일)로 장 열렸는지 확인
            String todayStr = TimeUtil.getToday();
            ArrayList<LinkedHashMap> list = ((ArrayList) jsonObject.get("output"));
            for(LinkedHashMap map : list){
                if(((String)map.get("bass_dt")).equals(todayStr)){
                    opndYn = (String)map.get("opnd_yn");
                    break;
                }
            }
            log.info("Y".equals(opndYn) ? "거래일입니다." : "휴장일입니다.");
        });

        isOnceTrade = false;
        executedOnce = false;
        cachedStocks = null;
    }

    // 1분마다 만료된 데이터 삭제
    @Scheduled(fixedRate = 1 * 60 * 1000)
    public void cleanupExpiredData() {
        dataStore.cleanup();
    }

    @PostConstruct
    public void scheduleReactiveTask() {
        updateOpndYn();
        Mono.defer(() -> runServiceMono()
                        .onErrorResume(e -> {
                            log.error("runServiceMono 에러 발생", e);
                            return Mono.empty(); // 에러 무시하고 다음 step으로
                        }))
                .then(Mono.delay(Duration.ofSeconds(1))) // 1초 대기
                .repeat() // 무한 반복
                .subscribe();
    }

    public Mono<Void> runServiceMono(){
        boolean betweenNineAndThree = isBetween9And3();
//        boolean betweenNineAndThree = true;
        if ("Y".equalsIgnoreCase(opndYn) && betweenNineAndThree) {
//          if (true){
            // 개장일인 경우에만 로직 실행
            // 개장일인 경우에만 로직 실행
            Flux<Stock> buyTargets = getBuyTargets();
            Flux<Stock> sellTargets = getSellTargets();
            TRADE_MODE = 1;

            return logic.executeLogic(buyTargets, sellTargets);

        } else {
            if(!betweenNineAndThree) {
                if(TRADE_MODE!=0) {
                    logUtil.saveAndPrintLog("0", "거래실행 시간이 아닙니다. 서비스 실행을 건너뜁니다.");
                    TRADE_MODE = 0;
                }
            }
            else {
                if(TRADE_MODE!=0) {
                    logUtil.saveAndPrintLog("0", "오늘은 휴장일입니다. 서비스 실행을 건너뜁니다.");
                    TRADE_MODE = 0;
                }
            }
            return Mono.empty();
        }

    }

    private Mono<Void> execute(Flux<Stock> sellTargets) {

        TRADE_MODE = 1;

        // ⏰ 09:00:45 이전 → 무조건 empty
        if (isBeforeBuyTime()) {
            return logic.executeLogic(Flux.empty(), sellTargets);
        }

        // 🔥 09:00:45 이후 → 딱 1번만 cached 사용
        if (!executedOnce && cachedStocks != null) {
            executedOnce = true;
            return logic.executeLogic(cachedStocks, sellTargets);
        }

        // ⛔ 이후는 계속 empty
        return logic.executeLogic(Flux.empty(), sellTargets);
    }



    private Flux<Stock> getBuyTargets(){
//        logUtil.saveAndPrintLog("0","매수 대상 받아오기");
        if(!isOnceTrade) {
            StockTradeRankInfoReqDTO preReqDTO = StockTradeRankInfoReqDTO.builder()
                    .FID_RSFL_RATE2("")
                    .FID_COND_MRKT_DIV_CODE("NX")
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

            StockTradeRankInfoReqDTO reqDTO = StockTradeRankInfoReqDTO.builder()
                    .FID_RSFL_RATE2("")
                    .FID_COND_MRKT_DIV_CODE("J")
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

            return apiUtil.post("/api/v1/stock-trade-updown-rank-info", preReqDTO, Stock.class)
                    .map(Stock::getStockShortCode)
                    .collect(Collectors.toSet())       // B Set 준비
                    .flatMapMany(bSet -> {
//                        log.info("bSet.size() : " + bSet);
                                return apiUtil.post("/api/v1/stock-trade-updown-rank-info", reqDTO, Stock.class)
                                        .filter(a -> !bSet.contains(a.getStockShortCode())) // NTX 종목 제외
                                        .filter(a-> {
                                            String stockName = a.getStockName();
                                            return (!stockName.contains("ETN") && !stockName.contains("ETF")
                                                    && !stockName.contains("KODEX") && !stockName.contains("TIGER")
                                                    && !stockName.contains("RISE") && !stockName.contains("ACE")
                                                    && !stockName.contains("PLUS"));
                                        }) //ETN, ETF, 펀드상품 제외
                                        .filterWhen(a ->{
                                                    String yesterday = TimeUtil.getTodayAddDays(-1);
                                                    String today = TimeUtil.getToday();
                                                    StockDailyCandleReqDTO input = StockDailyCandleReqDTO.builder()
                                                            .stock(a)
                                                            .FID_INPUT_DATE_1(yesterday)
                                                            .FID_INPUT_DATE_2(today)
                                                            .build();
                                                    return apiUtil.post("/api/v1/stock-daily-candle", input, Candle.class)
                                                            .index()
                                                            .filter(tuple->{
                                                                long idx = tuple.getT1();
                                                                Candle candle = tuple.getT2();

                                                                //첫번째 캔들(금일캔들)
                                                                if(idx == 0){
//                                                                    return (candle.getStartPrice() <= candle.getEndPrice() // 양봉+보합 필터링
//                                                                            && candle.getStartPrice()*1.03f >= candle.getEndPrice()); // 시가대비 3%이하 상승종목만필터링

                                                                    return candle.getStartPrice() <= candle.getEndPrice(); // 양봉+보합 필터링
                                                                }
                                                                //두번째 캔들(전일캔들)
                                                                else if(idx == 1){
                                                                    return candle.getTradeMoney()>= 1000000000L;
                                                                }
                                                                return true;
                                                            }) // 전일거래대금 10억이상인지 필터링
                                                            .hasElements();
                                                }
                                        )
                                        .take(N)                                         // 최대 N개
                                        .collectList()                                   // 리스트로 모음
                                        .flatMapMany(list -> {
                                            isOnceTrade = (list.size() > 0);            //1개라도 있으면 그만
                                            if(isOnceTrade)
                                                log.info("필터링 후 매수대상 종목 "+list.size()+"개");
                                            return Flux.fromIterable(list);
                                        });
                            }
                    );
        }
        else
            return Flux.empty();
    }

    private Flux<Stock> getSellTargets(){
//        logUtil.saveAndPrintLog("0","매도 대상 받아오기");

        return myBoxUtil.getStockMono().flatMapMany(map ->
                Flux.fromIterable(map.entrySet())
                        .map(Map.Entry::getValue)
        );
    }

    public boolean isBetween9And3() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(9, 00, 05);  // 오전 9시
        LocalTime end = LocalTime.of(15, 30);   // 오후 3시
        return !now.isBefore(start) && !now.isAfter(end);
    }

    public boolean isBeforeBuyTime(){
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(9, 00, 45);  // 오전 9시 00분 45초
        return now.isBefore(start);
    }

}
