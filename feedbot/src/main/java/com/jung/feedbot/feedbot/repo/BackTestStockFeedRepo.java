package com.jung.feedbot.feedbot.repo;


import com.jung.feedbot.feedbot.domain.BackTestStockFeed;
import com.jung.feedbot.feedbot.domain.StockKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackTestStockFeedRepo extends JpaRepository<BackTestStockFeed, StockKey> {
    List<BackTestStockFeed> findByStockKey_StockNameOrderByStockKey_Date(String stockName);
}
