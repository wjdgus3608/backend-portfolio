package com.jung.feedbot.feedbot.tagparser;

import org.openqa.selenium.WebElement;

public interface TheRichTagParser {
    String parseStockNameAtMain(WebElement element);
    String parseStockFeedPeriodAtMain(WebElement element);
    String parseStockYearFeedRate(WebElement element);
}
