package com.jung.logic.controller;

import com.jung.domain.stock.Stock;
import com.jung.kisclient.CommonApiClient;
import com.jung.logic.service.logic.CommonLogic;
import com.jung.logic.service.logic.FirstLogic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TestController {
    private final CommonApiClient commonApiClient;
    private final FirstLogic firstLogic;
    @GetMapping("/result-stocks")
    public ResponseEntity<?> getResultStocks(){
        log.info("start TestController getResultStocks");
        return ResponseEntity.ok(firstLogic.startWork());
    }


    @GetMapping("/test")
    public ResponseEntity<?> getTest(){
        log.info("start TestController getResultStocks");
        Stock st = new Stock();
        st.setStockShortCode("900110");
        return ResponseEntity.ok(commonApiClient.callStockBuyPerson(st));
    }
}
