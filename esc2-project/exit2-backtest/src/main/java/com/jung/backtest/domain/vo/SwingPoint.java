package com.jung.backtest.domain.vo;

import com.jung.backtest.chart.ChartServiceImpl;
import com.jung.backtest.domain.em.SwingPointType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class SwingPoint {

    private final double price;
    private final int index;
    private final SwingPointType type;

    double price() { return price; }
    int index() { return index; }
    SwingPointType type() { return type; }
}
