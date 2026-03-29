package com.jung.backtest.repo;

import com.jung.backtest.domain.em.CandleTimeType;
import com.jung.backtest.domain.entity.CandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandleRepository extends JpaRepository<CandleEntity,Long> {
    List<CandleEntity> findTop500ByTickerAndCandleTimeTypeAndTimeAtLessThanEqualOrderByTimeAtDesc(
            String ticker,
            CandleTimeType candleTimeType,
            String timeAt
    );

    CandleEntity findTopByTickerAndCandleTimeTypeAndTimeAt(
            String ticker,
            CandleTimeType candleTimeType,
            String timeAt
    );
}
