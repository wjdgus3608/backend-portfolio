package com.jung.batch.repo;

import com.jung.batch.domain.em.CandleTimeType;
import com.jung.batch.domain.entity.CandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandleRepository extends JpaRepository<CandleEntity,Long> {
    List<CandleEntity> findByTickerAndCandleTimeTypeAndTimeAtBetweenOrderByTimeAt(
            String ticker,
            CandleTimeType candleTimeType,
            String from,
            String to
    );
}
