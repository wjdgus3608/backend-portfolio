package jo.jung.clientmodule.ctrl;

import jo.jung.clientmodule.service.CommonApiClient;
import jo.jung.common.dateutil.TimeUtil;
import jo.jung.domain.api.*;
import jo.jung.domain.candle.Candle;
import jo.jung.domain.candle.CandleSign;
import jo.jung.domain.nowprice.NowPrice;
import jo.jung.domain.stock.Stock;
import jo.jung.domain.trade.HoTrade;
import jo.jung.domain.trade.PersonTrade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ApiController {

    private final CommonApiClient commonApiClient;

    //거래량순위 조회
    @PostMapping("/stock-trade-rank-info")
    public Flux<Stock> callGetStockTradeRankInfo(@RequestBody StockTradeRankInfoReqDTO input){
        return commonApiClient.callGetStockTradeRankInfo(input).flatMapMany(json -> {
            ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output");

            return Flux.fromIterable(outputArray)
                    .map(item -> {
                String stockName = (String) item.get("hts_kor_isnm");
                String stockShortCode = (String) item.get("mksc_shrn_iscd");
                int rank = Integer.parseInt((String) item.get("data_rank"));
                long price = Long.parseLong((String) item.get("stck_prpr"));
                CandleSign candleSign = ((String) item.get("prdy_vrss_sign")).equals("2") ? CandleSign.PLUS : CandleSign.MINUS;
                float rate = Float.parseFloat((String) item.get("prdy_ctrt"));

                return Stock.builder()
                        .stockName(stockName)
                        .stockShortCode(stockShortCode)
                        .rank(rank)
                        .price(price)
                        .candleSign(candleSign)
                        .rate(rate)
                        .build();
            });
        });

    }

    //상승률 순위 조회
    @PostMapping("/stock-trade-updown-rank-info")
    public Flux<Stock> callGetStockUpDownRankInfo(@RequestBody StockTradeRankInfoReqDTO input){

        return commonApiClient.callGetStockUpDownRankInfo(input).flatMapMany(json -> {
            ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output");
            return Flux.fromIterable(outputArray)
                    .map(item -> {
                        String stockName = (String) item.get("hts_kor_isnm");
                        String stockShortCode = (String) item.get("stck_shrn_iscd");
                        int rank = Integer.parseInt((String) item.get("data_rank"));

                        return Stock.builder()
                                .stockName(stockName)
                                .stockShortCode(stockShortCode)
                                .rank(rank)
                                .build();
                    });
        });

    }

    //주식 일별분봉조회
    @PostMapping("/stock-daily-minute-candle")
    public Flux<Candle> callStockDailyMinuteCandle(@RequestBody StockDailyMinuteCandleReqDTO input){
        return commonApiClient.callStockDailyMinuteCandle(input).flatMapMany(json -> {
            ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output2");

            return Flux.fromIterable(outputArray)
                    .map(Candle::minuteJsonToCandle);
        });
    }

    //주식 당일분봉조회
    @PostMapping("/stock-today-minute-candle")
    public Flux<Candle> callStockTodayMinuteCandle(@RequestBody StockTodayMinuteCandleReqDTO input){
        return commonApiClient.callStockTodayMinuteCandle(input).flatMapMany(json -> {
            ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output2");

            return Flux.fromIterable(outputArray)
                    .map(Candle::minuteJsonToCandle);
        });
    }

    //매수 주문
    @PostMapping("/order-buy")
    public Mono<JSONObject> callMakeBuyOrder(@RequestBody OrderReqDTO input){
        return commonApiClient.callMakeBuyOrder(input)
                .flatMap(Mono::just);
    }

    //매도 주문
    @PostMapping("/order-sell")
    public Mono<JSONObject> callMakeSellOrder(@RequestBody OrderReqDTO input){
        return commonApiClient.callMakeSellOrder(input)
                .flatMap(Mono::just);
    }

    //주식현재가 호가/예상체결
    @PostMapping("/stock-ho-price")
    public Mono<HoTrade> callStockHoPrice(@RequestBody Stock input){
        return commonApiClient.callStockHoPrice(input)
                .flatMap(json -> {
                    LinkedHashMap<String, Object> output = (LinkedHashMap<String, Object>) json.get("output1");
                    HoTrade hoTrade = HoTrade.jsonToHoTrade(output);
                    return Mono.just(hoTrade);
                });
    }

    //주식현재가 체결
    @PostMapping("/stock-today-trade-amount")
    public Flux<HoTrade> callStockTodayTradeAmount(@RequestBody Stock input){
        return commonApiClient.callStockTodayTradeAmount(input).flatMapMany(json -> {
            ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output");

            return Flux.fromIterable(outputArray)
                    .map(HoTrade::jsonToHoTradeAmount);
        });
    }

    //주식현재가 당일시간대별체결
    @PostMapping("/stock-today-time-trade-amount")
    public Flux<HoTrade> callStockTodayTimeTradeAmount(@RequestBody StockTodayTimeTradeAmountReqDTO input){
        return commonApiClient.callStockTodayTimeTradeAmount(input).flatMapMany(json -> {
            ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output2");
            return Flux.fromIterable(outputArray)
                    .map(HoTrade::jsonToTimelyTradeAmount);
        });
    }

    //주식잔고조회
    @GetMapping("/my-box")
    public Flux<Stock> callGetMyBox(){
        return commonApiClient.callGetMyBox().flatMapMany(json -> {
            ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output1");
            return Flux.fromIterable(outputArray)
                    .map(item -> {
                        String stockName = (String) item.get("prdt_name");
                        String stockShortCode = (String) item.get("pdno");
                        long price = (long)Double.parseDouble((String) item.get("pchs_avg_pric"));
                        long amount = (long)Double.parseDouble((String) item.get("ord_psbl_qty"));

                        return Stock.builder()
                                .stockName(stockName)
                                .stockShortCode(stockShortCode)
                                .price(price)
                                .hasAmount(amount)
                                .build();
                    });
        });
    }

    //주식잔액조회
    @GetMapping("/my-money")
    public Mono<Long> callGetMyMoney(){
        return commonApiClient.callGetMyMoney().flatMap(json -> {
            LinkedHashMap<String, Object> output = (LinkedHashMap<String, Object>) json.get("output");
            long money = Long.parseLong((String) output.get("ord_psbl_cash"));
            return Mono.just(money);
        });
    }

    //주식기본조회
    @PostMapping("/stock-info")
    public Mono<ResponseEntity<?>> callGetStockInfo(@RequestBody Stock input){
        return commonApiClient.callGetStockInfo(input);
    }

    //주식 마켓정보조회
    @GetMapping("/market-day-info")
    public Mono<JSONObject> callMarketDayInfo(){
        return commonApiClient.callMarketDayInfo();
    }

    //주식 현재가조회
    @PostMapping("/stock-now-price")
    public Mono<NowPrice> callStockNowPrice(@RequestBody Stock input){
        return commonApiClient.callStockNowPrice(input)
                .flatMap(json -> {
                    LinkedHashMap<String, Object> output = (LinkedHashMap<String, Object>) json.get("output");
                    NowPrice nowPrice = NowPrice.jsonToNowPrice(output);
                    return Mono.just(nowPrice);
                });
    }

    //주식 일봉조회
    @PostMapping("/stock-daily-price")
    public Flux<Candle> callStockDailyPrice(@RequestBody Stock input){
        return commonApiClient.callStockDailyPrice(input).flatMapMany(json -> {
            ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output");

            return Flux.fromIterable(outputArray)
                    .map(Candle::dailyCandleJsonToCandle);
        });
    }

    //주식 투자자정보조회
    @PostMapping("/stock-buy-person")
    public Flux<PersonTrade> callStockBuyPerson(@RequestBody Stock input){
        return commonApiClient.callStockBuyPerson(input).flatMapMany(json -> {
            ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output");

            return Flux.fromIterable(outputArray)
                    .flatMap(obj -> {
                        PersonTrade personTrade = PersonTrade.jsonToPersonTrade(obj);
                        if (personTrade == null) return Mono.empty(); // null인 경우 무시
                        personTrade.setStockCode(input.getStockShortCode());
                        personTrade.setStockName(input.getStockName());
                        return Mono.just(personTrade); // 반드시 non-null
                    });
        });
    }

    //주식 기간별 캔들조회
    @PostMapping("/stock-daily-candle")
    public Flux<Candle> callStockDailyCandle(@RequestBody StockDailyCandleReqDTO input){
        return commonApiClient.callStockDailyCandle(input).flatMapMany(json -> {
            ArrayList<LinkedHashMap> outputArray = (ArrayList) json.get("output2");

            return Flux.fromIterable(outputArray)
                    .map(Candle::dailyCandleJsonToCandle);
        });
    }

}
