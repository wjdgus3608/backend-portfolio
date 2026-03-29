package com.jung.backtest.service;

import com.jung.backtest.chart.ChartService;
import com.jung.backtest.chart.ChartServiceImpl;
import com.jung.backtest.common.PriceUtil;
import com.jung.backtest.common.TimeUtil;
import com.jung.backtest.domain.em.*;
import com.jung.backtest.domain.entity.CandleEntity;
import com.jung.backtest.domain.vo.*;
import com.jung.backtest.repo.CandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackTestService {

    private final boolean ORDER_GEN_LOG = false; //주문생성 로그
    private final boolean BUY_SELL_LOG = false; //매수매도 로그
    private final boolean DAILY_LOG = false; //일별수익 로그

    private final ChartService chartService;
    private final CandleRepository candleRepository;

    private String startDate = "20250101"; //백테스트 캔들 시작일
    private String endDate = "20251231"; //백테스트 캔들 종료일
    private double money = 100000; // 가상잔고(USDT)
    private List<Order> orderList = new ArrayList<>(); //가상주문리스트
    private List<Position> positionList = new ArrayList<>(); //가상잔고(보유 종목리스트)
    private final double ticket = 2000; //한번 매수할때 단위(USDT)
    private final CandleTimeType candleTime5m  = CandleTimeType.FIVE_MINUTE; //백테스팅할 캔들
    private final CandleTimeType candleTime15m = CandleTimeType.FIFTEEN_MINUTE; //백테스팅할 캔들
    private final double mLeverageRate = 20f;

    private long totalTradeCnt = 0L;
    private long gainTradeCnt = 0L;
    private long painTradeCnt = 0L;

    private double maxDiff = 0f;
    private double maxDiffBTC = 0f;
    private double maxDiffETH = 0f;
    private double maxDiffXRP = 0f;
    private double maxDiffSOL = 0f;
    private double minGainRate = Double.MAX_VALUE;
    private double minDailyGainRate = Double.MAX_VALUE;
    private double maxGainRate = Double.MIN_VALUE;
    private double maxDailyGainRate = Double.MIN_VALUE;
    private double avgGainRate = 0;
    private double avgDailyGainRate = 0;
    private double maxPainPrice = 0f;
    private double minMoney = money;
    private boolean maxPainContinue = false;
    private double currentPainPrice = 0f;
    private int minMonthCount = Integer.MAX_VALUE;
    private int maxMonthCount = 0;
    private int monthTradeCnt = 0;
    private int testInCnt = 0;
    private int testFinalCnt = 0;




    private final List<String> tickers = List.of(
            "BTCUSDT",
            "ETHUSDT",
            "XRPUSDT",
            "SOLUSDT"
    );//거래할 티커종류

    public void runBackTest(){
        //startDate의 00시00분00초로 현재시간세팅
        //candleTime단위로 현재시간 증가시키면서 시뮬레이션
        //현재시간별 모든 티커 매수 또는 매도 신호탐지
        //DB에서 현재시간기준 최근 500개 캔들 가져와서 신호탐지(부족하면 있는만큼만)
        //신호탐지는 이미 구현되어있는 함수고 아래 조건 지나가면 매수 또는 매도 조건만족으로 판단
        //신호탐지 - BoxRange box = detectBoxRange(ticker);
        //if(box == null || !isBoxFair(ticker, box)) return;
        //매수실행, 매도실행 부분은 내가 구현할거니 주석으로 비워두기
        //매수,매도시 매매시각,  로그출력


        log.info("===== BACK TEST START =====");
        log.info("기간 : {} ~ {}", startDate, endDate);
        log.info("초기자산 : {}", money);

        String currentTime = startDate + "000000";
//        String currentTime = startDate + "055500";
        String endTime = endDate + "235959";
//        String endTime = endDate + "010000";

        // 5분 / 15분 캔들 큐
        Map<String, Deque<Candle>> candle5mQueueMap = new HashMap<>();
        Map<String, Deque<Candle>> candle15mQueueMap = new HashMap<>();

        // =========================
        // 1️⃣ 초기 캔들 로딩
        // =========================
        for (String ticker : tickers) {

            // 5분봉
            List<CandleEntity> entities5m =
                    candleRepository.findTop500ByTickerAndCandleTimeTypeAndTimeAtLessThanEqualOrderByTimeAtDesc(
                            ticker, candleTime5m, currentTime
                    );

            if (entities5m.size() < 500) continue;
            Collections.reverse(entities5m);
            candle5mQueueMap.put(
                    ticker,
                    new ArrayDeque<>(Candle.toCandleList(entities5m))
            );

            // 15분봉
            List<CandleEntity> entities15m =
                    candleRepository.findTop500ByTickerAndCandleTimeTypeAndTimeAtLessThanEqualOrderByTimeAtDesc(
                            ticker, candleTime15m, currentTime
                    );

            if (entities15m.size() < 500) continue;
            Collections.reverse(entities15m);
            candle15mQueueMap.put(
                    ticker,
                    new ArrayDeque<>(Candle.toCandleList(entities15m))
            );
        }

        double monthRate = 0;
        double dayRate = 0;
        double preMonthTotalValue = getTotalValue();
        double preDailyTotalValue = getTotalValue();

        // =========================
        // 2️⃣ 메인 루프 (5분봉 기준)
        // =========================
        while (currentTime.compareTo(endTime) <= 0) {

            for (String ticker : tickers) {
                final String NOW_TIME = currentTime;

                Deque<Candle> deque5m = candle5mQueueMap.get(ticker);
                if (deque5m == null || deque5m.size() < 500) continue;


                List<Candle> candles5m =
                        deque5m.stream()
                                .filter(c -> c.getTimeAt().compareTo(NOW_TIME) <= 0)
                                .collect(Collectors.toList());
                Candle current5m = candles5m.getLast();
                // =========================
                // 📦 5분봉 박스 매수 로직
                // =========================
//                if (isBuySignal(candles5m, current5m)) {
//                    List<Candle> ableCandles = candles5m.subList(0,candles5m.size()-1);
//                    BoxRange box = detectBoxRange(ableCandles);
//                    makeBoxPositionAndOrder(ticker, box, candles5m);
//                }
                //OB로직(상승오더블럭) 탐지
//                if (isBuySignal2(candles5m)) {
//                    List<Candle> ableCandles =
//                            candles5m.subList(0, candles5m.size() - 1);
//
//                    OrderBlock ob = chartService.getUpOrderBlock(ableCandles);
//                    makeObPositionAndOrder(ticker, ob, candles5m, 1);
//                }
//
//                //OB로직(하락오더블럭) 탐지
//                if (isBuySignal3(candles5m)) {
//                    List<Candle> ableCandles =
//                            candles5m.subList(0, candles5m.size() - 1);
//
//                    OrderBlock ob = chartService.getDownOrderBlock(ableCandles);
//                    makeObPositionAndOrder(ticker, ob, candles5m, 0);
//                }

                Deque<Candle> deque15m = candle15mQueueMap.get(ticker);
                if (deque15m == null || deque15m.size() < 500) continue;

                List<Candle> candles15m =
                        deque15m.stream()
                                .filter(c -> c.getTimeAt().compareTo(NOW_TIME) <= 0)
                                .collect(Collectors.toList());


                //OB로직(기준+상위 2개 타임라인 크로스 체크) 탐지
                if (isBuySignal2(candles5m)) {

                    List<Candle> ableCandles =
                            candles5m.subList(0, candles5m.size() - 1);
                    OrderBlock ob = chartService.getUpOrderBlock(ableCandles);
                    List<Candle> ableCandles2 =
                            candles15m.subList(0, candles15m.size() - 1);
//                    OrderBlock ob2 = chartService.getUpOrderBlock(ableCandles2);


                    testInCnt++;
                    List<OrderBlock> recentUpOrderBlocks = chartService.getRecentUpOrderBlocks(ableCandles2, 3);
                    for(OrderBlock ob2 : recentUpOrderBlocks) {

//                        log.info(" ob : "+ob.getBottom()+"~"+ob.getTop()+" "+ob2.getBottom()+"~"+ob2.getTop());
                        if (isObCrossed(ob, ob2)) {
                            OrderBlock crossOb = getCrossOb(ob,ob2);
                            testFinalCnt++;
                            makeObPositionAndOrder(ticker, crossOb, candles5m, 1);

                            break;
                        }
                    }
                }

                //OB로직(하락오더블럭) 탐지
                if (isBuySignal3(candles5m)) {
                    List<Candle> ableCandles =
                            candles5m.subList(0, candles5m.size() - 1);
                    OrderBlock ob = chartService.getDownOrderBlock(ableCandles);
                    List<Candle> ableCandles2 =
                            candles15m.subList(0, candles15m.size() - 1);
//                    OrderBlock ob2 = chartService.getDownOrderBlock(ableCandles2);
                    testInCnt++;
                    List<OrderBlock> recentDownOrderBlocks = chartService.getRecentDownOrderBlocks(ableCandles2, 3);
                    for(OrderBlock ob2 : recentDownOrderBlocks) {

                        if (isObCrossed(ob, ob2)) {
                            OrderBlock crossOb = getCrossOb(ob,ob2);
                            testFinalCnt++;
                            makeObPositionAndOrder(ticker, crossOb, candles5m, 0);
                            break;
                        }
                    }
                }

                // =========================
                // 🧱 15분봉 OB 로직
                // (15분봉 마감 시점에만)
                // =========================
//                if (TimeUtil.isFifteenMinuteClose(currentTime)) {
//
//                    Deque<Candle> deque15m = candle15mQueueMap.get(ticker);
//                    if (deque15m == null || deque15m.size() < 500) continue;
//
//                    List<Candle> candles15m =
//                            deque15m.stream()
//                                    .filter(c -> c.getTimeAt().compareTo(NOW_TIME) <= 0)
//                                    .collect(Collectors.toList());
//
//                    if (isBuySignal2(candles15m)) {
//                        List<Candle> ableCandles =
//                                candles15m.subList(0, candles15m.size() - 1);
//
//                        OrderBlock ob = chartService.getUpOrderBlock(ableCandles);
//                        makeObPositionAndOrder(ticker, ob, candles15m, 1);
//                    }
//
//                    if (isBuySignal3(candles15m)) {
//                        List<Candle> ableCandles =
//                                candles15m.subList(0, candles15m.size() - 1);
//
//                        OrderBlock ob = chartService.getDownOrderBlock(ableCandles);
//                        makeObPositionAndOrder(ticker, ob, candles15m, 0);
//                    }
//                }

                // =========================
                // 주문 처리
                // =========================
                executeOrders(candles5m);
                deletePassedOrders(currentTime);
            }

            // =========================
            // 3️⃣ 다음 캔들로 이동
            // =========================
            String nextTime = TimeUtil.plusCandleTime(currentTime, candleTime5m);

            for (String ticker : tickers) {

                // 5분봉 업데이트
                Deque<Candle> deque5m = candle5mQueueMap.get(ticker);
                if (deque5m != null) {
                    CandleEntity e5m =
                            candleRepository.findTopByTickerAndCandleTimeTypeAndTimeAt(
                                    ticker, candleTime5m, nextTime
                            );
                    if (e5m != null) {
                        deque5m.pollFirst();
                        deque5m.addLast(Candle.toCandle(e5m));
                    }
                }

                // N2분봉 업데이트 (N2분 경계에서만)
                if (TimeUtil.isNMinuteClose(nextTime,candleTime15m.equals(CandleTimeType.FIVE_MINUTE) ? 5 : (candleTime15m.equals(CandleTimeType.FIFTEEN_MINUTE) ? 15 : 60))) {
                    Deque<Candle> deque15m = candle15mQueueMap.get(ticker);
                    if (deque15m != null) {
                        CandleEntity e15m =
                                candleRepository.findTopByTickerAndCandleTimeTypeAndTimeAt(
                                        ticker, candleTime15m, nextTime
                                );
                        if (e15m != null) {
                            deque15m.pollFirst();
                            deque15m.addLast(Candle.toCandle(e15m));
                        }
                    }
                }
            }

            // =========================
            // 월별 수익률
            // =========================
            if (!currentTime.substring(0, 6).equals(nextTime.substring(0, 6))) {
                monthRate = (getTotalValue() - preMonthTotalValue) / 16000 * 100;
                log.info("month : {} monthRate : {}", currentTime.substring(0, 6), monthRate);
                minGainRate = Math.min(minGainRate, monthRate);
                maxGainRate = Math.max(maxGainRate, monthRate);
                avgGainRate += monthRate;
                preMonthTotalValue = getTotalValue();

                minMonthCount = Math.min(monthTradeCnt, minMonthCount);
                maxMonthCount = Math.max(monthTradeCnt, maxMonthCount);
                monthTradeCnt = 0;
            }
            //일별 수익률
            if (!currentTime.substring(0, 8).equals(nextTime.substring(0, 8))) {
                dayRate = (getTotalValue() - preDailyTotalValue) / 16000 * 100;
                if(DAILY_LOG)
                    log.info("day : {} daytotal : {} dayRate : {}", currentTime.substring(0, 8),(getTotalValue() - preDailyTotalValue) ,dayRate);
                minDailyGainRate = Math.min(minDailyGainRate, dayRate);
                maxDailyGainRate = Math.max(maxDailyGainRate, dayRate);
                avgDailyGainRate += dayRate;
                preDailyTotalValue = getTotalValue();
            }

            currentTime = nextTime;
        }

        // =========================
        // 결과 로그
        // =========================
        log.info("===== BACK TEST END =====");
        log.info(
                "최종자산 : {} / 수익금액 : {} / 수익률 : {}% / 총거래횟수 : {}({}/{}) / 승률 : {}",
                getTotalValue(),
                getTotalValue() - 100000f,
                (getTotalValue() - 100000f) / 16000 * 100,
                totalTradeCnt,
                gainTradeCnt,
                painTradeCnt,
                gainTradeCnt * 1.0f / totalTradeCnt * 100f
        );

//        log.info("maxDiff BTC : {}", maxDiffBTC);
//        log.info("maxDiff ETH : {}", maxDiffETH);
//        log.info("maxDiff XRP : {}", maxDiffXRP);
//        log.info("maxDiff SOL : {}", maxDiffSOL);
        log.info("maxPainPrice : {}", maxPainPrice);
        log.info("minMoney : {}", minMoney);
        log.info("daily max/min/avg rate : {}%/{}%/{}%",
                maxDailyGainRate, minDailyGainRate, avgGainRate / 365
        );
        log.info("max/min/avg rate : {}%/{}%/{}%",
                maxGainRate, minGainRate, avgGainRate / 12
        );
        log.info("monthly tradeCnt(max/min) : "+maxMonthCount+"/"+minMonthCount);
//        log.info("test cnt : "+testInCnt+" "+testFinalCnt);
        log.info("total tax {}({}%) ",totalTradeCnt*ticket*2*0.0002,(totalTradeCnt*ticket*2*0.0002)/(getTotalValue() - 100000f)*100);
    }

    private boolean isBoxDetected(List<Candle> candles, Candle currentCandle){
        // 📌 신호 탐지
        BoxRange box = detectBoxRange(candles);
        if (box == null || !isBoxFair(currentCandle, box)) {
            return false;
        }
        return true;
    }

    //박스 시그널
    private boolean isBuySignal(List<Candle> candles, Candle currentCandle){
        List<Candle> ableCandles = candles.subList(0,candles.size()-1);
        if (isBoxDetected(ableCandles, currentCandle)) {
            BoxRange box = detectBoxRange(ableCandles);
            if (box != null && isBoxFair(currentCandle, box)) {
               return true;
            }
        }

        return false;
    }

    //상승오더블럭 시그널
    private boolean isBuySignal2(List<Candle> candles){
        List<Candle> ableCandles = candles.subList(0,candles.size()-1);

        OrderBlock orderBlock = chartService.getUpOrderBlock(ableCandles);
        if(orderBlock == null)
            return false;
        return true;
    }

    //하락오더블럭 시그널
    private boolean isBuySignal3(List<Candle> candles){
        List<Candle> ableCandles = candles.subList(0,candles.size()-1);

        OrderBlock orderBlock = chartService.getDownOrderBlock(ableCandles);
        if(orderBlock == null)
            return false;
        return true;
    }

    private boolean isObCrossed(OrderBlock ob1, OrderBlock ob2){

        if(ob1.getBottom()>ob2.getTop() || ob2.getBottom()>ob1.getTop())
            return false;

        return true;
    }

    private boolean isObContained(OrderBlock ob1, OrderBlock ob2){
        return ob1.getBottom()>=ob2.getBottom() && ob1.getTop()<=ob2.getTop();
    }

    private OrderBlock getCrossOb(OrderBlock ob1, OrderBlock ob2){
        OrderBlock crossOb = new OrderBlock();

        crossOb.setTop(Math.min(ob1.getTop(), ob2.getTop()));
        crossOb.setBottom(Math.max(ob1.getBottom(), ob2.getBottom()));
        crossOb.setPrevCandleLow(ob1.getPrevCandleLow());

        return crossOb;
    }


    private boolean isSellSignal(){
        return true;
    }

    private void makeBoxPositionAndOrder(String ticker, BoxRange box, List<Candle> candles){
        double boxTopPrice = box.getTop();
        double boxBottomPrice = box.getBottom();
        double boxDiffPrice = boxTopPrice - boxBottomPrice;

        Candle currentCandle = candles.getLast();
        Candle preCandle = candles.get(candles.size()-2);

        if(!hasTickerOrOrdered(ticker,LongShortType.SHORT)){
            //숏 매수 신호탐지
            //손절선 조건도 추가
            if(boxTopPrice<=preCandle.getTopPrice()
                    && preCandle.getCandleSign().equals(CandleSign.BLUE)
                    && preCandle.getEndPrice()<(boxTopPrice+boxDiffPrice*0.5)
                    && 1>((boxTopPrice+boxDiffPrice*0.5)/preCandle.getEndPrice()-1)*mLeverageRate){
                double amount = Math.floor(ticket/currentCandle.getStartPrice() * 10000) / 10000;
                UUID orderId = UUID.randomUUID();

                Order shortOrder = Order.builder()
                        .orderId(orderId)
                        .orderType("0")
                        .buySellType(BuySellType.BUY)
                        .longShortType(LongShortType.SHORT)
                        .leverageRate(1f)
                        .orderPrice(currentCandle.getStartPrice())
                        .orderStatus(OrderStatus.NEW)
                        .orderAmount(amount)
                        .tradeType(TradeType.LIMIT)
                        .stock(Stock.builder().stockName(ticker).build())
                        .timeAt(currentCandle.getTimeAt())
                        .build();
                createOrder(shortOrder);



                Order slOrder = Order.builder()
                        .orderId(orderId)
                        .orderType("0")
                        .buySellType(BuySellType.SELL)
                        .longShortType(LongShortType.SHORT)
                        .leverageRate(1f)
                        .orderPrice(boxTopPrice+boxDiffPrice*0.5)
                        .orderStatus(OrderStatus.NEW)
                        .orderAmount(amount)
                        .tradeType(TradeType.LIMIT)
                        .stock(Stock.builder().stockName(ticker).build())
                        .timeAt(currentCandle.getTimeAt())
                        .build();
                createOrder(slOrder);

                ///매수시 TP/SL 설정
                Order tpOrder = Order.builder()
                        .orderId(orderId)
                        .orderType("0")
                        .buySellType(BuySellType.SELL)
                        .longShortType(LongShortType.SHORT)
                        .leverageRate(1f)
                        .orderPrice(boxTopPrice-boxDiffPrice*0.6)
                        .orderStatus(OrderStatus.NEW)
                        .orderAmount(amount)
                        .tradeType(TradeType.LIMIT)
                        .stock(Stock.builder().stockName(ticker).build())
                        .timeAt(currentCandle.getTimeAt())
                        .build();
                createOrder(tpOrder);
                maxDiff = Math.max(maxDiff,(boxDiffPrice*0.5f)/boxTopPrice);
                if(ticker.equals("BTCUSDT"))
                    maxDiffBTC = Math.max(maxDiffBTC,(boxTopPrice+boxDiffPrice*0.5)/currentCandle.getStartPrice()-1);
                else if(ticker.equals("ETHUSDT"))
                    maxDiffETH = Math.max(maxDiffETH,(boxTopPrice+boxDiffPrice*0.5)/currentCandle.getStartPrice()-1);
                else if(ticker.equals("XRPUSDT"))
                    maxDiffXRP = Math.max(maxDiffXRP,(boxTopPrice+boxDiffPrice*0.5)/currentCandle.getStartPrice()-1);
                else if(ticker.equals("SOLUSDT"))
                    maxDiffSOL = Math.max(maxDiffSOL,(boxTopPrice+boxDiffPrice*0.5)/currentCandle.getStartPrice()-1);
            }
        }
        if(!hasTickerOrOrdered(ticker,LongShortType.LONG)){
            //롱 매수 신호탐지
            if(boxBottomPrice>=preCandle.getBottomPrice()
                    && preCandle.getCandleSign().equals(CandleSign.RED)
                    && preCandle.getEndPrice()>(boxBottomPrice-boxDiffPrice*0.5)
                    && 1>(1-(boxBottomPrice-boxDiffPrice*0.5)/preCandle.getEndPrice())*mLeverageRate){
                double amount = Math.floor(ticket/currentCandle.getStartPrice() * 10000) / 10000;
                UUID orderId = UUID.randomUUID();

                Order longOrder = Order.builder()
                        .orderId(orderId)
                        .orderType("0")
                        .buySellType(BuySellType.BUY)
                        .longShortType(LongShortType.LONG)
                        .leverageRate(1.0f)
                        .orderPrice(currentCandle.getStartPrice())
                        .orderStatus(OrderStatus.NEW)
                        .orderAmount(amount)
                        .tradeType(TradeType.LIMIT)
                        .stock(Stock.builder().stockName(ticker).build())
                        .timeAt(currentCandle.getTimeAt())
                        .build();
                createOrder(longOrder);


                Order slOrder = Order.builder()
                        .orderId(orderId)
                        .orderType("0")
                        .buySellType(BuySellType.SELL)
                        .longShortType(LongShortType.LONG)
                        .leverageRate(1.0f)
                        .orderPrice(boxBottomPrice-boxDiffPrice*0.5)
                        .orderStatus(OrderStatus.NEW)
                        .orderAmount(amount)
                        .tradeType(TradeType.LIMIT)
                        .stock(Stock.builder().stockName(ticker).build())
                        .timeAt(currentCandle.getTimeAt())
                        .build();
                createOrder(slOrder);

                ///매수시 TP/SL 설정
                Order tpOrder = Order.builder()
                        .orderId(orderId)
                        .orderType("0")
                        .buySellType(BuySellType.SELL)
                        .longShortType(LongShortType.LONG)
                        .leverageRate(1.0f)
                        .orderPrice(boxBottomPrice+boxDiffPrice*0.6)
                        .orderStatus(OrderStatus.NEW)
                        .orderAmount(amount)
                        .tradeType(TradeType.LIMIT)
                        .stock(Stock.builder().stockName(ticker).build())
                        .timeAt(currentCandle.getTimeAt())
                        .build();
                createOrder(tpOrder);

                maxDiff = Math.max(maxDiff,(boxDiffPrice*0.5f)/boxBottomPrice);
                if(ticker.equals("BTCUSDT"))
                    maxDiffBTC = Math.max(maxDiffBTC,1-(boxBottomPrice-boxDiffPrice*0.5)/currentCandle.getStartPrice());
                else if(ticker.equals("ETHUSDT"))
                    maxDiffETH = Math.max(maxDiffETH,1-(boxBottomPrice-boxDiffPrice*0.5)/currentCandle.getStartPrice());
                else if(ticker.equals("XRPUSDT"))
                    maxDiffXRP = Math.max(maxDiffXRP,1-(boxBottomPrice-boxDiffPrice*0.5)/currentCandle.getStartPrice());
                else if(ticker.equals("SOLUSDT"))
                    maxDiffSOL = Math.max(maxDiffSOL,1-(boxBottomPrice-boxDiffPrice*0.5)/currentCandle.getStartPrice());
            }
        }


    }


    private void makeObPositionAndOrder(String ticker, OrderBlock orderBlock, List<Candle> candles, int type){
        double orderBlockTopPrice = orderBlock.getTop();
        double orderBlockBottomPrice = orderBlock.getBottom();
        double orderBlockDiffPrice = orderBlockTopPrice - orderBlockBottomPrice;

        Candle currentCandle = candles.getLast();
        Candle preCandle = candles.get(candles.size()-2);

        if(type==0 && !hasTickerOrOrdered(ticker,LongShortType.SHORT)){

            //숏 매수 신호탐지
            double amount = Math.floor(ticket/orderBlockBottomPrice * 10000) / 10000;
            UUID orderId = UUID.randomUUID();

            List<SwingPoint> swingPoints = chartService.findSwingPoints(candles.subList(Math.max(0, candles.size() - 30), candles.size() - 1));
            Optional<SwingPoint> latestLow = swingPoints.stream()
                    .filter(sp -> sp.getType() == SwingPointType.LOW)
                    .max(Comparator.comparingInt(SwingPoint::getIndex));
            if(latestLow.isEmpty()) return;
            if(isAllPainCloser(type,orderBlock,mLeverageRate)) return;

            Order shortOrder = Order.builder()
                    .orderId(orderId)
                    .orderType("0")
                    .buySellType(BuySellType.BUY)
                    .longShortType(LongShortType.SHORT)
                    .leverageRate(1.0f)
                    .orderPrice(orderBlockBottomPrice)
                    .orderStatus(OrderStatus.NEW)
                    .orderAmount(amount)
                    .tradeType(TradeType.LIMIT)
                    .stock(Stock.builder().stockName(ticker).build())
                    .timeAt(currentCandle.getTimeAt())
                    .build();
            createOrder(shortOrder);

            ///매수시 TP/SL 설정
            //TP 50% 분할매도 적용
            Order tpOrder = Order.builder()
                    .orderId(orderId)
                    .orderType("0")
//                    .orderType("1")
                    .tpOrSl(0)
                    .buySellType(BuySellType.SELL)
                    .longShortType(LongShortType.SHORT)
                    .orderPrice(latestLow.get().getPrice())
                    .orderStatus(OrderStatus.NEW)
                    .orderAmount(amount)
//                    .orderAmount(amount/2)
                    .tradeType(TradeType.LIMIT)
                    .stock(Stock.builder().stockName(ticker).build())
                    .timeAt(currentCandle.getTimeAt())
                    .build();
            createOrder(tpOrder);
//            Order tpSecondOrder = Order.builder()
//                    .orderId(orderId)
//                    .orderType("0")
//                    .tpOrSl(0)
//                    .buySellType(BuySellType.SELL)
//                    .longShortType(LongShortType.SHORT)
//                    .orderPrice(orderBlockBottomPrice-(orderBlockBottomPrice-latestLow.get().getPrice())*2)
//                    .orderStatus(OrderStatus.NEW)
//                    .orderAmount(amount/2)
//                    .tradeType(TradeType.LIMIT)
//                    .stock(Stock.builder().stockName(ticker).build())
//                    .timeAt(currentCandle.getTimeAt())
//                    .build();
//            createOrder(tpSecondOrder);

            Order slOrder = Order.builder()
                    .orderId(orderId)
                    .orderType("0")
//                    .orderType("1")
                    .tpOrSl(1)
                    .buySellType(BuySellType.SELL)
                    .longShortType(LongShortType.SHORT)
                    .orderPrice(orderBlock.getPrevCandleLow())
                    .orderStatus(OrderStatus.NEW)
                    .orderAmount(amount)
//                    .orderAmount(amount/2)
                    .tradeType(TradeType.LIMIT)
                    .stock(Stock.builder().stockName(ticker).build())
                    .timeAt(currentCandle.getTimeAt())
                    .build();
            createOrder(slOrder);
//            Order slSecondOrder = Order.builder()
//                    .orderId(orderId)
//                    .orderType("0")
//                    .tpOrSl(1)
//                    .buySellType(BuySellType.SELL)
//                    .longShortType(LongShortType.SHORT)
//                    .orderPrice(orderBlockBottomPrice+((orderBlock.getPrevCandleLow()-orderBlockBottomPrice)*2))
//                    .orderStatus(OrderStatus.NEW)
//                    .orderAmount(amount/2)
//                    .tradeType(TradeType.LIMIT)
//                    .stock(Stock.builder().stockName(ticker).build())
//                    .timeAt(currentCandle.getTimeAt())
//                    .build();
//            createOrder(slSecondOrder);



                if(ORDER_GEN_LOG)
                    log.info(ticker+" 숏 주문생성(매수가/TP/SL) : "+orderBlockBottomPrice+"/"+latestLow.get().getPrice()+"/"+orderBlock.getPrevCandleLow()+" time at : "+currentCandle.getTimeAt());
//                maxDiff = Math.max(maxDiff,(boxDiffPrice*0.5f)/boxTopPrice);
                if(ticker.equals("BTCUSDT"))
                    maxDiffBTC = Math.max(maxDiffBTC,((orderBlock.getPrevCandleLow())/orderBlockBottomPrice)-1);
                else if(ticker.equals("ETHUSDT"))
                    maxDiffETH = Math.max(maxDiffETH,((orderBlock.getPrevCandleLow())/orderBlockBottomPrice)-1);
                else if(ticker.equals("XRPUSDT"))
                    maxDiffXRP = Math.max(maxDiffXRP,((orderBlock.getPrevCandleLow())/orderBlockBottomPrice)-1);
                else if(ticker.equals("SOLUSDT"))
                    maxDiffSOL = Math.max(maxDiffSOL,((orderBlock.getPrevCandleLow())/orderBlockBottomPrice)-1);

        }
        if(type==1 && !hasTickerOrOrdered(ticker,LongShortType.LONG)){

            //롱 매수 신호탐지
                double amount = Math.floor(ticket/orderBlockTopPrice * 10000) / 10000;
                UUID orderId = UUID.randomUUID();


            List<SwingPoint> swingPoints = chartService.findSwingPoints(candles.subList(Math.max(0, candles.size() - 30), candles.size() - 1));
            Optional<SwingPoint> latestHigh = swingPoints.stream()
                    .filter(sp -> sp.getType() == SwingPointType.HIGH)
                    .max(Comparator.comparingInt(SwingPoint::getIndex));
            if(latestHigh.isEmpty()) return;
            if(isAllPainCloser(type,orderBlock,mLeverageRate)) return;

            Order longOrder = Order.builder()
                        .orderId(orderId)
                        .orderType("0")
                        .buySellType(BuySellType.BUY)
                        .longShortType(LongShortType.LONG)
                        .leverageRate(1.0f)
                        .orderPrice(orderBlockTopPrice)
                        .orderStatus(OrderStatus.NEW)
                        .orderAmount(amount)
                        .tradeType(TradeType.LIMIT)
                        .stock(Stock.builder().stockName(ticker).build())
                        .timeAt(currentCandle.getTimeAt())
                        .build();
            createOrder(longOrder);

            ///매수시 TP/SL 설정
            //TP 50% 분할매도 적용
            Order tpOrder = Order.builder()
                    .orderId(orderId)
                    .orderType("0")
//                    .orderType("1")
                    .tpOrSl(0)
                    .buySellType(BuySellType.SELL)
                    .longShortType(LongShortType.LONG)
                    .leverageRate(1.0f)
                    .orderPrice(latestHigh.get().getPrice())
                    .orderStatus(OrderStatus.NEW)
                    .orderAmount(amount)
//                    .orderAmount(amount/2)
                    .tradeType(TradeType.LIMIT)
                    .stock(Stock.builder().stockName(ticker).build())
                    .timeAt(currentCandle.getTimeAt())
                    .build();
            createOrder(tpOrder);

//            Order tpSecondOrder = Order.builder()
//                    .orderId(orderId)
//                    .orderType("0")
//                    .tpOrSl(0)
//                    .buySellType(BuySellType.SELL)
//                    .longShortType(LongShortType.LONG)
//                    .leverageRate(1.0f)
//                    .orderPrice(orderBlockTopPrice+(latestHigh.get().getPrice()-orderBlockTopPrice)*2)
//                    .orderStatus(OrderStatus.NEW)
//                    .orderAmount(amount/2)
//                    .tradeType(TradeType.LIMIT)
//                    .stock(Stock.builder().stockName(ticker).build())
//                    .timeAt(currentCandle.getTimeAt())
//                    .build();
//            createOrder(tpSecondOrder);

            Order slOrder = Order.builder()
                    .orderId(orderId)
                    .orderType("0")
//                    .orderType("1")
                    .tpOrSl(1)
                    .buySellType(BuySellType.SELL)
                    .longShortType(LongShortType.LONG)
                    .leverageRate(1.0f)
                    .orderPrice(orderBlock.getPrevCandleLow())
                    .orderStatus(OrderStatus.NEW)
                    .orderAmount(amount)
//                    .orderAmount(amount/2)
                    .tradeType(TradeType.LIMIT)
                    .stock(Stock.builder().stockName(ticker).build())
                    .timeAt(currentCandle.getTimeAt())
                    .build();
            createOrder(slOrder);
//            Order slSecondOrder = Order.builder()
//                    .orderId(orderId)
//                    .orderType("0")
//                    .tpOrSl(1)
//                    .buySellType(BuySellType.SELL)
//                    .longShortType(LongShortType.LONG)
//                    .leverageRate(1.0f)
//                    .orderPrice(orderBlockTopPrice-(orderBlockTopPrice-orderBlock.getPrevCandleLow())*2)
//                    .orderStatus(OrderStatus.NEW)
//                    .orderAmount(amount/2)
//                    .tradeType(TradeType.LIMIT)
//                    .stock(Stock.builder().stockName(ticker).build())
//                    .timeAt(currentCandle.getTimeAt())
//                    .build();
//            createOrder(slSecondOrder);

            if(ORDER_GEN_LOG)
                log.info(ticker+" 롱 주문생성(매수가/TP/SL) : "+orderBlockTopPrice+"/"+latestHigh.get().getPrice()+"/"+orderBlock.getPrevCandleLow()+" time at : "+currentCandle.getTimeAt());
//                maxDiff = Math.max(maxDiff,(orderBlockDiffPrice*0.5f)/orderBlockBottomPrice);

            if(ticker.equals("BTCUSDT"))
                maxDiffBTC = Math.max(maxDiffBTC,1-((orderBlock.getPrevCandleLow())/orderBlockTopPrice));
            else if(ticker.equals("ETHUSDT"))
                maxDiffETH = Math.max(maxDiffETH,1-((orderBlock.getPrevCandleLow())/orderBlockTopPrice));
            else if(ticker.equals("XRPUSDT"))
                maxDiffXRP = Math.max(maxDiffXRP,1-((orderBlock.getPrevCandleLow())/orderBlockTopPrice));
            else if(ticker.equals("SOLUSDT"))
                maxDiffSOL = Math.max(maxDiffSOL,1-((orderBlock.getPrevCandleLow())/orderBlockTopPrice));
        }


    }



    private void executeOrders(List<Candle> candles){
        for(Order order : orderList){
            BuySellType buySellType = order.getBuySellType();
            LongShortType longShortType = order.getLongShortType();
            String stockName = order.getStock().getStockName();
            double orderPrice = order.getOrderPrice();
            Candle lastCandle = candles.getLast();
            if(!order.getOrderStatus().equals(OrderStatus.NEW)) continue;
            //매수
            if(buySellType.equals(BuySellType.BUY)){

                if(order.getTradeType().equals(TradeType.MARKET)){
                    double amount = order.getOrderAmount();
                    //잔고 감액
                    setMoney(getMoney()-(amount*orderPrice));

                    //포지션 생성
                    Position position = Position.builder()
                            .orderAmount(amount)
                            .orderPrice(orderPrice)
                            .leverageRate(order.getLeverageRate())
                            .longShortType(order.getLongShortType())
                            .stock(order.getStock())
                            .timeAt(lastCandle.getTimeAt())
                            .build();
                    createPosition(position);
//                    log.info(stockName+" 매수 "+order.getLongShortType().name()+"["+lastCandle.getTimeAt()+"]"+"(가격/잔고/자산/보유수) : "+orderPrice+"/"+getMoney()+"/"+getTotalValue()+"/"+positionList.size());
                    order.setOrderStatus(OrderStatus.FILLED);
                }
                else if(longShortType.equals(LongShortType.LONG)){

                    //이번캔들에서 하방돌파 했다면
                    if (lastCandle.getTopPrice() >= orderPrice && lastCandle.getBottomPrice() <= orderPrice) {
                        String detailTime = getDetailTime(stockName, orderPrice,lastCandle.getTimeAt());
                        double amount = order.getOrderAmount();
                        //잔고 감액
                        setMoney(getMoney()-(amount*orderPrice));

                        //포지션 생성
                        Position position = Position.builder()
                                .orderAmount(amount)
                                .orderPrice(orderPrice)
                                .leverageRate(order.getLeverageRate())
                                .longShortType(order.getLongShortType())
                                .stock(order.getStock())
                                .timeAt(detailTime)
                                .build();
                        createPosition(position);
                        if(BUY_SELL_LOG)
                            log.info(stockName+" 매수 "+order.getLongShortType().name()+"["+detailTime+"]"+"(가격/잔고/자산/보유수) : "+orderPrice+"/"+getMoney()+"/"+getTotalValue()+"/"+positionList.size());
                        order.setOrderStatus(OrderStatus.FILLED);
                    }
                }
                else if(longShortType.equals(LongShortType.SHORT)){
                    //이번캔들에서 상향돌파 했다면
                    if(lastCandle.getBottomPrice() <= orderPrice && lastCandle.getTopPrice() >= orderPrice) {
                        String detailTime = getDetailTime(stockName, orderPrice, lastCandle.getTimeAt());
                        double amount = order.getOrderAmount();
                        //잔고 감액
                        setMoney(getMoney()-(amount*orderPrice));

                        //포지션 생성
                        Position position = Position.builder()
                                .orderAmount(amount)
                                .orderPrice(orderPrice)
                                .leverageRate(order.getLeverageRate())
                                .longShortType(order.getLongShortType())
                                .stock(order.getStock())
                                .timeAt(detailTime)
                                .build();
                        createPosition(position);
                        if(BUY_SELL_LOG)
                            log.info(stockName+" 매수 "+order.getLongShortType().name()+"["+detailTime+"]"+"(가격/잔고/자산/보유수) : "+orderPrice+"/"+getMoney()+"/"+getTotalValue()+"/"+positionList.size());
                        order.setOrderStatus(OrderStatus.FILLED);
                    }
                }
            }
            //매도
            else{
                Position position = Position.builder()
                        .stock(Stock.builder().stockName(stockName).build())
                        .longShortType(longShortType)
                        .build();
                if(positionList.contains(position)){
                    position = positionList.get(positionList.indexOf(position));


                    if(lastCandle.getBottomPrice() <= orderPrice && lastCandle.getTopPrice() >= orderPrice) {
                        if(!isThisOrderFair(stockName, order, position, lastCandle.getTimeAt())) continue;
                        String detailTime = getDetailTime(stockName, orderPrice, lastCandle.getTimeAt());

//                        double amount = (order.getOrderType().equals("0") ? position.getOrderAmount() : order.getOrderAmount());
                        boolean isAllSellMode = false;
                        if(position.getOrderAmount()<= order.getOrderAmount())
                            isAllSellMode = true;

                        double amount = isAllSellMode ? position.getOrderAmount() : order.getOrderAmount();
                        double earnMoney = PriceUtil.getEarnMoney(position.getOrderPrice(), orderPrice, amount, position.getLongShortType());
                        //잔고 업데이트
                        setMoney(getMoney()+(amount*position.getOrderPrice()+earnMoney));

                        //주문 amount가 포지션의 전부면 포지션 제거
//                        if(order.getOrderType().equals("0")){
                        if(isAllSellMode) {
                            //포지션 삭제
                            Position prePosition = Position.builder()
                                    .longShortType(order.getLongShortType())
                                    .stock(order.getStock())
                                    .build();
                            deletePosition(prePosition);
                        }
                        else{
                            position.setOrderAmount(position.getOrderAmount()-order.getOrderAmount());
                        }
                        totalTradeCnt++;
                        monthTradeCnt++;
                        if(earnMoney>=0) {
                            gainTradeCnt++;
                            maxPainContinue=false;
                        }
                        else {
                            painTradeCnt++;
                            if(maxPainContinue) {
                                currentPainPrice+=(-earnMoney);
                                maxPainPrice = Math.max(maxPainPrice, currentPainPrice);
                            }
                            else
                                currentPainPrice=-earnMoney;
                            minMoney = Math.min(minMoney,getTotalValue());
                            maxPainContinue=true;
                        }
                        if(BUY_SELL_LOG)
                            log.info(stockName+" 매도 "+order.getLongShortType().name()+"["+detailTime+"]"+"(가격/손익/잔고/자산/보유수) : "+orderPrice+"/"+earnMoney+"/"+getMoney()+"/"+getTotalValue()+"/"+positionList.size());
                        order.setOrderStatus(OrderStatus.FILLED);

                        //ordertype이 0이면 부분매도가 아니니 매도 전부 제거(1인경우 매도는 전부유지해야함)
                        if(isAllSellMode) {
                            // 🔥 같은 UUID를 가진 나머지 매도 주문 제거
                            markPairedSellOrdersCanceled(order.getOrderId());
                        }
                    }
                }
            }

        }

        orderList.removeIf(o->o.getOrderStatus().equals(OrderStatus.FILLED));
    }

    private boolean isThisOrderFair(String ticker, Order order, Position position, String targetTime) {

        List<Candle> candles = getOneMinuteCandlesFromOneHour(ticker, targetTime);
        if (candles.size() <= 0) return false;

        int startIdx = findStartIdx(candles, targetTime);
        if (startIdx == -1) {
            log.info("타겟 1시간봉의 첫 1분봉을 찾지 못함. ticker={}", ticker);
            return false;
        }

        int sellTimeAt = findPriceAppearIndex(candles, startIdx, order.getOrderPrice());
        if (sellTimeAt == -1) {
            return false;
        }

        //매수보다 다음순인지
        if (position.getTimeAt().compareTo(candles.get(sellTimeAt).getTimeAt()) >= 0) {
            return false;
        }
        //다른 매도보다 먼저인지
        Order otherOrder = findOtherOrder(order);
        if (otherOrder == null) {
            return false;
        }
        int otherSellTimeAt = findPriceAppearIndex(candles, startIdx, otherOrder.getOrderPrice());
        if (otherSellTimeAt == -1) {
            return true;
        }
        if (candles.get(sellTimeAt).getTimeAt().compareTo(candles.get(otherSellTimeAt).getTimeAt()) >= 0){
            return false;
        }
//        log.info(ticker+" 매도 순서 확인 : "+order.getOrderPrice()+" 가 "+otherOrder.getOrderPrice()+
//                "보다 먼저도달 "+candles.get(sellTimeAt).getTimeAt()+" < "+candles.get(otherSellTimeAt).getTimeAt());

        return true;
    }

    private String getDetailTime(String ticker, double targetPrice, String targetTime){
        List<Candle> candles = getOneMinuteCandlesFromOneHour(ticker, targetTime);
        if(candles.size()<=0) return targetTime;

        int startIdx = findStartIdx(candles, targetTime);
        if(startIdx==-1) return targetTime;

        int appearIdx = findPriceAppearIndex(candles,startIdx,targetPrice);
        if(appearIdx==-1) return targetTime;

        return candles.get(appearIdx).getTimeAt();
    }

    private int findPriceAppearIndex(List<Candle> candles, int startIdx, double targetPrice){
        for(int i=startIdx; i<candles.size()-1; i++){
            Candle candle = candles.get(i);
            if(candle.getTopPrice()>= targetPrice && candle.getBottomPrice() <= targetPrice){
                return i;
            }
        }
        return -1;
    }

    private List<Candle> getOneMinuteCandlesFromOneHour(String ticker, String targetTime){
        String nextHourTime = TimeUtil.plusMinutes(targetTime,60);
        List<CandleEntity> entities =
                candleRepository.findTop500ByTickerAndCandleTimeTypeAndTimeAtLessThanEqualOrderByTimeAtDesc(
                        ticker, CandleTimeType.ONE_MINUTE, nextHourTime
                );

        Collections.reverse(entities);
        return Candle.toCandleList(entities);
    }

    private int findStartIdx(List<Candle> candles,String targetTime){
        int startIdx = -1;
        for(int i=Math.max(0,candles.size()-61); i<candles.size(); i++){
            Candle candle = candles.get(i);
            if(candle.getTimeAt().equals(targetTime)){
                startIdx=i;
                break;
            }
        }

        return startIdx;
    }

    private Order findOtherOrder(Order order){
        for (Order o : orderList) {
            if (o.getBuySellType() == BuySellType.SELL
                    && o.getOrderStatus() == OrderStatus.NEW
                    && o.getOrderId().equals(order.getOrderId())
                    && o.getTpOrSl() != order.getTpOrSl()) {
                return o;
            }
        }
        return null;
    }

    //매도 한쪽 체결되었으면 나머지 매도 제거
    private void markPairedSellOrdersCanceled(UUID orderId) {
        for (Order o : orderList) {
            if (o.getBuySellType() == BuySellType.SELL
                    && o.getOrderStatus() == OrderStatus.NEW
                    && o.getOrderId().equals(orderId)) {

                o.setOrderStatus(OrderStatus.FILLED);
            }
        }
    }

    private double getTotalValue(){
        double value = 0;
        for(Position position : positionList){
            value+= (position.getOrderPrice()* position.getOrderAmount());
        }
        return value+money;
    }

    private void createPosition(Position position){
        positionList.add(position);
    }

    private void deletePosition(Position position){
        positionList.remove(position);
    }

    private double getMoney(){
        return this.money;
    }

    private void setMoney(double money){
        this.money = money;
    }

    private void deletePassedOrders(String nowTime){
        Set<UUID> removeKeys = new HashSet<>();

        for(Order order : orderList){
            if(order.getOrderStatus().equals(OrderStatus.NEW) && order.getBuySellType().equals(BuySellType.BUY)) {
                if (TimeUtil.isNMinutesPassed(order.getTimeAt(), nowTime,
                        candleTime5m.equals(CandleTimeType.FIVE_MINUTE)?5:(candleTime5m.equals(CandleTimeType.FIFTEEN_MINUTE)?15:60))) {
                    order.setOrderStatus(OrderStatus.CANCELED);
                    removeKeys.add(order.getOrderId());
                }
            }
        }

        for(Order order : orderList){
            if(order.getBuySellType().equals(BuySellType.SELL) && removeKeys.contains(order.getOrderId()))
                order.setOrderStatus(OrderStatus.CANCELED);
        }

        orderList.removeIf(o->o.getOrderStatus().equals(OrderStatus.CANCELED));
    }

    private boolean hasTickerOrOrdered(String ticker, LongShortType longShortType){
        for(Order order : orderList){
            if(order.getStock().getStockName().equals(ticker) && order.getLongShortType().equals(longShortType))
                return true;
        }
        for(Position position : positionList){
            if(position.getStock().getStockName().equals(ticker) && position.getLongShortType().equals(longShortType))
                return true;
        }

        return false;
    }

    private void createOrder(Order order){
        orderList.add(order);
    }


    private BoxRange detectBoxRange(List<Candle> candles){
        return chartService.getBoxRange(candles);
    }

    private boolean isBoxFair(Candle lastCandle ,BoxRange box){
        //백테스트만 시작가로 바꿔야할듯
        if(lastCandle.getStartPrice() > box.getTop()+0.15*box.getAtr() ||
                lastCandle.getStartPrice() < box.getBottom()+0.15*box.getAtr())
            return false;
        return true;
    }

    private boolean isAllPainCloser(int type, OrderBlock block, double rate){
        //숏
        if(type==0 && ((block.getPrevCandleLow()/block.getBottom()-1)*100*rate)>=100){
            return true;
        }
        //롱
        else if(type==1 && ((1-block.getPrevCandleLow()/block.getTop())*100*rate)>=100){
            return true;
        }
        return false;
    }
}
