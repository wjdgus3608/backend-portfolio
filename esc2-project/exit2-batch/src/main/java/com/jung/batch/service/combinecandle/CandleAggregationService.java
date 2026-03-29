package com.jung.batch.service.combinecandle;

import com.jung.batch.domain.em.CandleTimeType;
import com.jung.batch.domain.entity.CandleEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandleAggregationService {

    public CandleEntity aggregate(
            String ticker,
            List<CandleEntity> candles,
            CandleTimeType targetType,
            String bucketTime
    ) {
        CandleEntity first = candles.get(0);
        CandleEntity last  = candles.get(candles.size() - 1);

        double high = candles.stream()
                .mapToDouble(CandleEntity::getTopPrice)
                .max()
                .orElse(0);

        double low = candles.stream()
                .mapToDouble(CandleEntity::getBottomPrice)
                .min()
                .orElse(0);

        double volume = candles.stream()
                .mapToDouble(CandleEntity::getTradeAmount)
                .sum();

        double money = candles.stream()
                .mapToDouble(CandleEntity::getTradeMoney)
                .sum();

        return CandleEntity.builder()
                .ticker(ticker)
                .timeAt(bucketTime)
                .startPrice(first.getStartPrice())
                .endPrice(last.getEndPrice())
                .topPrice(high)
                .bottomPrice(low)
                .tradeAmount(volume)
                .tradeMoney(money)
                .candleTimeType(targetType)
                .candleSign(last.getCandleSign())
                .build();
    }
}

