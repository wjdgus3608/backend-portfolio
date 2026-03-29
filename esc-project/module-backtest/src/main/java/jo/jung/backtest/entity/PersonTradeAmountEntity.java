package jo.jung.backtest.entity;

import jakarta.persistence.*;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Entity
@IdClass(TradeAmountId.class)
@Table(name = "person_trade_amount")
@ToString
public class PersonTradeAmountEntity {
    @Id
    @Column(name = "stock_code")
    private String stockCode;
    @Column(name = "stock_name")
    private String stockName;
    @Column(name = "person_buy_amount")
    private long personBuyAmount;
    @Column(name = "foreign_buy_amount")
    private long foreignBuyAmount;
    @Column(name = "agency_buy_amount")
    private long agencyBuyAmount;

    @Id
    @Column(name = "time_at")
    private String timeAt;

}
