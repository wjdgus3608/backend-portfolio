package com.jung.batch.service.combinecandle;

import com.jung.batch.common.TimeUtil;
import com.jung.batch.domain.em.CandleTimeType;
import com.jung.batch.domain.entity.CandleAggCheckpoint;
import com.jung.batch.domain.entity.CandleEntity;
import com.jung.batch.repo.CandleAggCheckpointRepository;
import com.jung.batch.repo.CandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleAggregationBatchService {

    private final CandleRepository candleRepository;
    private final CandleAggregationService aggregationService;
    private final CandleAggCheckpointRepository checkpointRepository;
    private final CandlePersistService candlePersistService;

    public void generate(
            String ticker,
            CandleTimeType targetType,
            int intervalMinutes,
            String endTime
    ) {
        String fromTime = checkpointRepository
                .findByTickerAndCandleTimeType(ticker, targetType)
                .map(cp -> TimeUtil.plusMinutes(cp.getLastTime(), intervalMinutes))
                .orElse("20190909193800");

        CandleTimeType baseType = null;
        if(targetType.equals(CandleTimeType.FIVE_MINUTE))
            baseType = CandleTimeType.ONE_MINUTE;
        else if(targetType.equals(CandleTimeType.FIFTEEN_MINUTE))
            baseType = CandleTimeType.FIVE_MINUTE;
        else if(targetType.equals(CandleTimeType.ONE_HOUR))
            baseType = CandleTimeType.FIFTEEN_MINUTE;

        log.info("fromTime : "+fromTime);

        List<CandleEntity> baseCandles =
                candleRepository.findByTickerAndCandleTimeTypeAndTimeAtBetweenOrderByTimeAt(
                        ticker,
                        baseType,
                        fromTime,
                        endTime
                );

        Map<String, List<CandleEntity>> bucketed =
                baseCandles.stream()
                        .collect(Collectors.groupingBy(
                                c -> TimeUtil.toBucketTime(
                                        c.getTimeAt(), intervalMinutes
                                ),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        for (Map.Entry<String, List<CandleEntity>> entry : bucketed.entrySet()) {

            CandleEntity entity =
                    aggregationService.aggregate(
                            ticker,
                            entry.getValue(),
                            targetType,
                            entry.getKey()
                    );


            //체크포인트
            CandleAggCheckpoint checkpoint =
                    checkpointRepository
                            .findByTickerAndCandleTimeType(ticker, targetType)
                            .orElseGet(() ->
                                    CandleAggCheckpoint.builder()
                                            .ticker(ticker)
                                            .candleTimeType(targetType)
                                            .build()
                            );

            checkpoint.setLastTime(entry.getKey());

            candlePersistService.saveDataAndCheckpoint(entity, checkpoint);
//            log.info("checksave : "+entry.getKey());
        }

        log.info(
                "📊 [{}][{}] aggregation done",
                ticker,
                targetType
        );
    }

}

