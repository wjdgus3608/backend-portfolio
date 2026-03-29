package com.jung.app.runner;

import com.jung.app.service.trade.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunService {
    private final TradeService tradeService;

    @Scheduled(fixedDelay = 1000) // 이전 실행 끝난 후 1초
    public void runEvery5Seconds() {
        try {
            tradeService.runTrade();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
