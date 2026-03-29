package com.jung.domain.coin;

import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Coin {
    private String cName;
    private Long earnAmount;
    private Double earnRate;
    private Double cAmount;
    private Double buyAvgPrice;
    private Long valuePrice;
    private Long buyAmount;
    private LocalDateTime buyTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coin coin = (Coin) o;
        return cName.equals(coin.cName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cName);
    }
}
