package com.jung.feedbot.feedbot.service;

import com.jung.feedbot.feedbot.crawler.TheRichCrawler;
import com.jung.feedbot.feedbot.crawler.YahooCrawler;
import com.jung.feedbot.feedbot.domain.*;
import com.jung.feedbot.feedbot.repo.BackTestStockFeedRepo;
import com.jung.feedbot.feedbot.repo.BackTestStockPriceRepo;
import com.jung.feedbot.feedbot.repo.BackTestStockRepo;
import com.jung.feedbot.feedbot.repo.TradeStockInfoRepo;
import com.jung.feedbot.feedbot.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockInfoServiceImpl implements StockInfoService{

    @Value("${custom.filter.monthly-feed:0.03f}")
    private float MONTHLY_FEED_LIMIT;

    private final TheRichCrawler theRichCrawler;
    private final YahooCrawler yahooCrawler;
    private final TradeSimulatorService tradeSimulatorService;
    private final BuyLogicService buyLogicService;
    private final SellLogicService sellLogicService;

    private final BackTestStockRepo backTestStockRepo;
    private final BackTestStockFeedRepo backTestStockFeedRepo;
    private final BackTestStockPriceRepo backTestStockPriceRepo;
    private final TradeStockInfoRepo tradeStockInfoRepo;


    private final String COLLECT_MODE = "01";
    private final String EXEC_MODE = "02";


    @Override
    public void collectBackTestData(){
        //종목 종류 수집
        List<BackTestStock> backTestStocks = collectStockBasicInfo(COLLECT_MODE);
        log.info("backTestStocks size after collectStockBasicInfo: "+backTestStocks.size());

        //수집 종목 필터링
        backTestStocks = filterCollectedStocks(backTestStocks);
        log.info("backTestStocks size after filterCollectedStocks: "+backTestStocks.size());

        //종목 배당락일, 배당금 수집
        List<BackTestStockFeed> backTestStocksFeed = collectStockFeedInfo(backTestStocks);
        log.info("backTestStocksFeed size: "+backTestStocksFeed.size());

        List<BackTestStockPrice> backTestStockPrices = collectStockPriceInfo(backTestStocks);
        log.info("backTestStocks size final: "+backTestStockPrices.size());


        backTestStockRepo.saveAll(backTestStocks);
        backTestStockFeedRepo.saveAll(backTestStocksFeed);
        backTestStockPriceRepo.saveAll(backTestStockPrices);

    }




    private List<BackTestStock> collectStockBasicInfo(String type) {
        return theRichCrawler.crawlMainPage(DateUtil.getDate(), type);
    }

    private List<BackTestStock> filterCollectedStocks(List<BackTestStock> stockList) {
        return stockList.stream()
                .filter(item->item.getFeedPeriod() == FeedPeriod.MONTH
                && item.getYearFeedRate()/12 >=MONTHLY_FEED_LIMIT)
                .collect(Collectors.toList());
    }

    private List<BackTestStockFeed> collectStockFeedInfo(List<BackTestStock> stockList) {
        List<BackTestStockFeed> backTestStocksFeed = new ArrayList<>();
        for(BackTestStock stock:stockList){
            List<BackTestStockFeed> currentFeedInfo = theRichCrawler.crawlDetailPage(stock.getStockName());
            backTestStocksFeed.addAll(currentFeedInfo);
        }

        return backTestStocksFeed;
    }

    private List<BackTestStockPrice> collectStockPriceInfo(List<BackTestStock> stockList) {
        List<BackTestStockPrice> backTestStockPrices = new ArrayList<>();

        for(BackTestStock stock:stockList){
            List<BackTestStockPrice> currentPriceInfo = yahooCrawler.crawlPricePage(stock.getStockName());
            backTestStockPrices.addAll(currentPriceInfo);
        }


        return backTestStockPrices;

    }

    @Override
    public void execBackTest() {
        float totalGain = 0f;

        Iterable<BackTestStock> stocks = backTestStockRepo.findAll();
        for(BackTestStock stock:stocks){
            String stockName = stock.getStockName();
            log.info(stockName+" simulate");

            List<BackTestStockFeed> feedInfoList = backTestStockFeedRepo.findByStockKey_StockNameOrderByStockKey_Date(stockName);
            List<BackTestStockPrice> priceInfoList = backTestStockPriceRepo.findByStockKey_StockNameOrderByStockKey_Date(stockName);

            float result = tradeSimulatorService.simulateWithLogic(feedInfoList, priceInfoList, stock, "2023");
            totalGain += result;
        }

        log.info("total gain $ : "+totalGain);
    }

    @Scheduled(cron = "0 0 17 * * ?") //매일 17시 00분에 실행
//    @Scheduled(cron = "0 0/1 * * * ?") // 매 1분마다 실행
    @Override
    public void execFilterTodayStock() {
        //종목 종류 수집
        List<BackTestStock> collectedStocks = collectStockBasicInfo(EXEC_MODE);
        log.info("collectedStocks size after collectStockBasicInfo: "+collectedStocks.size());

        //수집 종목 필터링
        collectedStocks = filterCollectedStocks(collectedStocks);
        log.info("collectedStocks size after filterCollectedStocks: "+collectedStocks.size());
        log.info("filterCollectedStocks: "+collectedStocks);

        //종목 배당락일, 배당금 수집
        List<BackTestStockFeed> collectedStockFeeds = collectStockFeedInfo(collectedStocks);
        log.info("collectedStocks size: "+collectedStockFeeds.size());

        List<TradeStockInfo> resultStocks = new ArrayList<>();

        for(BackTestStock stock : collectedStocks){
            List<BackTestStockFeed> tempFeedList = collectedStockFeeds
                    .stream()
                    .filter(item->
                            item.getStockKey().getStockName().equals(stock.getStockName()))
                    .sorted(Comparator.comparing(a->a.getStockKey().getDate()))
                    .toList();

            boolean isBuy = buyLogicService.isBuy(tempFeedList, DateUtil.getDate());
            if(isBuy){
                TradeStockInfo sellPoint = sellLogicService.findSellPoint(tempFeedList, stock);
                StockKey stockKey = new StockKey(stock.getStockName(),DateUtil.getDate());
                TradeStockInfo tradeStockInfo = new TradeStockInfo(stockKey, sellPoint.getFeedDate(), sellPoint.getDropDate(), sellPoint.getSellRate());
                resultStocks.add(tradeStockInfo);
            }
        }

        log.info("result : "+resultStocks);

        tradeStockInfoRepo.saveAll(resultStocks);

    }

    @Override
    public List<String> retrieveStockList() {
        return tradeStockInfoRepo.findDateList();
    }

    @Override
    public List<TradeStockInfo> retrieveStockDetail(String date) {
        return tradeStockInfoRepo.findByStockKey_Date(date);
    }

    @Override
    public void testInsert() {
        TradeStockInfo tradeStockInfo = new TradeStockInfo(new StockKey("testStock","20250203")
        ,"20250303","20250301",0.3f);
        tradeStockInfoRepo.save(tradeStockInfo);
    }
}
