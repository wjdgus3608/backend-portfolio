package com.jung.app.service.api;

import com.jung.app.domain.vo.Candle;
import com.jung.app.domain.vo.Order;
import com.jung.app.domain.vo.Position;

import java.util.List;

public interface ApiClientService {
    List<Candle> getRecentCandlesByTicker(String ticker, String interval, int limit);
    List<Position> getFuturesPositionsAsList();
    double getFuturesAvailableUSDT();
    List<Order> getAllOrders(List<String> symbols);
    Order createOrder(Order order);
    boolean deleteOrder(long orderId);
}
