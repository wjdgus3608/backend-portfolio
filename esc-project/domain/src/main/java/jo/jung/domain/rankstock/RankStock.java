package jo.jung.domain.rankstock;

import jo.jung.domain.candle.Candle;
import jo.jung.domain.candle.CandleSign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankStock {
    private String stockShortCode;
    private String stockName;
    private Candle candle;
    private int rank;
    private String timeAt;
}
