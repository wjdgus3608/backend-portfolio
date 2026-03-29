package com.jung.feedbot.feedbot.service;

import com.jung.feedbot.feedbot.domain.BackTestStock;
import com.jung.feedbot.feedbot.domain.BackTestStockFeed;
import com.jung.feedbot.feedbot.domain.BackTestStockPrice;

import java.util.List;

public interface BuyLogicService {
    boolean isBuy(List<BackTestStockFeed> feedInfoList, String dayInfo);
}
