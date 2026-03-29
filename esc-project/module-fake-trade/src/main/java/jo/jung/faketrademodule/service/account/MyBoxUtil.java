package jo.jung.faketrademodule.service.account;

import jo.jung.common.apiclient.ApiUtil;
import jo.jung.common.dateutil.TimeUtil;
import jo.jung.common.logclient.LogUtil;
import jo.jung.common.priceutil.PriceUtil;
import jo.jung.domain.api.StockDailyMinuteCandleReqDTO;
import jo.jung.domain.candle.Candle;
import jo.jung.domain.mybox.MyBox;
import jo.jung.domain.stock.Stock;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MyBoxUtil {

    private final ApiUtil apiUtil;
    private final LogUtil logUtil;

    private final long TICKET = 1000000; //거래단위

    private MyBox myBox = MyBox.builder()
            .money(100_000_000L)
            .stockMap(new ConcurrentHashMap<>())
            .timeSet(new HashSet<>())
            .build();

    public Mono<MyBox> getMyBox(){
        return Mono.fromSupplier(()->myBox);
    }

    public Mono<Long> getMoney() {
        return Mono.fromSupplier(() -> myBox.getMoney());
    }

    public Mono<Map<String, Stock>> getStockMono() {
        return Mono.fromSupplier(() -> myBox.getStockMap());
    }

    public Mono<Boolean> buyStock(Stock stock) {
        return Mono.defer(() -> {
            synchronized (MyBoxUtil.class) {
                String time = TimeUtil.getCurrentTimeHHmmss();
                StockDailyMinuteCandleReqDTO reqDTO = StockDailyMinuteCandleReqDTO.builder()
                        .stock(stock)
                        .FID_INPUT_HOUR_1(time)
                        .build();

                // 캔들 정보 조회
                Flux<Candle> candleFlux = apiUtil.post("/api/v1/stock-today-minute-candle", reqDTO, Candle.class);

                // 첫 번째 캔들 정보를 사용하여 stock을 업데이트
                return candleFlux.next() // 첫 번째 candle만 사용
                        .flatMap(candle -> {
                            stock.setPrice(candle.getEndPrice());
                            stock.setLowPrice(candle.getBottomPrice());
                            stock.setTimeAt(candle.getTimeAt());
                            stock.setHasAmount(TICKET/candle.getEndPrice());

                            long money = myBox.getMoney();
                            long remainMoney = money - stock.getPrice()*stock.getHasAmount();

                            Map<String, Stock> stockMap = myBox.getStockMap();
                            Set<String> timeSet = myBox.getTimeSet();
                            LocalDateTime now = LocalDateTime.now();
                            String timeKey = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                            String key = timeKey+stock.getStockShortCode();
                            if (remainMoney < 0 || stockMap.containsKey(stock.getStockShortCode()) || timeSet.contains(key)) {
                                return Mono.just(false);
                            }

                            stockMap.put(stock.getStockShortCode(), stock);
                            timeSet.add(key);
                            myBox.setMoney(remainMoney);
                            logUtil.saveAndPrintLog("0",stock.getStockName()+" 매수 가격 : "+stock.getPrice()+" 매수개수 : "+stock.getHasAmount());
                            logUtil.saveAndPrintLog("0"," 잔고 : "+myBox.getMoney()+" 보유 종목수 : "+myBox.getStockMap().size());
                            printBoxStatus();

                            return Mono.just(true);
                        })
                        .defaultIfEmpty(false); // candle이 없을 경우 대비
            }
        });
    }


    public Mono<Boolean> sellStock(Stock stock) {
        return Mono.defer(() -> {
            synchronized (MyBoxUtil.class) {
                Map<String, Stock> stockMap = myBox.getStockMap();

                // 종목 보유 여부 확인
                if (!stockMap.containsKey(stock.getStockShortCode())) {
                    return Mono.just(false);
                }

                String time = TimeUtil.getCurrentTimeHHmmss();
                StockDailyMinuteCandleReqDTO reqDTO = StockDailyMinuteCandleReqDTO.builder()
                        .stock(stock)
                        .FID_INPUT_HOUR_1(time)
                        .build();

                // 캔들 정보 조회
                Flux<Candle> candleFlux = apiUtil.post("/api/v1/stock-today-minute-candle", reqDTO, Candle.class);

                // 첫 번째 캔들 정보를 사용하여 stock을 업데이트
                return candleFlux.next() // 첫 번째 candle만 사용
                        .flatMap(candle -> {
                            long nowPrice = candle.getEndPrice();
                            Stock myStock = stockMap.get(stock.getStockShortCode());
                            long preValue = myStock.getPrice() * myStock.getHasAmount();
                            long earnMoney = PriceUtil.getEarnMoney(myStock.getPrice(), nowPrice, myStock.getHasAmount());
                            long updatedMoney = myBox.getMoney() + preValue + earnMoney;

                            logUtil.saveAndPrintLog("0",stock.getStockName()+" 매도 가격 : "+nowPrice+" 수익 : "+earnMoney);
                            myBox.setMoney(updatedMoney);
                            // 종목 제거
                            stockMap.remove(stock.getStockShortCode());

                            long sum = 0;
                            for(String key : stockMap.keySet()){
                                Stock stock1 = stockMap.get(key);
                                long amount = stock1.getHasAmount();
                                long price = stock1.getPrice();
                                sum += amount * price;
                            }
                            logUtil.saveAndPrintLog("0"," 자산평가 : "+(myBox.getMoney()+sum));
                            logUtil.saveAndPrintLog("0"," 잔고 : "+myBox.getMoney()+" 보유 종목수 : "+myBox.getStockMap().size());
                            printBoxStatus();

                            return Mono.just(true);
                        })
                        .defaultIfEmpty(false); // candle이 없을 경우 대비
            }
        });
    }

    public Mono<Void> initTimeSet(){
        myBox.getTimeSet().clear();
        return Mono.empty();
    }

    public void printBoxStatus(){
        Map<String, Stock> map = myBox.getStockMap();
        logUtil.saveAndPrintLog("0", "@@@ 잔고 현황 @@@");
        for(String key : map.keySet()){
            Stock stock = map.get(key);
            logUtil.saveAndPrintLog("0", stock.getStockName()+
                    " - 매수가격, 매수량 : "+stock.getPrice()+","+stock.getHasAmount()+
                    " 매수시점 : "+stock.getTimeAt());
        }

    }
}
