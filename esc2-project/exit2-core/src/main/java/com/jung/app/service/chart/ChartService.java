package com.jung.app.service.chart;

import com.jung.app.domain.vo.BoxRange;
import com.jung.app.domain.vo.Candle;
import com.jung.app.domain.vo.OrderBlock;
import com.jung.app.domain.vo.SwingPoint;

import java.util.List;

public interface ChartService {
    OrderBlock getUpOrderBlock(List<Candle> candles);
    OrderBlock getDownOrderBlock(List<Candle> candles);
    List<SwingPoint> findSwingPoints(List<Candle> candles);
}
