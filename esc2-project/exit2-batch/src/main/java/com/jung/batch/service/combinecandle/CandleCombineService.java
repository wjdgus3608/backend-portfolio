package com.jung.batch.service.combinecandle;

import com.jung.batch.common.TimeUtil;
import com.jung.batch.domain.em.CandleTimeType;
import com.jung.batch.service.collectcandle.CandleBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleCombineService {

    private final CandleBatchService minuteBatchService;
    private final CandleAggregationBatchService aggregationBatchService;

    private final List<String> tickers = List.of("BTCUSDT", "ETHUSDT", "XRPUSDT", "SOLUSDT");

    public void executeAll() {

        String endTime = TimeUtil.getNowTime();

        for (String ticker : tickers) {

            log.info("🚀 Start ticker [{} FIVE_MINUTE]", ticker);

            // 2️⃣ 5분봉
            aggregationBatchService.generate(
                    ticker,
                    CandleTimeType.FIVE_MINUTE,
                    5,
                    endTime
            );

            log.info("🚀 Start ticker [{} FIFTEEN_MINUTE]", ticker);

            // 3️⃣ 15분봉
            aggregationBatchService.generate(
                    ticker,
                    CandleTimeType.FIFTEEN_MINUTE,
                    15,
                    endTime
            );

            log.info("🚀 Start ticker [{} ONE_HOUR]", ticker);

            // 4️⃣ 1시간봉
            aggregationBatchService.generate(
                    ticker,
                    CandleTimeType.ONE_HOUR,
                    60,
                    endTime
            );

            log.info("✅ End ticker [{}]", ticker);
        }
    }
}
