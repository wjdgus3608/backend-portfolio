package com.jung.app.domain.vo;

import com.jung.app.domain.vo.em.CandleSign;
import com.jung.app.domain.vo.em.CandleTimeType;
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
