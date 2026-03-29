package jo.jung.domain.stock;

import jo.jung.domain.candle.CandleSign;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {
    private String stockNormalCode;
    private String stockShortCode;
    private String stockName;
    private long price;
    private long lowPrice;
    private long hasAmount;
    private long issueAmount;
    private BigDecimal totalValue;
    private CandleSign candleSign;
    private float rate;
    private int rank;
    private String timeAt;
}
