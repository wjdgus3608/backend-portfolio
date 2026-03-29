package com.jung.selladmin.vo;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;

import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AutoTradeStock {
    @Id
    private String stockShortCode;
    private String stockName;
    private boolean autoSellActive = false;
    private float myPrice;
    private String buyTimeAt;
    @Enumerated(EnumType.STRING)
    private SellType sellType;
    private String sellDesc;
    private float futureGainPrice;
    private float futurePainPrice;
    private float futureGainRate;
    private float futurePainRate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoTradeStock stock = (AutoTradeStock) o;
        return stockShortCode != null && stockShortCode.equals(stock.stockShortCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockShortCode);
    }
}
