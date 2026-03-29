package com.jung.backtest.domain.vo;

import com.jung.backtest.domain.em.CandleSign;
import com.jung.backtest.domain.em.CandleTimeType;
import com.jung.backtest.domain.entity.CandleEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@Builder
public class Candle {
    double topPrice;
    double bottomPrice;
    double startPrice;
    double endPrice;
    double tradeAmount;
    double tradeMoney;
    String timeAt;
    CandleTimeType candleTimeType;
    CandleSign candleSign;

    public static Candle toCandle(CandleEntity entity) {
        if (entity == null) return null;

        return Candle.builder()
                .topPrice(entity.getTopPrice())
                .bottomPrice(entity.getBottomPrice())
                .startPrice(entity.getStartPrice())
                .endPrice(entity.getEndPrice())
                .tradeAmount(entity.getTradeAmount())
                .tradeMoney(entity.getTradeMoney())
                .timeAt(entity.getTimeAt())
                .candleTimeType(entity.getCandleTimeType())
                .candleSign(entity.getCandleSign())
                .build();
    }

    public static List<Candle> toCandleList(List<CandleEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(Candle::toCandle)
                .toList(); // Java 16+
    }
}
