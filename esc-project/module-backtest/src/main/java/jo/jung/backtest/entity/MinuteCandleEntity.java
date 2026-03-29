package jo.jung.backtest.entity;

import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@IdClass(TradeAmountId.class)
@Entity
@Table(name = "minute_candles")
@ToString
public class MinuteCandleEntity {
    @Id
    @Column(name = "stock_code")
    private String stockCode;
    @Column(name = "stock_name")
    private String stockName;
    private long topPrice;
    private long bottomPrice;
    private long startPrice;
    private long endPrice;
    private long tradeAmount;
    private long tradeMoney;

    @Id
    @Column(name = "time_at")
    private String timeAt;
}
