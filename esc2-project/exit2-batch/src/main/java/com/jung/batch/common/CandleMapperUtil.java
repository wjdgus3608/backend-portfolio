package com.jung.batch.common;

import com.jung.batch.domain.em.CandleTimeType;

public class CandleMapperUtil {
    public static CandleTimeType fromInterval(String interval) {
        switch (interval) {
            case "1m":
                return CandleTimeType.ONE_MINUTE;
            case "5m":
                return CandleTimeType.FIVE_MINUTE;
            case "15m":
                return CandleTimeType.FIFTEEN_MINUTE;
            case "1h":
                return CandleTimeType.ONE_HOUR;
            case "4h":
                return CandleTimeType.FOUR_HOUR;
            default:
                throw new IllegalArgumentException("지원하지 않는 interval: " + interval);
        }
    }
}
