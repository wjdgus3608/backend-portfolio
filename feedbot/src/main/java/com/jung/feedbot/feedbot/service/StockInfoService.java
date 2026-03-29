package com.jung.feedbot.feedbot.service;

import com.jung.feedbot.feedbot.domain.TradeStockInfo;

import java.util.List;

public interface StockInfoService {
    void collectBackTestData();
    void execBackTest();
    void execFilterTodayStock();
    List<String> retrieveStockList();
    List<TradeStockInfo> retrieveStockDetail(String date);
    void testInsert();
}
