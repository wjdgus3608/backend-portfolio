package com.jung.backtest.runner;

import com.jung.backtest.service.BackTestService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final BackTestService backTestService;

    // ✅ 앱 시작 시 1번 실행
    @PostConstruct
    public void runAtStartup() {
        backTestService.runBackTest();
    }

}

