package com.jung.backtest.chart;


import com.jung.backtest.domain.vo.BoxRange;
import com.jung.backtest.domain.vo.Candle;
import com.jung.backtest.domain.vo.OrderBlock;
import com.jung.backtest.domain.vo.SwingPoint;

import java.util.List;

public interface ChartService {
    BoxRange getBoxRange(List<Candle> candles);
    BoxRange getBoxRange2(List<Candle> candles);
    OrderBlock getUpOrderBlock(List<Candle> candles);
    OrderBlock getDownOrderBlock(List<Candle> candles);
    List<SwingPoint> findSwingPoints(List<Candle> candles);

    List<OrderBlock> getRecentUpOrderBlocks(List<Candle> candles, int maxCount);
    List<OrderBlock> getRecentDownOrderBlocks(List<Candle> candles, int maxCount);
}
