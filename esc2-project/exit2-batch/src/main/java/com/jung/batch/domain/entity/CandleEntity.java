package com.jung.batch.domain.entity;

import com.jung.batch.domain.em.CandleSign;
import com.jung.batch.domain.em.CandleTimeType;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(
        name = "exit2_candle",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"ticker","candleTimeType", "timeAt"})
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker;

    private double topPrice;
    private double bottomPrice;
    private double startPrice;
    private double endPrice;
    private double tradeAmount;
    private double tradeMoney;

    // yyyyMMddHHmmss
    @Column(length = 14, nullable = false)
    private String timeAt;

    @Enumerated(EnumType.STRING)
    private CandleTimeType candleTimeType;

    @Enumerated(EnumType.STRING)
    private CandleSign candleSign;
}

