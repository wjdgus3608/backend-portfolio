package com.jung.feedbot.feedbot.service;

import com.jung.feedbot.feedbot.domain.BackTestStock;
import com.jung.feedbot.feedbot.domain.BackTestStockFeed;
import com.jung.feedbot.feedbot.domain.BackTestStockPrice;
import com.jung.feedbot.feedbot.domain.TradeStockInfo;

import java.util.List;

public interface SellLogicService {
    float isSell(List<BackTestStockFeed> feedInfoList, BackTestStockPrice priceInfo, float myPrice, BackTestStock stock);
    TradeStockInfo findSellPoint(List<BackTestStockFeed> feedInfoList, BackTestStock stock);
}
