package com.jung.app.domain.vo;

import com.jung.app.domain.vo.em.LongShortType;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder
public class Position {
    String positionId;
    Stock stock;
    LongShortType longShortType;
    double leverageRate;
    double orderPrice;
    double orderAmount;
    String timeAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position other = (Position) o;

        // stock null 처리 포함
        String thisStockName = stock != null ? stock.getStockName() : null;
        String otherStockName = other.stock != null ? other.stock.getStockName() : null;

        return Objects.equals(thisStockName, otherStockName)
                && this.longShortType == other.longShortType;
    }

    @Override
    public int hashCode() {
        String stockName = stock != null ? stock.getStockName() : null;
        return Objects.hash(stockName, longShortType);
    }
}
