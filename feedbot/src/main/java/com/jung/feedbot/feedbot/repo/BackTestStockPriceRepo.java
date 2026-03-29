package com.jung.feedbot.feedbot.repo;

import com.jung.feedbot.feedbot.domain.BackTestStockPrice;
import com.jung.feedbot.feedbot.domain.StockKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackTestStockPriceRepo extends JpaRepository<BackTestStockPrice, StockKey> {
    List<BackTestStockPrice> findByStockKey_StockNameOrderByStockKey_Date(String stockName);
}
