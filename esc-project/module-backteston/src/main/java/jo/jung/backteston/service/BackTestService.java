package jo.jung.backteston.service;

import jakarta.annotation.PostConstruct;
import jo.jung.backteston.entity.MinuteCandleEntity;
import jo.jung.backteston.entity.TradeAmountEntity;
import jo.jung.backteston.repo.MinuteCandleRepo;
import jo.jung.backteston.repo.TradeAmountRepo;
import jo.jung.backteston.service.algorithm.BuyAlgorithm;
import jo.jung.backteston.service.algorithm.SellAlgorithm;
import jo.jung.backteston.service.recipe.Recipe;
import jo.jung.backteston.service.recipe.RecipeGenerator;
import jo.jung.backteston.service.signal.CustomParam;
import jo.jung.backteston.util.IndicatorUtil;
import jo.jung.common.priceutil.PriceUtil;
import jo.jung.domain.candle.Candle;
import jo.jung.domain.trade.Trade;
import jo.jung.domain.trade.TradeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple3;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
//백테스팅은 과거 데이터이므로 실시간 신호포착이나 매수,매도 한계있음. 따라서 종가기준으로 신호감지 및 매수매도 가정함
public class BackTestService {
    private final TradeAmountRepo tradeAmountRepo;
    private final MinuteCandleRepo minuteCandleRepo;
    private final RecipeGenerator recipeGenerator;

    private final int TOP_COUNT = 30;

    //잔고
    private Map<String,Trade> tempBox = new ConcurrentHashMap<>();
    //캔들리스트
    List<MinuteCandleEntity> candles = new ArrayList<>();
    //timeStr로 리스트에서 해당 캔들 index를 구하는 맵
    HashMap<String, Integer> candleIdxMap = new HashMap<>();
    HashMap<String, String> blackListMap = new HashMap<>();
    private Map<String, Integer> timeIndexMap = new HashMap<>();
    private List<String> savedDateList = null;

    long money = 100000000L;
    long ticket = 1000000L;
    long totalGain = 0L;
    int gainCnt = 0;
    int painCnt = 0;
    int tradeCnt = 0;
    int remainCnt = 0;
    int remainGainCnt = 0;
    int remainPainCnt = 0;
    final float gainNPercent = 0.01f;
    final float painNPercent = 0.01f;

    // 최초 1회 실행
    @PostConstruct
    public void runOnStartup() {
        runBackTest();
    }

