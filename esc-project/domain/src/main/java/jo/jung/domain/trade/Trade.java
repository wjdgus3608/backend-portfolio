package jo.jung.domain.trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {
    String stockCode;
    String stockName;
    TradeType tradeType;
    long myPrice;
    long tradePrice;
    long amount;
    long targetGainPrice;
    long targetPainPrice;
    long buyPrice;
    long sellPrice;
    String timeAt;
}
