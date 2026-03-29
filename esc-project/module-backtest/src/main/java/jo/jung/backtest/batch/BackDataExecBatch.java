package jo.jung.backtest.batch;

import jakarta.annotation.PostConstruct;
import jo.jung.backtest.service.BackDataService;
import jo.jung.backtest.service.CsvReader;
import jo.jung.common.apiclient.ApiUtil;
import jo.jung.domain.api.StockDailyCandleReqDTO;
import jo.jung.domain.candle.Candle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackDataExecBatch {

    private final CsvReader csvReader;
    private final BackDataService backDataService;
    private final ApiUtil apiUtil;


    // 최초 1회 실행
    @PostConstruct
    public void runOnStartup() {
        run();
    }

    // 매일 19시에 실행
    // MODE : 0 - 전체실행
    @Scheduled(cron = "0 0 19 * * *")
    public void run() {
        log.info("[백데이터 적재 실행]");

        try {
            //종목파일 읽기 및 테이블에 저장
//            csvReader.readCsvNSaveData();
            //@@종목추천 프로젝트 백데이터 적재
//            backDataService.callNSaveStarStockData();

            //@@EXIT 프로젝트 백데이터 적재
            //모든 종목 거래량 호출및 저장
            backDataService.callNSaveAllStocksInfo();
            //TOP 거래량 종목 분봉 저장
            backDataService.callNSaveTopStocksMinuteCandle();
        }
        catch (Exception e){
            log.info("에러 종료: "+e);
        }

        log.info("[백데이터 적재 종료]");
    }


}
