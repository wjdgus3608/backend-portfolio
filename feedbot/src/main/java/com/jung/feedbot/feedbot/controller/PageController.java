package com.jung.feedbot.feedbot.controller;

import com.jung.feedbot.feedbot.domain.TradeStockInfo;
import com.jung.feedbot.feedbot.service.StockInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class PageController {

    private final StockInfoService stockInfoService;

    @GetMapping("/stocks")
    public String getDataList(Model model){
        List<String> stockList = stockInfoService.retrieveStockList();
        model.addAttribute("data",stockList);
        return "index";
    }

    @GetMapping("/stock/{date}")
    public String getDataDetail(Model model, @PathVariable String date){
        List<TradeStockInfo> stockInfoList = stockInfoService.retrieveStockDetail(date);
        model.addAttribute("data", stockInfoList);
        return "detail";
    }


}
