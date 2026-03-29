package com.jung.backtest.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jung.backtest.domain.em.CandleSign;
import com.jung.backtest.domain.em.CandleTimeType;
import com.jung.backtest.domain.vo.Candle;

import java.util.ArrayList;
import java.util.List;

public class BinanceCandleParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Binance Kline JSON → List<Candle>
     */
    public static List<Candle> parse(
            String json,
            String interval
    ) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<Candle> candles = new ArrayList<>();

            CandleTimeType timeType =
                    CandleMapperUtil.fromInterval(interval);

            for (JsonNode node : root) {

                long openTimeMillis = node.get(0).asLong();
                double open = node.get(1).asDouble();
                double high = node.get(2).asDouble();
                double low = node.get(3).asDouble();
                double close = node.get(4).asDouble();
                double volume = node.get(5).asDouble();
                double quoteVolume = node.get(7).asDouble();

                Candle candle = Candle.builder()
                        .startPrice(open)
                        .topPrice(high)
                        .bottomPrice(low)
                        .endPrice(close)
                        .tradeAmount(volume)
                        .tradeMoney(quoteVolume)
                        .candleTimeType(timeType)
                        .candleSign(close >= open ? CandleSign.RED : CandleSign.BLUE)
                        .timeAt(TimeUtil.millisToString(openTimeMillis))
                        .build();

                candles.add(candle);
            }

            return candles;

        } catch (Exception e) {
            throw new RuntimeException("캔들 파싱 실패", e);
        }
    }
}
