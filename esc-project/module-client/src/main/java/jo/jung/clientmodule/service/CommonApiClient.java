package jo.jung.clientmodule.service;

import jo.jung.domain.api.*;
import jo.jung.domain.stock.Stock;
import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface CommonApiClient {
    //주식 마켓정보조회
    Mono<JSONObject> callMarketDayInfo();
    Mono<ResponseEntity<?>> callOAuth();
    //주식 현재가조회
    Mono<JSONObject> callStockNowPrice(Stock input);
    //주식 일봉조회
    Mono<JSONObject> callStockDailyPrice(Stock input);
    //주식 투자자정보조회
    Mono<JSONObject> callStockBuyPerson(Stock input);

    //신규
    //주식일별분봉조회
    Mono<JSONObject> callStockDailyMinuteCandle(StockDailyMinuteCandleReqDTO input);
    //주식당일분봉조회
    Mono<JSONObject> callStockTodayMinuteCandle(StockTodayMinuteCandleReqDTO input);
    //주식현재가 호가/예상체결
    Mono<JSONObject> callStockHoPrice(Stock input);
    //주식현재가 체결
    Mono<JSONObject> callStockTodayTradeAmount(Stock input);
    //주식현재가 당일시간대별체결
    Mono<JSONObject> callStockTodayTimeTradeAmount(StockTodayTimeTradeAmountReqDTO input);
    //주식잔고조회
    Mono<JSONObject> callGetMyBox();
    //주식잔액조회
    Mono<JSONObject> callGetMyMoney();
    //주식기본조회
    Mono<ResponseEntity<?>> callGetStockInfo(Stock input);
    //거래량순위 조회
    Mono<JSONObject> callGetStockTradeRankInfo(StockTradeRankInfoReqDTO input);
    //상승률순위 조회
    Mono<JSONObject> callGetStockUpDownRankInfo(StockTradeRankInfoReqDTO input);
    //주식 일봉조회(기간별)
    Mono<JSONObject> callStockDailyCandle(StockDailyCandleReqDTO input);

    Mono<JSONObject> callMakeBuyOrder(OrderReqDTO input);
    Mono<JSONObject> callMakeSellOrder(OrderReqDTO input);

}
