package com.jung.logic.service.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jung.common.util.TimeUtil;
import com.jung.domain.stock.Stock;
import com.jung.kisclient.CommonApiClient;
import com.jung.logic.service.ReadStockCodesService;
import com.jung.logic.service.filter.StockFilter;
import com.jung.logic.service.log.LogService;
import com.jung.logic.vo.FilteredStock;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirstLogic{

    private final CommonApiClient commonApiClient;
    private final StockFilter stockFilter;
    private final LogService logService;

    // 최초 1회 실행
    @PostConstruct
    public void runOnStartup() {
//        startWork();
    }


    public List<?> startWork(){
        List<Stock> stockCodes = ReadStockCodesService.getStockCodes();

        stockCodes = stockFilter.filterByTotalStockPrice(stockCodes);
        stockCodes = stockFilter.filterByPriceUpAndAmount(stockCodes);
        stockCodes = stockFilter.filterByLongCandle(stockCodes);
        stockCodes = stockFilter.filterByAmountWhenDown(stockCodes);
        stockCodes = stockFilter.filterByBottomPrice(stockCodes);
        stockCodes = stockFilter.filterByBuyPerson(stockCodes);
        stockCodes = stockFilter.filterByStableShapeDays(stockCodes);

        log.info("done \n"+stockCodes);
        saveToLogDB(stockCodes);

        return stockCodes;
    }

    @Scheduled(cron = "0 10 18 * * *")
    public List<?> getFilteredStocks() {
        log.info("work start!");
        if(!isMarketOpened()){
            log.warn("개장일이 아닙니다.");
            return null;
        }
        return startWork();
    }

    private void saveToLogDB(List<Stock> stockCodes){
        Date date = new Date();
        SimpleDateFormat f1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String nowTime = f1.format(date);
        for(Stock stock : stockCodes){
            ObjectMapper objectMapper = new ObjectMapper();
            FilteredStock filteredStock = objectMapper.convertValue(stock,FilteredStock.class);
            filteredStock.setSearchTime(nowTime);

            logService.saveLog(filteredStock);
        }
    }

    private boolean isMarketOpened(){
        String opndYn = "N";
        ResponseEntity<?> response = commonApiClient.callMarketDayInfo();

        String todayStr = TimeUtil.getToday();
        ArrayList<LinkedHashMap> list = ((ArrayList)((JSONObject) response.getBody()).get("output"));
        for(LinkedHashMap map : list){
            if(((String)map.get("bass_dt")).equals(todayStr)){
                opndYn = (String)map.get("opnd_yn");
                break;
            }
        }

        return "Y".equals(opndYn);
    }
}
