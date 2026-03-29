package jo.jung.backtest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceContext;
import jo.jung.backtest.entity.MinuteCandleEntity;
import jo.jung.backtest.entity.PersonTradeAmountEntity;
import jo.jung.backtest.entity.StockEntity;
import jo.jung.backtest.entity.TradeAmountEntity;
import jo.jung.backtest.repo.MinuteCandleRepo;
import jo.jung.backtest.repo.PersonTradeAmountRepo;
import jo.jung.backtest.repo.StockRepo;
import jo.jung.backtest.repo.TradeAmountRepo;
import jo.jung.common.apiclient.ApiUtil;
import jo.jung.common.dateutil.TimeUtil;
import jo.jung.domain.api.StockDailyCandleReqDTO;
import jo.jung.domain.api.StockDailyMinuteCandleReqDTO;
import jo.jung.domain.candle.Candle;
import jo.jung.domain.stock.Stock;
import jo.jung.domain.trade.HoTrade;
import jo.jung.domain.trade.PersonTrade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
@Service
public class BackDataService {
    private final ApiUtil apiUtil;
    private final StockRepo stockRepo;
    private final TradeAmountRepo tradeAmountRepo;
    private final MinuteCandleRepo minuteCandleRepo;
    private final PersonTradeAmountRepo personTradeAmountRepo;

    @PersistenceContext
    private EntityManager entityManager;

    private Map<String, Integer> timeIndexMap = new HashMap<>();
    private List<String> savedDateList = null;


    private final int TOP_COUNT = 30;


    public void callNSaveAllStocksInfo(){
        //resetTable();
        Iterable<StockEntity> stocks = stockRepo.findAll();
        List<StockEntity> list = StreamSupport.stream(stocks.spliterator(), false)
                .collect(Collectors.toList());

        log.info("list size : "+list.size());
        for(StockEntity entity : list){
            fetchAndSaveStockDetail(entity);
        }
    }

    public void callNSaveTopStocksMinuteCandle(){
        List<TradeAmountEntity> topStocks = selectTopStocks(TOP_COUNT);

        //전체날짜 리스트 인덱싱
        int idx = 0;
        savedDateList = getSavedDateList();
        for(String dateStr: savedDateList){
            timeIndexMap.put(dateStr,idx++);
        }


        log.info("topStocks : "+topStocks.size());
        for(TradeAmountEntity tradeAmountEntity : topStocks){
            String nextMarketDate = getNextMarketDate(tradeAmountEntity.getTimeAt());
            if(!nextMarketDate.equals(""))
                fetchAndSaveStockCandles(tradeAmountEntity, nextMarketDate);
        }
    }

    public void callNSaveStarStockData(){
//                resetTable();
        Iterable<StockEntity> stocks = stockRepo.findAll();
        List<StockEntity> list = StreamSupport.stream(stocks.spliterator(), false)
                .collect(Collectors.toList());

        log.info("list size : "+list.size());
        for(StockEntity entity : list){
            fetchAndSaveStockBuyPersonAmount(entity);
        }
    }

    private List<TradeAmountEntity> selectTopStocks(int topCount){
        return tradeAmountRepo.findTopNByTimeAt(topCount);
    }

    private String getNextMarketDate(String todayStr){
        int nextIdx = timeIndexMap.get(todayStr)+1;
        if(nextIdx>= savedDateList.size())
            return "";
        return savedDateList.get(nextIdx);
    }

