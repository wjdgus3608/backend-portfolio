package com.jung.feedbot.feedbot.controller;

import com.jung.feedbot.feedbot.service.StockInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final StockInfoService stockInfoService;

    @GetMapping("/backdata")
    public void collectBackData(){
        stockInfoService.collectBackTestData();
    }

    @GetMapping("/testdata")
    public void stockTest(){
        stockInfoService.execBackTest();
    }

    @GetMapping("/filterdata")
    public void filterData(){
        stockInfoService.execFilterTodayStock();
    }

}
