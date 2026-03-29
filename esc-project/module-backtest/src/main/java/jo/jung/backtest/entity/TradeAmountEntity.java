package jo.jung.backtest.entity;

import jakarta.persistence.*;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Entity
@IdClass(TradeAmountId.class)
@Table(name = "trade_amount")
@ToString
public class TradeAmountEntity {
    @Id
    @Column(name = "stock_code")
    private String stockCode;
    @Column(name = "stock_name")
    private String stockName;
    @Column(name = "trade_amount")
    private long tradeAmount;

    @Id
    @Column(name = "time_at")
    private String timeAt;

}
