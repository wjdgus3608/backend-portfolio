package com.jung.batch.repo;

import com.jung.batch.domain.em.CandleTimeType;
import com.jung.batch.domain.entity.CandleAggCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandleAggCheckpointRepository
        extends JpaRepository<CandleAggCheckpoint, Long> {

    Optional<CandleAggCheckpoint>
    findByTickerAndCandleTimeType(String ticker, CandleTimeType type);

}