    public void runBackTest(){
        //거래량 상위 TOP_COUNT개 조회(날짜 오름차순 정렬)
        //과거 데이터 처음날짜부터 끝까지
        List<TradeAmountEntity> topStocks = selectTopStocks(TOP_COUNT);
        String yesterday = "";
        String today = "";

        //전체날짜 리스트 인덱싱
        int idx = 0;
        savedDateList = tradeAmountRepo.getSavedDateList();
        for(String dateStr: savedDateList){
            timeIndexMap.put(dateStr,idx++);
        }

        log.info(topStocks.getFirst().getTimeAt()+" to "+topStocks.getLast().getTimeAt());

        List<Recipe> recipeList = recipeGenerator.generateAllRecipes();
        log.info("recipe size : "+recipeList.size());
        int recipeIndex = 0;
        for(Recipe recipe : recipeList) {

            money = 100000000L;
            totalGain = 0L;
            gainCnt = 0;
            painCnt = 0;
            tradeCnt = 0;
            remainCnt = 0;
            remainGainCnt = 0;
            remainPainCnt = 0;

            log.info("recipeIndex : "+recipeIndex++);
            BuyAlgorithm buyAlgorithm = recipe.getBuyAlgo();
            SellAlgorithm sellAlgorithm = recipe.getSellAlgo();

            log.info(buyAlgorithm.getDescription());
            log.info(sellAlgorithm.getDescription());

            for (TradeAmountEntity entity : topStocks) {
                String stockCode = entity.getStockCode();
                String stockName = entity.getStockName();
//                log.info("@@@ " + stockName + " 종목 스캔시작@@@");
                today = getNextMarketDate(entity.getTimeAt());
                if (today.equals("20240912") || today.equals(""))
                    continue;
                //날짜 바뀌었을때
                if (!today.equals(yesterday)) {
                    if(tempBox.size()!=0)
                        log.info("일일정산로그 이상함 수정필요");
                    //일일 정산로그 출력
                    printDailyLog(today);
                    //잔액 초기화
                    money = 100000000L;
                    blackListMap.clear();
                }

                candles.clear();
                //특정종목의 하루 전체 분봉조회
//                log.info("분봉조회 파라미터 : " + stockCode + " " + today);
                candles = selectTodayMinuteCandle(stockCode, today);
                //candles = IndicatorUtil.makeOneMinuteCandleToFiveMinuteCandle(candles);
//                log.info("조회분봉 사이즈 : " + candles.size());
                if (candles.size() <= 0 || candles.getLast().getStartPrice()<1000) // 1000원이하 동전주 제외 추가
                    continue;

                candleIdxMap.clear();
                //캔들 인덱스 저장
                for (int i = 0; i < candles.size(); i++) {
                    candleIdxMap.put(candles.get(i).getTimeAt(), i);
                }

                DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyyMMdd");
                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                // 기준 날짜 지정 (오늘 날짜 기준)

                LocalDate date = LocalDate.parse(today, formatter1);
                LocalDateTime startTime = date.atTime(9, 10, 0);
                LocalDateTime endTime = date.atTime(15, 30, 0);

                // 반복문: 1분 단위로 증가
                while (!startTime.isAfter(endTime)) {
                    // yyyyMMddHHmmss 형식의 문자열 출력
                    String timeStr = startTime.format(formatter2);
                    if (!candleIdxMap.containsKey(timeStr)) {
                        startTime = startTime.plusMinutes(1);
                        continue;
                    }
                    if (!tempBox.containsKey(stockCode)) {
                        long buyPrice = isBuySignal(timeStr, buyAlgorithm);
                        if(buyPrice!=-1){
                            Trade trade = Trade.builder()
                                    .tradeType(TradeType.BUY)
                                    .buyPrice(buyPrice)
                                    .stockCode(stockCode)
                                    .stockName(stockName)
                                    .build();
                            buy(timeStr, trade);
                        }
                    }

                    if (!tempBox.containsKey(stockCode)) {
                        startTime = startTime.plusMinutes(1);
                        continue;
                    }
                    long sellPrice = isSellSignal(timeStr, tempBox.get(stockCode), sellAlgorithm);
                    if (sellPrice != 0) {
                        Trade trade = tempBox.get(stockCode);
                        trade.setTradeType(TradeType.SELL);
                        trade.setSellPrice(sellPrice);
                        sell(timeStr, trade);
                        tradeCnt++;
                    }

                    // 1분 증가
                    startTime = startTime.plusMinutes(1);
                    //5분증가
//                startTime = startTime.plusMinutes(5);
                }

                for(String key : tempBox.keySet()) {
                    Trade trade = tempBox.get(key);
                    trade.setTradeType(TradeType.REMAIN);
                    trade.setSellPrice(0);
                    sellForClear(trade);
                    remainCnt++;
                    tradeCnt++;
                }

                yesterday = today;
//                log.info("@@@ " + stockName + " 종목 스캔 끝@@@");
            }
            //일일 정산로그 출력
            printDailyLog(today);
            //잔고 및 잔액 초기화
            money = 100000000L;
            //총 정산로그 출력
            log.info("total gain : " + totalGain + "(" + (float) totalGain / money * 100f + "%)");
            log.info("total tradeCnt : " + tradeCnt + " remainCnt : " + tempBox.size() + " gainCnt:painCnt " + gainCnt + ":" + painCnt);
            log.info("remainTradeCnt : " + remainCnt + " remainGain:remainPainCnt " + remainGainCnt + ":" + remainPainCnt);
            log.info("win rate : " + (float) gainCnt / tradeCnt * 100 + "%");
        }
    }

    private String getNextMarketDate(String todayStr){
        int nextIdx = timeIndexMap.get(todayStr)+1;
        if(nextIdx>= savedDateList.size())
            return "";
        return savedDateList.get(nextIdx);
    }

    private List<TradeAmountEntity> selectTopStocks(int topCount){
        return tradeAmountRepo.findTopNByTimeAt(topCount);
    }

    private List<MinuteCandleEntity> selectTodayMinuteCandle(String stockCode, String today){
        return minuteCandleRepo.findByStockCodeAndDate(stockCode, today);
    }

