package jo.jung.backtest.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeAmountId implements Serializable {
    private String stockCode;
    private String timeAt;
}
