package com.jung.batch.api;


import com.jung.batch.domain.Candle;

import java.util.List;

public interface ApiClientService {
    List<Candle> getRecentCandlesByTicker(String ticker, String interval, int limit);
    List<Candle> getCandlesByTickerAndPeriod(
            String ticker,
            String interval,
            String startTime,
            String endTime,
            int limit
    );
}
