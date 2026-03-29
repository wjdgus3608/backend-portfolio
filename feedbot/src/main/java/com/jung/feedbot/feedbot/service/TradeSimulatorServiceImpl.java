package com.jung.feedbot.feedbot.service;

import com.jung.feedbot.feedbot.domain.BackTestStock;
import com.jung.feedbot.feedbot.domain.BackTestStockFeed;
import com.jung.feedbot.feedbot.domain.BackTestStockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeSimulatorServiceImpl implements TradeSimulatorService{

    private final BuyLogicService buyLogicService;
    private final SellLogicService sellLogicService;

    private final float TICKET = 1000f;
    private float gainMoney = 0f;
    private int myAmount = 0;
    private float myPrice = 0f;
    private float myRest = 0f;

    @Override
    public float simulateWithLogic(List<BackTestStockFeed> feedInfoList, List<BackTestStockPrice> priceInfoList, BackTestStock stock, String year) {
        boolean hasStock = false;
        String dayInfo = "";
        initTestInfo();

        int buyCnt = 0;
        int sellCnt = 0;

        for (BackTestStockPrice priceInfo : priceInfoList){
            dayInfo = priceInfo.getStockKey().getDate();
            if(!dayInfo.substring(0,4).equals(year)) continue;
            if(!hasStock){
                boolean isBuy = buyLogicService.isBuy(feedInfoList, dayInfo);
                if(isBuy){
                    buyCnt++;
                    hasStock = true;
                    executeBuy(priceInfo);
                }
            }
            if(hasStock){
                float sellPrice = sellLogicService.isSell(feedInfoList, priceInfo, myPrice, stock);
                if(sellPrice != 0f){
                    if(sellPrice<myPrice){
                        log.info("minus day : "+dayInfo);
                    }
                    sellCnt++;
                    hasStock = false;
                    executeSell(sellPrice);
                }
            }
        }

        log.info("buyCnt and sellCnt: "+buyCnt+" "+sellCnt);
        log.info("now stock gain: "+gainMoney+" "+gainMoney/TICKET*100+"%");

        return gainMoney;
    }

    private void initTestInfo(){
        gainMoney = 0;
        myAmount = 0;
        myPrice = 0;
        myRest = 0;
    }

    private void executeBuy(BackTestStockPrice priceInfo){
        float price = priceInfo.getOpen();
        myPrice = price;
        myAmount = (int)Math.floor(TICKET/price);
        myRest = TICKET - myAmount * myPrice;
    }

    private void executeSell(float sellPrice){
        gainMoney += sellPrice * myAmount - TICKET + myRest;
        float resultPercent = (sellPrice-myPrice)/sellPrice*100f;
        log.info(resultPercent+"% gain");
        myAmount = 0;
        myPrice = 0f;
        myRest = 0f;
    }
}