    private long isBuySignal(String timeStr, BuyAlgorithm buyAlgorithm){
        int index = candleIdxMap.get(timeStr);
        long buyPrice = buyAlgorithm.evaluate(candles.subList(0, index + 1));
        if(buyPrice!=-1) {
//            log.info("매수 조건만족 at : "+timeStr);
        }
        return buyPrice;
    }

    private long isSellSignal(String timeStr, Trade myStock, SellAlgorithm sellAlgorithm){
        int index = candleIdxMap.get(timeStr);
        CustomParam customParam = new CustomParam(candles.subList(0,index+1), myStock, gainNPercent, painNPercent);
        long sellPrice = sellAlgorithm.evaluate(customParam);
        if(sellPrice != 0){
//            log.info("매도 조건만족 at : "+timeStr);
        }
        return sellPrice;
    }



    private void buy(String timeStr, Trade trade){
        if(money < ticket) {
            log.info("No more Money...");
            return;
        }
        int candleIdx = candleIdxMap.get(timeStr);
        MinuteCandleEntity candle = candles.get(candleIdx);
        long nowPrice = (trade.getBuyPrice()==0 ? candle.getStartPrice() : trade.getBuyPrice());
        long amount = ticket/nowPrice;

//        log.info("매수 실행 at : "+candle.getTimeAt()+" 매수 가격 : "+nowPrice);

        trade.setAmount(amount);
        trade.setMyPrice(nowPrice);
        trade.setTimeAt(timeStr);
        tempBox.put(trade.getStockCode(), trade);
        money-= (amount * nowPrice);
    }



    private void sell(String timeStr, Trade trade){
        int candleIdx = candleIdxMap.get(timeStr);
        MinuteCandleEntity candle = candles.get(candleIdx);

        long nowPrice = candle.getEndPrice();
        long hasAmount =  trade.getAmount();
        long myPrice = trade.getMyPrice();
        long sellPrice = trade.getSellPrice();

        if(sellPrice!= 0)
            nowPrice = sellPrice;

        if(trade.getTradeType().equals(TradeType.REMAIN)){
            if(nowPrice>myPrice)
                remainGainCnt++;
            else
                remainPainCnt++;
        }

        long earnMoney = PriceUtil.getEarnMoney(myPrice, nowPrice, hasAmount);
        if(earnMoney > 0) {
            gainCnt++;
        }
        else {
            painCnt++;
            blackListMap.put(trade.getStockCode(),timeStr);
        }

//        log.info("매도 실행 at : "+candle.getTimeAt()+" 매도 가격 : "+nowPrice+" 손익 : "+earnMoney);

        money+= (myPrice*hasAmount+earnMoney);
        tempBox.remove(trade.getStockCode());

    }


    private void sellForClear(Trade trade){
        MinuteCandleEntity candle = candles.getLast();

        long nowPrice = candle.getEndPrice();
        long hasAmount =  trade.getAmount();
        long myPrice = trade.getMyPrice();
        long sellPrice = trade.getSellPrice();

        if(sellPrice!= 0)
            nowPrice = sellPrice;

        if(trade.getTradeType().equals(TradeType.REMAIN)){
            if(nowPrice>myPrice)
                remainGainCnt++;
            else
                remainPainCnt++;
        }

        long earnMoney = PriceUtil.getEarnMoney(myPrice, nowPrice, hasAmount);
        if(earnMoney > 0) {
            gainCnt++;
        }
        else {
            painCnt++;
        }

//        log.info("매도 실행 at : "+candle.getTimeAt()+" 매도 가격 : "+nowPrice+" 손익 : "+earnMoney);

        money+= (myPrice*hasAmount+earnMoney);
        tempBox.remove(trade.getStockCode());

    }

    private void printDailyLog(String today){
        long gain = money-100000000L;
//        log.info("일일결산 : "+today+" 수익 "+gain);
        totalGain += gain;
    }

    private boolean isBlackList(String stockCode, String nowTimeStr){
        return blackListMap.containsKey(stockCode) && !isPast30Minutes(blackListMap.get(stockCode), nowTimeStr);
    }

    public static boolean isPast30Minutes(String timeStr, String nowTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // 파싱
        LocalDateTime targetTime = LocalDateTime.parse(timeStr, formatter);
        LocalDateTime nowTime = LocalDateTime.parse(nowTimeStr, formatter);

        // 30분 이상 지났는지 체크
        Duration diff = Duration.between(targetTime, nowTime);

        return diff.toMinutes() >= 30;
    }

}
