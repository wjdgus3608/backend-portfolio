package com.jung.batch.service.collectcandle;

import com.jung.batch.api.ApiClientService;
import com.jung.batch.common.TimeUtil;
import com.jung.batch.domain.Candle;
import com.jung.batch.domain.entity.CandleBatchCheckpoint;
import com.jung.batch.domain.entity.CandleEntity;
import com.jung.batch.repo.CandleBatchCheckpointRepository;
import com.jung.batch.repo.CandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleBatchService {

    private final ApiClientService apiClientService;
    private final CandleRepository candleRepository;
    private final CandleCheckpointService candleCheckpointService;
    private final CandleBatchCheckpointRepository checkpointRepository;

    private final List<String> tickers = List.of(
            "BTCUSDT",
            "ETHUSDT",
            "XRPUSDT",
            "SOLUSDT"
    );

    private final String startTime = "20190909193800";
    private final String endTime   = TimeUtil.getNowTime();
//    private final String endTime   = "20230313000000";

    public void executeBatch() {

        log.info("📌 CandleBatch START - endTime={}", endTime);

        for (String ticker : tickers) {

            log.info("▶️ Ticker batch start [{}]", ticker);

            String fromTime = checkpointRepository
                    .findById(ticker)
                    .map(CandleBatchCheckpoint::getLastTime)
                    .orElse(startTime);

            log.info("⏱️ [{}] start fromTime={}", ticker, fromTime);

            while (TimeUtil.beforeOrEqual(fromTime, endTime)) {

                log.debug("🔄 [{}] request candles fromTime={}", ticker, fromTime);

                List<Candle> candles =
                        apiClientService.getCandlesByTickerAndPeriod(
                                ticker,
                                "1m",
                                fromTime,
                                endTime,
                                1000
                        );

                if (candles.isEmpty()) {
                    log.warn("⚠️ [{}] no candle data returned, break loop", ticker);
                    break;
                }

                List<CandleEntity> entities = new ArrayList<>();

                for (Candle c : candles) {

                    String candleTime = c.getTimeAt();

                    if (candleTime.compareTo(fromTime) < 0) continue;
                    if (candleTime.compareTo(endTime) > 0) break;

                    entities.add(
                            CandleEntity.builder()
                                    .ticker(ticker)
                                    .topPrice(c.getTopPrice())
                                    .bottomPrice(c.getBottomPrice())
                                    .startPrice(c.getStartPrice())
                                    .endPrice(c.getEndPrice())
                                    .tradeAmount(c.getTradeAmount())
                                    .tradeMoney(c.getTradeMoney())
                                    .timeAt(candleTime)
                                    .candleTimeType(c.getCandleTimeType())
                                    .candleSign(c.getCandleSign())
                                    .build()
                    );

                    fromTime = TimeUtil.plusOneMinute(candleTime);
                }

                if (!entities.isEmpty()) {
                    candleRepository.saveAll(entities);

                    log.info(
                            "💾 [{}] saved {} candles (lastTime={})",
                            ticker,
                            entities.size(),
                            fromTime
                    );

                    candleCheckpointService.saveCheckpoint(ticker, fromTime);

                    log.debug(
                            "✅ [{}] checkpoint updated -> {}",
                            ticker,
                            fromTime
                    );
                } else {
                    log.warn("⚠️ [{}] no valid candles to save", ticker);
                }
            }

            log.info("⏹️ Ticker batch end [{}]", ticker);
        }

        log.info("✅ CandleBatch END");
    }
}



