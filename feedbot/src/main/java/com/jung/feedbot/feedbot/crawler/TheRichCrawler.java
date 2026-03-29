package com.jung.feedbot.feedbot.crawler;


import com.jung.feedbot.feedbot.domain.BackTestStock;
import com.jung.feedbot.feedbot.domain.BackTestStockFeed;
import com.jung.feedbot.feedbot.domain.FeedPeriod;
import com.jung.feedbot.feedbot.domain.StockKey;
import com.jung.feedbot.feedbot.utils.SeleniumUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class TheRichCrawler{

    private int totalCnt = 0;
    private int dupCnt = 0;

    private final SeleniumUtil seleniumUtil = new SeleniumUtil();

    private final String MAIN_PAGE_URL = "https://www.therich.io/dividend-calendar";
    private final String DETAIL_PAGE_URL = "https://www.therich.io/stock?ticker=";

    private final String MAIN_STOCK_LIST_TAG = ".stocks .sc-880bfd65-0.fIIolK";
    private final String MAIN_STOCK_NAME_TAG = ".ticker";
    private final String MAIN_STOCK_FEED_PERIOD_TAG = ".months";
    private final String MAIN_STOCK_YEAR_FEED_RATE_TAG = ".yield";
    private final String MAIN_DATE_TAG = ".Calendar__day.-ltr.point";
    private final String MAIN_PRE_MONTH_TAG = ".Calendar__monthArrowWrapper.-right";
    private final String MAIN_NEXT_MONTH_TAG = ".Calendar__monthArrowWrapper.-left";
    private final String MORE_VIEW_BTN_TAG = "button.more-view-button";
    private final String FEED_ROWS_TAG = ".dividend-history > table > tbody > tr";

    public List<BackTestStock> crawlMainPage(String baseDate, String crawlType){

        Map<String, BackTestStock> stockMap = new HashMap<>();


        seleniumUtil.loadUrl(MAIN_PAGE_URL);
        seleniumUtil.waitSec(10);

        //백테스팅 데이터 만들기(전월~기준년월 수집)
        if(crawlType.equals("01")) {
            crawlNowMainPage(stockMap);
            movePreMonth();
            crawlNowMainPage(stockMap);
        }
        //알고리즘 결과 만들기(금일기준)
        else if(crawlType.equals("02")){
            crawlNowMainPage(stockMap);
            moveNextMonth();
            crawlNowMainPage(stockMap);
        }

        log.info("total : "+totalCnt);

        List<BackTestStock> stockList = new LinkedList<>(stockMap.values());
        return stockList;
    }

    private void crawlNowMainPage(Map<String, BackTestStock> stockMap){

        for(int i=0; i<5; i++){
            seleniumUtil.pageScrollToBottomByJS();
            seleniumUtil.waitSec(5);
        }

        List<WebElement> elements = seleniumUtil.getElementsByTag(MAIN_STOCK_LIST_TAG);

        for(WebElement element : elements){
            String name = parseStockNameAtMain(element);
            log.info("name : "+name);
            String feedPeriod = parseStockFeedPeriodAtMain(element);
            float yearFeedRate = parseStockYearFeedRate(element);

            if(stockMap.containsKey(name))
                dupCnt++;

            stockMap.put(name ,new BackTestStock(name,transFeedPeriodType(feedPeriod),yearFeedRate));
            totalCnt++;
        }

        seleniumUtil.pageScrollToTopByJS();
        seleniumUtil.waitSec(2);
    }

    private String parseStockNameAtMain(WebElement element) {
        return seleniumUtil.getElementFromElement(element, MAIN_STOCK_NAME_TAG).getText();
    }

    private String parseStockFeedPeriodAtMain(WebElement element) {
        String text = seleniumUtil.getElementsFromElement(element, MAIN_STOCK_FEED_PERIOD_TAG).getLast().getText();
        return text.split(": ")[1];
    }

    private float parseStockYearFeedRate(WebElement element) {
        String text = seleniumUtil.getElementFromElement(element, MAIN_STOCK_YEAR_FEED_RATE_TAG).getText();
        return Float.parseFloat(text.replaceAll("[^0-9.]",""))*0.01f;
    }

    private void movePreMonth(){
        seleniumUtil.getElementByTag(MAIN_PRE_MONTH_TAG).click();
        seleniumUtil.waitSec(5);
    }

    private void moveNextMonth(){
        seleniumUtil.getElementByTag(MAIN_NEXT_MONTH_TAG).click();
        seleniumUtil.waitSec(5);
    }

    private FeedPeriod transFeedPeriodType(String feedPeriodStr){
        if("월배당".equals(feedPeriodStr))
            return FeedPeriod.MONTH;
        else if("연배당".equals(feedPeriodStr))
            return FeedPeriod.YEAR;
        else if("주배당".equals(feedPeriodStr))
            return FeedPeriod.WEEK;
        else
            return FeedPeriod.DAY;
    }

    public List<BackTestStockFeed> crawlDetailPage(String ticker){
        List<BackTestStockFeed> feedList = new ArrayList<>();
        seleniumUtil.loadUrl(DETAIL_PAGE_URL+ticker);
        seleniumUtil.waitSec(15);

        try{
            seleniumUtil.getElementByTag(MORE_VIEW_BTN_TAG).click();
        }
        catch (Exception e){
            log.info(ticker+" 더보기 버튼 없음");
        }

        List<WebElement> rows = seleniumUtil.getElementsByTag(FEED_ROWS_TAG);

        for(WebElement row : rows){

            List<WebElement> elements = seleniumUtil.getElementsFromElement(row, "td");
            String feedDate = elements.get(0).getText().replaceAll("\\.","");
            float feedAmount = Float.parseFloat(elements.get(2).getText().replaceAll("\\$",""));

            StockKey key = new StockKey(ticker,feedDate);
            BackTestStockFeed feedInfo = new BackTestStockFeed(key,feedAmount);
            feedList.add(feedInfo);
        }

        return feedList;
    }



}
