package com.jung.app.service.trade;

import com.jung.app.common.PriceUtil;
import com.jung.app.common.TimeUtil;
import com.jung.app.domain.vo.*;
import com.jung.app.domain.vo.em.*;
import com.jung.app.service.account.AccountService;
import com.jung.app.service.api.ApiClientService;
import com.jung.app.service.chart.ChartService;
import com.jung.app.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FakeTradeService implements TradeService{

    private final ApiClientService apiClientService;
    private final ChartService chartService;
    private final OrderService orderService;
    private final AccountService accountService;


    private BoxRange boxRange = null;
    private String[] tickers = {"BTCUSDT","ETHUSDT","XRPUSDT","SOLUSDT"};
    private final double ticket = 2000; //USDT 단위
    private Map<String,StanceType> preStance = new HashMap<>();
    private final CandleTimeType candleTime  = CandleTimeType.ONE_HOUR; //테스팅할 캔들
    private final double mLeverageRate = 20f;
    //주문 삭제경과 분
    private final int mOverMinute = 60;


    @Override
    public void runTrade() {

        //모의트레이딩시만 있는 함수, 목표가 도달시 매도처리
        executeOrders();
        resetOver30MinuteOrders();
        if(accountService.getMoney()<ticket){
            log.info("잔고부족...");
            return;
        }

        for(String ticker : tickers){
            List<Candle> candles = apiClientService.getRecentCandlesByTicker(ticker,candleTime.equals(CandleTimeType.FIVE_MINUTE)?"5m":(candleTime.equals(CandleTimeType.FIFTEEN_MINUTE)?"15m":"1h"),500);
//            StanceType stanceType = detectStanceStatus(candles);
//
//            if(stanceType.equals(StanceType.BOX)){
//                if(!stanceType.equals(preStance.getOrDefault(ticker,null)))
//                    log.info(ticker+" 박스전략 실행");
//                runBoxTrade(ticker, candles);
//            }
//            else if(stanceType.equals(StanceType.RIDE)){
//                if(!stanceType.equals(preStance.getOrDefault(ticker,null)))
//                    log.info(ticker+" 추세전략 실행");
//                runRideTrade(ticker);
//            }
//            else{
//                if(!stanceType.equals(preStance.getOrDefault(ticker,null)))
//                    log.info(ticker+" 관망장이므로 전략을 실행하지 않음.");
//            }
//            preStance.put(ticker,stanceType);

            OrderBlock upOrderBlock = detectUpOrderBlock(candles);
            OrderBlock downOrderBlock = detectDownOrderBlock(candles);
            //type 1은 상승형 오더블럭 탐지(Long 포지션)
            if(upOrderBlock != null){
                makeObPositionAndOrder(ticker, upOrderBlock, candles, 1);
            }

            if(downOrderBlock != null){
                makeObPositionAndOrder(ticker, downOrderBlock, candles, 0);
            }
        }
    }

    private OrderBlock detectUpOrderBlock(List<Candle> candles){
        List<Candle> ableCandles = candles.subList(0,candles.size()-1);
        return chartService.getUpOrderBlock(ableCandles);
    }

    private OrderBlock detectDownOrderBlock(List<Candle> candles){
        List<Candle> ableCandles = candles.subList(0,candles.size()-1);
        return chartService.getDownOrderBlock(ableCandles);
    }

    //모의투자에서의 주문 실행 함수, 현재봉의 시작점 끝점을 기준으로 목표가격도달여부 확인
    private void executeOrders(){
        List<Order> orderList = orderService.getAllOrders();
        List<Position> positionList = accountService.getPositionList();

        for(Order order : orderList){
            BuySellType buySellType = order.getBuySellType();
            LongShortType longShortType = order.getLongShortType();
            String stockName = order.getStock().getStockName();
            double orderPrice = order.getOrderPrice();
            Candle lastCandle = apiClientService.getRecentCandlesByTicker(stockName,
                    candleTime.equals(CandleTimeType.FIVE_MINUTE)?"5m":(candleTime.equals(CandleTimeType.FIFTEEN_MINUTE)?"15m":"1h"), 1).getLast();
            //매수
            if(buySellType.equals(BuySellType.BUY)){
                if(longShortType.equals(LongShortType.LONG)){
                    //이번캔들종가(현재가)가 매수가보다 아래면 매수가에 매수
                    if (lastCandle.getEndPrice() <= orderPrice) {
                        double amount = order.getOrderAmount();
                        //잔고 감액
                        accountService.setMoney(accountService.getMoney()-(amount*orderPrice));

                        //포지션 생성
                        Position position = Position.builder()
                                .orderAmount(amount)
                                .orderPrice(orderPrice)
                                .leverageRate(order.getLeverageRate())
                                .longShortType(order.getLongShortType())
                                .stock(order.getStock())
                                .timeAt(TimeUtil.getNowTime())
                                .build();
                        accountService.createPosition(position);
                        log.info(stockName+" 매수 "+order.getLongShortType().name()+"(가격/잔고/자산/보유수) : "+orderPrice+"/"+accountService.getMoney()+"/"+accountService.getTotalValue()+"/"+accountService.getPositionList().size());
                        order.setOrderStatus(OrderStatus.FILLED);
                    }
                }
                else if(longShortType.equals(LongShortType.SHORT)){
                    //이번캔들종가(현재가)가 매수가보다 위면 매수가에 매수
                    if(lastCandle.getEndPrice() >= orderPrice) {
                        double amount = order.getOrderAmount();
                        //잔고 감액
                        accountService.setMoney(accountService.getMoney()-(amount*orderPrice));

                        //포지션 생성
                        Position position = Position.builder()
                                .orderAmount(amount)
                                .orderPrice(orderPrice)
                                .leverageRate(order.getLeverageRate())
                                .longShortType(order.getLongShortType())
                                .stock(order.getStock())
                                .timeAt(TimeUtil.getNowTime())
                                .build();
                        accountService.createPosition(position);
                        log.info(stockName+" 매수 "+order.getLongShortType().name()+"(가격/잔고/자산/보유수) : "+orderPrice+"/"+accountService.getMoney()+"/"+accountService.getTotalValue()+"/"+accountService.getPositionList().size());
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

                    if(longShortType.equals(LongShortType.SHORT)){
                        if((order.getTpOrSl()==0 && lastCandle.getEndPrice() <= orderPrice)
                                ||(order.getTpOrSl()==1 && lastCandle.getEndPrice() >= orderPrice)) {
                            double amount = order.getOrderAmount();
                            double earnMoney = PriceUtil.getEarnMoney(position.getOrderPrice(), orderPrice, amount, position.getLongShortType());
                            //잔고 업데이트
                            accountService.setMoney(accountService.getMoney()+(amount*position.getOrderPrice()+earnMoney));

                            //포지션 삭제
                            Position prePosition = Position.builder()
                                    .longShortType(order.getLongShortType())
                                    .stock(order.getStock())
                                    .build();
                            accountService.deletePosition(prePosition);
                            log.info(stockName+" 매도 "+order.getLongShortType().name()+"(가격/손익/잔고/자산/보유수) : "+orderPrice+"/"+earnMoney+"/"+accountService.getMoney()+"/"+accountService.getTotalValue()+"/"+accountService.getPositionList().size());
                            order.setOrderStatus(OrderStatus.FILLED);

                            // 🔥 같은 UUID를 가진 나머지 매도 주문 제거
                            markPairedSellOrdersCanceled(order.getOrderId());
                        }
                    }
                    else if(longShortType.equals(LongShortType.LONG)){
                        if((order.getTpOrSl()==0 && lastCandle.getEndPrice() >= orderPrice)
                                ||(order.getTpOrSl()==1 && lastCandle.getEndPrice() <= orderPrice)) {
                            double amount = order.getOrderAmount();
                            double earnMoney = PriceUtil.getEarnMoney(position.getOrderPrice(), orderPrice, amount, position.getLongShortType());
                            //잔고 업데이트
                            accountService.setMoney(accountService.getMoney()+(amount*position.getOrderPrice()+earnMoney));

                            //포지션 삭제
                            Position prePosition = Position.builder()
                                    .longShortType(order.getLongShortType())
                                    .stock(order.getStock())
                                    .build();
                            accountService.deletePosition(prePosition);
                            log.info(stockName+" 매도 "+order.getLongShortType().name()+"(가격/손익/잔고/자산/보유수) : "+orderPrice+"/"+earnMoney+"/"+accountService.getMoney()+"/"+accountService.getTotalValue()+"/"+accountService.getPositionList().size());
                            order.setOrderStatus(OrderStatus.FILLED);

                            // 🔥 같은 UUID를 가진 나머지 매도 주문 제거
                            markPairedSellOrdersCanceled(order.getOrderId());
                        }
                    }


                }
            }

        }

        orderList.removeIf(o->o.getOrderStatus().equals(OrderStatus.FILLED));

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
                    .orderType("1")
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
            orderService.createOrder(shortOrder);

            ///매수시 TP/SL 설정
            Order tpOrder = Order.builder()
                    .orderId(orderId)
                    .orderType("1")
                    .tpOrSl(0)
                    .buySellType(BuySellType.SELL)
                    .longShortType(LongShortType.SHORT)
                    .orderPrice(latestLow.get().getPrice())
                    .orderStatus(OrderStatus.NEW)
                    .orderAmount(amount)
                    .tradeType(TradeType.LIMIT)
                    .stock(Stock.builder().stockName(ticker).build())
                    .timeAt(currentCandle.getTimeAt())
                    .build();
            orderService.createOrder(tpOrder);

            Order slOrder = Order.builder()
                    .orderId(orderId)
                    .orderType("1")
                    .tpOrSl(1)
                    .buySellType(BuySellType.SELL)
                    .longShortType(LongShortType.SHORT)
                    .orderPrice(orderBlock.getPrevCandleLow())
                    .orderStatus(OrderStatus.NEW)
                    .orderAmount(amount)
                    .tradeType(TradeType.LIMIT)
                    .stock(Stock.builder().stockName(ticker).build())
                    .timeAt(currentCandle.getTimeAt())
                    .build();
            orderService.createOrder(slOrder);
                log.info(ticker+" 숏 주문생성(매수가/TP/SL) : "+orderBlockBottomPrice+"/"+latestLow.get().getPrice()+"/"+orderBlock.getPrevCandleLow()+" time at : "+currentCandle.getTimeAt());
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
                    .orderType("1")
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
            orderService.createOrder(longOrder);

            ///매수시 TP/SL 설정
            Order tpOrder = Order.builder()
                    .orderId(orderId)
                    .orderType("1")
                    .tpOrSl(0)
                    .buySellType(BuySellType.SELL)
                    .longShortType(LongShortType.LONG)
                    .leverageRate(1.0f)
                    .orderPrice(latestHigh.get().getPrice())
                    .orderStatus(OrderStatus.NEW)
                    .orderAmount(amount)
                    .tradeType(TradeType.LIMIT)
                    .stock(Stock.builder().stockName(ticker).build())
                    .timeAt(currentCandle.getTimeAt())
                    .build();
            orderService.createOrder(tpOrder);
            Order slOrder = Order.builder()
                    .orderId(orderId)
                    .orderType("1")
                    .tpOrSl(1)
                    .buySellType(BuySellType.SELL)
                    .longShortType(LongShortType.LONG)
                    .leverageRate(1.0f)
                    .orderPrice(orderBlock.getPrevCandleLow())
                    .orderStatus(OrderStatus.NEW)
                    .orderAmount(amount)
                    .tradeType(TradeType.LIMIT)
                    .stock(Stock.builder().stockName(ticker).build())
                    .timeAt(currentCandle.getTimeAt())
                    .build();
            orderService.createOrder(slOrder);
            log.info(ticker+" 롱 주문생성(매수가/TP/SL) : "+orderBlockTopPrice+"/"+latestHigh.get().getPrice()+"/"+orderBlock.getPrevCandleLow()+" time at : "+currentCandle.getTimeAt());
        }


    }

    //매도 한쪽 체결되었으면 나머지 매도 제거
    private void markPairedSellOrdersCanceled(UUID orderId) {
        for (Order o : orderService.getAllOrders()) {
            if (o.getBuySellType() == BuySellType.SELL
                    && o.getOrderStatus() == OrderStatus.NEW
                    && o.getOrderId().equals(orderId)) {

                o.setOrderStatus(OrderStatus.FILLED);
            }
        }
    }

    private void resetOver30MinuteOrders(){
        List<Order> orders = orderService.getAllOrders();
        Set<UUID> removeKeys = new HashSet<>();
        for(Order order : orders){
            if(order.getOrderStatus().equals(OrderStatus.NEW) && order.getBuySellType().equals(BuySellType.BUY)
                    && TimeUtil.isNMinutesPassed(order.getTimeAt(),mOverMinute)){
                order.setOrderStatus(OrderStatus.CANCELED);
                removeKeys.add(order.getOrderId());
            }
        }

        for(Order order : orders){
            if(order.getBuySellType().equals(BuySellType.SELL) && removeKeys.contains(order.getOrderId()))
                order.setOrderStatus(OrderStatus.CANCELED);
        }



        orders.removeIf(o->o.getOrderStatus().equals(OrderStatus.CANCELED));
    }

    private boolean hasTickerOrOrdered(String ticker, LongShortType longShortType){
        List<Order> orderList = orderService.getAllOrders();
        List<Position> positionList = accountService.getPositionList();
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

    //레버리지 적용시 청산가 피하는 함수
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

//    private StanceType detectStanceStatus(List<Candle> candles){
//        List<Candle> ableCandles = candles.subList(0,candles.size()-1);
//        boxRange = detectBoxRange(ableCandles);
//        if(boxRange != null)
//            return StanceType.BOX;
//        return StanceType.STAY;
//    }
//    private BoxRange detectBoxRange(List<Candle> candles){
//        return chartService.getBoxRange(candles);
//    }
//
//

//    private void runBoxTrade(String ticker, List<Candle> candles){
//        List<Candle> ableCandles = candles.subList(0,candles.size()-1);
//        BoxRange box = detectBoxRange(ableCandles);
//        if(box == null || !isBoxFair(ticker, box)) return;
//
//        //매수 신호 탐지
//        double boxTopPrice = box.getTop();
//        double boxBottomPrice = box.getBottom();
//        double boxDiffPrice = boxTopPrice - boxBottomPrice;
//
//        Candle preCandle = candles.get(candles.size()-2);
//        Candle currentCandle = candles.getLast();
//
//        if(!hasTickerOrOrdered(ticker,LongShortType.SHORT)){
//            //숏 매수 신호탐지
//            //손절선 조건도 추가
//            if(boxTopPrice<=preCandle.getTopPrice()
//                    && preCandle.getCandleSign().equals(CandleSign.BLUE)
//                    && currentCandle.getEndPrice()<(boxTopPrice+boxDiffPrice*0.5)
//                    && 3<PriceUtil.getEarnMoney(currentCandle.getEndPrice(),boxTopPrice - boxDiffPrice * 0.6,Math.floor(ticket / currentCandle.getEndPrice() * 10000) / 10000,LongShortType.SHORT)
//                    && 1>((boxTopPrice+boxDiffPrice*0.5)/preCandle.getEndPrice()-1)*mLeverageRate) {
//                log.info(ticker+" box info: (top/bottom/timeAt) "+boxTopPrice+"/"+boxBottomPrice+"/"+box.getStartTimeAt().substring(4)+"~"+box.getEndTimeAt().substring(4));
//                double amount = Math.floor(ticket / currentCandle.getEndPrice() * 10000) / 10000;
//                UUID orderId = UUID.randomUUID();
//                ///숏 매수
//                Order shortOrder = Order.builder()
//                        .orderId(orderId)
//                        .buySellType(BuySellType.BUY)
//                        .longShortType(LongShortType.SHORT)
//                        .leverageRate(1f)
//                        .orderPrice(currentCandle.getEndPrice())
//                        .orderStatus(OrderStatus.NEW)
//                        .orderAmount(amount)
//                        .tradeType(TradeType.LIMIT)
//                        .stock(Stock.builder().stockName(ticker).build())
//                        .timeAt(TimeUtil.getNowTime())
//                        .build();
//
//                orderService.createOrder(shortOrder);
//                ///매수시 TP/SL 설정
//                Order tpOrder = Order.builder()
//                        .orderId(orderId)
//                        .buySellType(BuySellType.SELL)
//                        .longShortType(LongShortType.SHORT)
//                        .leverageRate(1f)
//                        .tpOrSl(0)
//                        .orderPrice(boxTopPrice - boxDiffPrice * 0.6)
//                        .orderStatus(OrderStatus.NEW)
//                        .orderAmount(amount)
//                        .tradeType(TradeType.LIMIT)
//                        .stock(Stock.builder().stockName(ticker).build())
//                        .timeAt(TimeUtil.getNowTime())
//                        .build();
//                orderService.createOrder(tpOrder);
//
//                Order slOrder = Order.builder()
//                        .orderId(orderId)
//                        .buySellType(BuySellType.SELL)
//                        .longShortType(LongShortType.SHORT)
//                        .leverageRate(1f)
//                        .tpOrSl(1)
//                        .orderPrice(boxTopPrice + boxDiffPrice * 0.5)
//                        .orderStatus(OrderStatus.NEW)
//                        .orderAmount(amount)
//                        .tradeType(TradeType.LIMIT)
//                        .stock(Stock.builder().stockName(ticker).build())
//                        .timeAt(TimeUtil.getNowTime())
//                        .build();
//                orderService.createOrder(slOrder);
//
//                log.info("박스 숏주문 생성(매수/TP/SL) : " + boxTopPrice + "/" + (boxTopPrice - boxDiffPrice * 0.6) + "/" + (boxTopPrice + boxDiffPrice * 0.5));
//            }
//        }
//
//        if(!hasTickerOrOrdered(ticker,LongShortType.LONG)) {
//            if(boxBottomPrice>=preCandle.getBottomPrice()
//                    && preCandle.getCandleSign().equals(CandleSign.RED)
//                    && currentCandle.getEndPrice()>(boxBottomPrice-boxDiffPrice*0.5)
//                    && 3<PriceUtil.getEarnMoney(currentCandle.getEndPrice(),boxBottomPrice + boxDiffPrice * 0.6,Math.floor(ticket / currentCandle.getEndPrice() * 10000) / 10000,LongShortType.LONG)
//                    && 1>(1-(boxBottomPrice-boxDiffPrice*0.5)/preCandle.getEndPrice())*mLeverageRate) {
//                log.info(ticker+" box info: (top/bottom/timeAt) "+boxTopPrice+"/"+boxBottomPrice+"/"+box.getStartTimeAt().substring(4)+"~"+box.getEndTimeAt().substring(4));
//                double amount = Math.floor(ticket / currentCandle.getEndPrice() * 10000) / 10000;
//                UUID orderId = UUID.randomUUID();
//                ///롱 매수
//                Order longOrder = Order.builder()
//                        .orderId(orderId)
//                        .buySellType(BuySellType.BUY)
//                        .longShortType(LongShortType.LONG)
//                        .leverageRate(1f)
//                        .orderPrice(currentCandle.getEndPrice())
//                        .orderStatus(OrderStatus.NEW)
//                        .orderAmount(amount)
//                        .tradeType(TradeType.LIMIT)
//                        .stock(Stock.builder().stockName(ticker).build())
//                        .timeAt(TimeUtil.getNowTime())
//                        .build();
//                orderService.createOrder(longOrder);
//                ///매수시 TP/SL 설정
//                Order tpOrder = Order.builder()
//                        .orderId(orderId)
//                        .buySellType(BuySellType.SELL)
//                        .longShortType(LongShortType.LONG)
//                        .leverageRate(1f)
//                        .tpOrSl(0)
//                        .orderPrice(boxBottomPrice + boxDiffPrice * 0.6)
//                        .orderStatus(OrderStatus.NEW)
//                        .orderAmount(amount)
//                        .tradeType(TradeType.LIMIT)
//                        .stock(Stock.builder().stockName(ticker).build())
//                        .timeAt(TimeUtil.getNowTime())
//                        .build();
//                orderService.createOrder(tpOrder);
//                Order slOrder = Order.builder()
//                        .orderId(orderId)
//                        .buySellType(BuySellType.SELL)
//                        .longShortType(LongShortType.LONG)
//                        .leverageRate(1f)
//                        .tpOrSl(1)
//                        .orderPrice(boxBottomPrice - boxDiffPrice * 0.5)
//                        .orderStatus(OrderStatus.NEW)
//                        .orderAmount(amount)
//                        .tradeType(TradeType.LIMIT)
//                        .stock(Stock.builder().stockName(ticker).build())
//                        .timeAt(TimeUtil.getNowTime())
//                        .build();
//                orderService.createOrder(slOrder);
//
//                log.info("박스 롱주문 생성(매수/TP/SL) : " + boxBottomPrice + "/" + (boxBottomPrice + boxDiffPrice * 0.6) + "/" + (boxBottomPrice - boxDiffPrice * 0.5));
//            }
//        }
//
//
//    }

    private boolean isBoxFair(String stockName ,BoxRange box){
        Candle lastCandle = apiClientService.getRecentCandlesByTicker(stockName, "5m", 1).getLast();
        if(lastCandle.getEndPrice() > box.getTop()+0.15*box.getAtr() ||
                lastCandle.getEndPrice() < box.getBottom()+0.15*box.getAtr())
            return false;
        return true;
    }

    private void runRideTrade(String ticker){

    }
}
