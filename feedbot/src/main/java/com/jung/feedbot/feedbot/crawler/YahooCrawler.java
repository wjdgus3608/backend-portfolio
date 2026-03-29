package com.jung.feedbot.feedbot.crawler;

import com.jung.feedbot.feedbot.domain.BackTestStock;
import com.jung.feedbot.feedbot.domain.BackTestStockPrice;
import com.jung.feedbot.feedbot.domain.StockKey;
import com.jung.feedbot.feedbot.utils.SeleniumUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class YahooCrawler {

    private final String PRICE_PAGE_URL = "https://finance.yahoo.com/quote/";
    private final String PERIOD_TAG = ".tertiary-btn.fin-size-small.menuBtn.rounded.yf-15mk0m";
    private final String FIVE_YEAR_TAG = "div.quickpicks.yf-1th5n0r > button[value='5_Y']";
    private final String DATA_ROWS_TAG = "table.noDl > tbody > tr";
    private final SeleniumUtil seleniumUtil = new SeleniumUtil();

    public List<BackTestStockPrice> crawlPricePage(String ticker){
        List<BackTestStockPrice> priceList = new ArrayList<>();
        seleniumUtil.loadUrl(PRICE_PAGE_URL+ticker+"/history/");
        seleniumUtil.waitSec(5);

        seleniumUtil.getElementByTag(PERIOD_TAG).click();
        seleniumUtil.waitSec(5);
        seleniumUtil.getElementByTag(FIVE_YEAR_TAG).click();
        seleniumUtil.waitSec(5);

        List<WebElement> rows = seleniumUtil.getElementsByTag(DATA_ROWS_TAG);
        log.info("rows size : "+rows.size());

        for(WebElement row : rows){
            List<WebElement> tds = seleniumUtil.getElementsFromElement(row, "td");
            if(tds.size()<3) continue;
            String date = convertDateFormat(tds.get(0).getText());
            float open = Float.parseFloat(tds.get(1).getText());
            float high = Float.parseFloat(tds.get(2).getText());
            float low = Float.parseFloat(tds.get(3).getText());

            BackTestStockPrice backTestStockPrice = new BackTestStockPrice(new StockKey(ticker, date),open,high,low);
            log.info(ticker+" "+backTestStockPrice);
            priceList.add(backTestStockPrice);
        }

        return priceList;
    }

    private String convertDateFormat(String inputDate) {
        // 입력 날짜 형식 (예: "Jan 10, 2025")
        SimpleDateFormat inputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
        // 출력 날짜 형식 (예: "20250110")
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMdd");

        try {
            // 입력 날짜를 Date 객체로 변환
            Date date = inputFormat.parse(inputDate);
            // 원하는 형식으로 출력
            return outputFormat.format(date);
        } catch (ParseException e) {
            System.out.println("날짜 파싱 오류: " + e.getMessage());
            return null;
        }
    }

}
