package com.jung.batch.domain.entity;

import com.jung.batch.domain.em.CandleTimeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "candle_agg_checkpoint",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"ticker", "candleTimeType"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandleAggCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker;

    @Enumerated(EnumType.STRING)
    private CandleTimeType candleTimeType;

    // 마지막 처리된 bucket time
    @Column(length = 14, nullable = false)
    private String lastTime;
}

