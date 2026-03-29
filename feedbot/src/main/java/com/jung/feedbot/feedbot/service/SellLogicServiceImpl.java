package com.jung.feedbot.feedbot.service;

import com.jung.feedbot.feedbot.domain.BackTestStock;
import com.jung.feedbot.feedbot.domain.BackTestStockFeed;
import com.jung.feedbot.feedbot.domain.BackTestStockPrice;
import com.jung.feedbot.feedbot.domain.TradeStockInfo;
import com.jung.feedbot.feedbot.utils.DateUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SellLogicServiceImpl implements SellLogicService{
    @Override
    public float isSell(List<BackTestStockFeed> feedInfoList, BackTestStockPrice priceInfo, float myPrice, BackTestStock stock) {
        float monthlyFeedRate = stock.getYearFeedRate()/12f;
        //월배당의 75% 달성시 그가격에 판매
        if((priceInfo.getHigh()-myPrice)/myPrice > monthlyFeedRate/4*3)
            return myPrice+myPrice*(monthlyFeedRate/4*3);

        //같은 -퍼센트일때 손절
//        if((priceInfo.getLow()-myPrice)/myPrice < -monthlyFeedRate/4*3)
//            return myPrice-myPrice*(monthlyFeedRate/4*3);

        //배당락일 다가오면 5일전날에 판매
        for(BackTestStockFeed feedInfo : feedInfoList){
            String feedDay = feedInfo.getStockKey().getDate();
            String targetDay = DateUtil.addTradingDays(feedDay,-5); //5일전 판매가 가장 결과좋음 23~24년
            if(priceInfo.getStockKey().getDate().equals(targetDay))
                return priceInfo.getOpen();
        }
        return 0f;
    }

    @Override
    public TradeStockInfo findSellPoint(List<BackTestStockFeed> feedInfoList, BackTestStock stock) {
        float sellRate = stock.getYearFeedRate()/12f/4*3;
        String dropDate = "";
        String resultFeedDay = "";
        //배당락일 다가오면 5일전날에 판매
        for(BackTestStockFeed feedInfo : feedInfoList){
            String feedDay = feedInfo.getStockKey().getDate();
            String targetDay = DateUtil.addTradingDays(feedDay,-5); //5일전 판매가 가장 결과좋음 23~24년
            if(DateUtil.getDate().compareTo(feedDay)<0){
                resultFeedDay = feedDay;
                dropDate = targetDay;
                break;
            }
        }

        return new TradeStockInfo(null,resultFeedDay,dropDate,sellRate);
    }
}