    @Transactional
    private void fetchAndSaveStockCandles(TradeAmountEntity entity, String targetDate){
        String[] timeArr = {"110000","130000","150000","153000"};


        boolean isExist = false;
        for(String timeKey : timeArr){
            isExist = checkInsertedCandle(entity.getStockCode(), targetDate+timeKey) == 1;
            if(isExist) {
                log.info("{} 존재하므로 생략",entity.getStockName());
                return;
            }
        }


        log.info("{} 분봉 저장 시작 {}", entity.getStockName(),targetDate);

        Stock stock = Stock.builder()
                .stockShortCode(entity.getStockCode())
                .stockName(entity.getStockName())
                .build();

        for(String timeKey : timeArr){
            StockDailyMinuteCandleReqDTO reqDTO = StockDailyMinuteCandleReqDTO.builder()
                    .stock(stock)
                    .FID_INPUT_DATE_1(targetDate)
                    .FID_INPUT_HOUR_1(timeKey)
                    .build();
            // API 호출을 동기적으로 처리
            List<Candle> candles = apiUtil.post("/api/v1/stock-daily-minute-candle", reqDTO, Candle.class)
                    .collectList() // Flux에서 모든 결과를 List로 모은다
                    .block(); // 동기 호출

            for(Candle candle : candles){
                MinuteCandleEntity minuteCandleEntity = MinuteCandleEntity.builder()
                        .stockCode(entity.getStockCode())
                        .stockName(entity.getStockName())
                        .startPrice(candle.getStartPrice())
                        .endPrice(candle.getEndPrice())
                        .bottomPrice(candle.getBottomPrice())
                        .topPrice(candle.getTopPrice())
                        .tradeAmount(candle.getTradeAmount())
                        .tradeMoney(candle.getTradeMoney())
                        .timeAt(candle.getTimeAt())
                        .build();
                minuteCandleRepo.save(minuteCandleEntity);
            }


        }

    }

    @Transactional
    public void fetchAndSaveStockDetail(StockEntity stock) {
        log.info("{} 거래량 저장 시작", stock.getStockName());

        String lastDate = getLatestInsertedDate(stock.getStockCode());

        //오늘 기준 1년전으로 세팅
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        // 오늘 날짜 기준으로 100일 단위로 끊어서 날짜 범위 배열 생성
        List<LocalDate[]> dateRanges = generateDateRanges(oneYearAgo,100);
        List<LocalDate[]> remainDateRanges;
        if(lastDate != null)
            remainDateRanges = getRemainingRangesFrom(lastDate, dateRanges);
        else
            remainDateRanges = dateRanges;

        log.info("remainDate Ranges : "+remainDateRanges.size());

        Stock stock1 = Stock.builder()
                .stockShortCode(stock.getStockCode())
                .build();

        for (LocalDate[] range : remainDateRanges) {
            String startDate = range[0].format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String endDate = range[1].format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            StockDailyCandleReqDTO reqDTO = StockDailyCandleReqDTO.builder()
                    .stock(stock1)
                    .FID_INPUT_DATE_1(startDate)
                    .FID_INPUT_DATE_2(endDate)
                    .build();

            // API 호출을 동기적으로 처리
            List<Candle> candles = apiUtil.post("/api/v1/stock-daily-candle", reqDTO, Candle.class)
                    .collectList() // Flux에서 모든 결과를 List로 모은다
                    .block(); // 동기 호출

            if (candles != null && candles.size() > 0) {
                for (Candle candle : candles) {
                    // 데이터 가공
                    TradeAmountEntity entity = TradeAmountEntity.builder()
                            .stockCode(stock.getStockCode())
                            .stockName(stock.getStockName())
                            .tradeAmount(candle.getTradeAmount())
                            .timeAt(candle.getTimeAt())
                            .build();

                    try {
                        tradeAmountRepo.save(entity);
                    } catch (Exception e) {
                        log.error("Insert 실패: {} ({} ~ {})", stock.getStockCode(), startDate, endDate, e);
                    }
                }
            } else {
                log.warn("No data received for {} (기간: {} ~ {})", stock.getStockCode(), startDate, endDate);
            }
        }

    }

    public void resetTable(){
        tradeAmountRepo.deleteAll();
    }

