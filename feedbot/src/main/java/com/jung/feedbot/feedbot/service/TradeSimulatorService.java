package com.jung.feedbot.feedbot.service;

import com.jung.feedbot.feedbot.domain.BackTestStock;
import com.jung.feedbot.feedbot.domain.BackTestStockFeed;
import com.jung.feedbot.feedbot.domain.BackTestStockPrice;

import java.util.List;

public interface TradeSimulatorService {
    float simulateWithLogic(List<BackTestStockFeed> feedInfoList, List<BackTestStockPrice> priceInfoList, BackTestStock stock, String year);
}
