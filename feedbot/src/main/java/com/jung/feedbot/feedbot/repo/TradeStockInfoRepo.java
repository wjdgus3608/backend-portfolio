package com.jung.feedbot.feedbot.repo;

import com.jung.feedbot.feedbot.domain.StockKey;
import com.jung.feedbot.feedbot.domain.TradeStockInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TradeStockInfoRepo extends JpaRepository<TradeStockInfo, StockKey> {
    List<TradeStockInfo> findByStockKey_Date(String date);
    @Query("SELECT t.stockKey.date FROM TradeStockInfo t GROUP BY t.stockKey.date ORDER BY t.stockKey.date")
    List<String> findDateList();
}
