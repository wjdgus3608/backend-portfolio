package com.jung.batch.service;

import com.jung.batch.service.collectcandle.CandleBatchService;
import com.jung.batch.service.combinecandle.CandleCombineService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final CandleBatchService candleBatchService;
    private final CandleCombineService candleCombineService;

    // ✅ 앱 시작 시 1번 실행
    @PostConstruct
    public void runAtStartup() {
        //1분봉 수집 및 DB저장
        candleBatchService.executeBatch();

        //5분,15분,1시간봉 생성 및 DB저장
        candleCombineService.executeAll();
    }

    // ✅ 하루에 1번 실행 (자정)
    @Scheduled(cron = "0 0 0 * * *")
    public void runDaily() {
        //1분봉 수집 및 DB저장
        candleBatchService.executeBatch();

        //5분,15분,1시간봉 생성 및 DB저장
        candleCombineService.executeAll();
    }
}

