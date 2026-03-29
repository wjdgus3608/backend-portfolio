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
public class RealTradeService implements TradeService{
    private final ApiClientService apiClientService;
    private final ChartService chartService;
    private final OrderService orderService;
    private final AccountService accountService;


    private BoxRange boxRange = null;
    private String[] tickers = {"BTCUSDT","ETHUSDT","XRPUSDT","SOLUSDT"};
    private final double ticket = 2000; //USDT 단위
    private Map<String, StanceType> preStance = new HashMap<>();
    private final CandleTimeType candleTime  = CandleTimeType.FIVE_MINUTE; //백테스팅할 캔들
    private final double mLeverageRate = 20f;
    //주문 삭제경과 분
    private final int mOverMinute = 60;


    @Override
    public void runTrade() {

        resetOver30MinuteOrders();
        if(accountService.getMoney()<ticket){
            log.info("잔고부족...");
            return;
        }

        for(String ticker : tickers){
//            List<Candle> candles = apiClientService.getRecentCandlesByTicker(ticker,"5m",500);
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
//
//            preStance.put(ticker,stanceType);
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
            if(order.getOrderStatus().equals(OrderStatus.NEW) && order.getBuySellType().equals(BuySellType.BUY) && TimeUtil.isNMinutesPassed(order.getTimeAt(),mOverMinute)){
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