    @Transactional
    public void fetchAndSaveStockBuyPersonAmount(StockEntity stock) {
        log.info("{} 투자자별 순매수량 저장 시작", stock.getStockName());

        String lastDate = getLatestInsertedDateStarStock(stock.getStockCode());

        Stock input = Stock.builder()
                .stockShortCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .build();

        // API 호출을 동기적으로 처리
        List<PersonTrade> personTradeList = apiUtil.post("/api/v1/stock-buy-person", input, PersonTrade.class)
                .collectList() // Flux에서 모든 결과를 List로 모은다
                .block(); // 동기 호출

        if (personTradeList != null && personTradeList.size() > 0) {
            for (PersonTrade personTrade : personTradeList) {
                if(lastDate==null || TimeUtil.isAfterDate(personTrade.getTimeAt(), lastDate)) {
                    // 데이터 가공
                    PersonTradeAmountEntity entity = PersonTradeAmountEntity.builder()
                            .stockCode(stock.getStockCode())
                            .stockName(stock.getStockName())
                            .personBuyAmount(personTrade.getPersonBuyAmount())
                            .foreignBuyAmount(personTrade.getForeignBuyAmount())
                            .agencyBuyAmount(personTrade.getAgencyBuyAmount())
                            .timeAt(personTrade.getTimeAt())
                            .build();
                    try {
                        personTradeAmountRepo.save(entity);
                    } catch (Exception e) {
                        log.error("Insert 실패: {} ({})", stock.getStockCode(), personTrade.getTimeAt(), e);
                    }
                }
            }
        } else {
            log.warn("No data received for {} Today", stock.getStockCode());
        }


    }


    private List<LocalDate[]> generateDateRanges(LocalDate startDate, int intervalDays) {
        List<LocalDate[]> ranges = new ArrayList<>();

        LocalDate start = startDate;
        LocalDate end = start.plusDays(intervalDays - 1);

        LocalDate today = LocalDate.now();

        while (!start.isAfter(today)) { // 오늘을 넘지 않도록 반복
            LocalDate rangeEnd = end.isAfter(today) ? today : end;
            ranges.add(new LocalDate[]{start, rangeEnd});
            start = rangeEnd.plusDays(1);
            end = start.plusDays(intervalDays - 1);
        }

        return ranges;
    }

    private static List<LocalDate[]> getRemainingRangesFrom(String lastDateStr, List<LocalDate[]> dateRanges) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate lastDate = LocalDate.parse(lastDateStr, formatter);

        for (int i = 0; i < dateRanges.size(); i++) {
            LocalDate start = dateRanges.get(i)[0];
            LocalDate end = dateRanges.get(i)[1];

            if ((lastDate.isEqual(start) || lastDate.isAfter(start)) &&
                    (lastDate.isEqual(end) || lastDate.isBefore(end))) {
                // i번째 범위부터 시작
                return dateRanges.subList(i, dateRanges.size());
            }
        }

        // 일치하는 범위가 없으면 전체 반환 or 빈 리스트 반환
        return List.of();
    }

    private String getLatestInsertedDate(String stockCode) {

        return (String) entityManager.createNativeQuery("""
            SELECT MAX(time_at) AS time_at FROM trade_amount
            WHERE stock_code = :code
        """)
                .setParameter("code", stockCode)
                .getSingleResult();
    }

    private long checkInsertedCandle(String stockCode, String timeAt) {
        return (long) entityManager.createNativeQuery("""
            SELECT EXISTS(
                SELECT 1
                FROM minute_candles
                WHERE stock_code = :code
                AND time_at = :timeAt
            )
        """)
                .setParameter("code", stockCode)
                .setParameter("timeAt", timeAt)
                .getSingleResult();
    }

    private String getLatestInsertedDateStarStock(String stockCode) {

        return (String) entityManager.createNativeQuery("""
            SELECT MAX(time_at) AS time_at FROM person_trade_amount
            WHERE stock_code = :code
        """)
                .setParameter("code", stockCode)
                .getSingleResult();
    }

    private List<String> getSavedDateList(){
        return entityManager.createNativeQuery("""
            SELECT DISTINCT time_at
            FROM trade_amount
            ORDER BY time_at ASC
            """)
                .getResultList();
    }

}
