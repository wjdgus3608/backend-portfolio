package com.jung.batch.domain;

import com.jung.batch.domain.em.CandleSign;
import com.jung.batch.domain.em.CandleTimeType;
import lombok.Builder;
import lombok.Data;

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
}
