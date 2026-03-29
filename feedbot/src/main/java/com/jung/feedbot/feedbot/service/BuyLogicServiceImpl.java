package com.jung.feedbot.feedbot.service;

import com.jung.feedbot.feedbot.domain.BackTestStock;
import com.jung.feedbot.feedbot.domain.BackTestStockFeed;
import com.jung.feedbot.feedbot.domain.BackTestStockPrice;
import com.jung.feedbot.feedbot.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class BuyLogicServiceImpl implements BuyLogicService{
    @Override
    public boolean isBuy(List<BackTestStockFeed> feedInfoList, String dayInfo) {
        for(BackTestStockFeed feedInfo : feedInfoList){
            if(dayInfo.startsWith("01", 4)) break; //1월은 매수하지말기
            if(feedInfoList.indexOf(feedInfo)<1) continue; //배당처음일때는 매수 캔슬
            String feedDay = feedInfo.getStockKey().getDate();
            String targetDay = DateUtil.addTradingDays(feedDay, 1);
            if(dayInfo.equals(targetDay)) {
                return true;
            }
            if(feedDay.compareTo(dayInfo)>0) {
                break;
            }
        }
        return false;
    }
}
