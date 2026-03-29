package com.jung.feedbot.feedbot.utils;

import lombok.AllArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

public class SeleniumUtil {

    private final String USER_AGENT = "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.20 Safari/537.36";

    private ChromeDriver driver = null;
    private WebDriverWait webDriverWait = null;


    public SeleniumUtil(){
        initCrawlingDriver();
    }


    private void initCrawlingDriver(){
        ChromeOptions options = new ChromeOptions();
        options.addArguments(USER_AGENT);
        options.addArguments("--disable-popup-blocking");       //팝업안띄움(운영 배포시 필요)
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--verbose");  // 로그를 자세히 기록
        driver = new ChromeDriver(options);
        webDriverWait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public void loadUrl(String url){
        driver.get(url);
    };

    public List<WebElement> getElementsByTag(String targetTag){
        waitForTagPresent(targetTag);
        return driver.findElements(By.cssSelector(targetTag));
    }

    public WebElement getElementByTag(String targetTag){
        waitForTagPresent(targetTag);
        return driver.findElement(By.cssSelector(targetTag));
    }

    public WebElement getElementFromElement(WebElement element, String targetTag){
        waitForTagPresent(element);
        return element.findElement(By.cssSelector(targetTag));
    }

    public List<WebElement> getElementsFromElement(WebElement element, String targetTag){
        waitForTagPresent(element);
        return element.findElements(By.cssSelector(targetTag));
    }

    public void waitForTagPresent(String targetTag){
        webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(targetTag)));
    }

    public void waitForTagPresent(WebElement element){
        webDriverWait.until(ExpectedConditions.visibilityOf(element));
    }

    public void pageScrollToBottom(String targetTag){
        WebElement scrollContainer = getElementByTag(targetTag);
        Actions actions = new Actions(driver);

        long preScrollTop = 0l;
        long scrollTop = 0l;

        while(true){
            actions.moveToElement(scrollContainer).perform();
            driver.executeScript("arguments[0].scrollTop = arguments[0].scrollHeight",scrollContainer);
            waitSec(5);
            scrollTop = getScrollTop(targetTag);
            if(preScrollTop == scrollTop)
                break;

            preScrollTop = scrollTop;
            waitSec(5);
        }

    }

    public void pageScrollToTopByJS(){
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.documentElement.scrollTop = 0;");
    }

    public void pageScrollToBottomByJS(){
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0, document.body.scrollHeight);");
    }

    public void executeJavaScript(String script, Object[]... params){
        driver.executeScript(script,params);
    }



    public long getScrollTop(String targetTag){
        return (long)driver.executeScript("return document.querySelector(arguments[0]).scrollTop",targetTag);
    }

    public void waitSec(long sec){
        try {
            Thread.sleep(sec*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
