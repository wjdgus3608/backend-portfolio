package com.jung.backtest.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exit2_candle_batch_checkpoint")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandleBatchCheckpoint {

    @Id
    private String ticker;

    // yyyyMMddHHmmss
    @Column(length = 14, nullable = false)
    private String lastTime;
}

