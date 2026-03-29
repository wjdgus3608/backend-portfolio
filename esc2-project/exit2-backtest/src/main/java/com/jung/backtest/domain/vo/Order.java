package com.jung.backtest.domain.vo;

import com.jung.backtest.domain.em.BuySellType;
import com.jung.backtest.domain.em.LongShortType;
import com.jung.backtest.domain.em.OrderStatus;
import com.jung.backtest.domain.em.TradeType;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder
public class Order {
    UUID orderId;
    Stock stock;
    BuySellType buySellType;
    LongShortType longShortType;

    double leverageRate;
    double orderPrice;
    int tpOrSl;
    double orderAmount;
    TradeType tradeType;
    double gainPrice;
    double painPrice;
    String timeAt;
    OrderStatus orderStatus;
    String orderType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order other = (Order) o;

        return Objects.equals(
                this.stock.getStockName(),
                other.stock.getStockName()
        )
                && this.buySellType == other.buySellType
                && this.longShortType == other.longShortType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                stock.getStockName(),
                buySellType,
                longShortType
        );
    }
}
