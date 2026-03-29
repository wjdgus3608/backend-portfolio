package com.jung.selladmin.controller;


import com.jung.domain.order.OrderStockReqDTO;
import com.jung.domain.order.OrderType;
import com.jung.domain.stock.Stock;
import com.jung.kisclient.CommonApiClient;
import com.jung.selladmin.dto.RetrieveStockRtnDTO;
import com.jung.selladmin.dto.SaveStockStatusReqDTO;
import com.jung.selladmin.service.AutoTradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AutoTradeController {

    private final AutoTradeService autoTradeService;
    private final CommonApiClient commonApiClient;

    @RequestMapping(value = "/auto", method = RequestMethod.GET)
    public String viewAutoTradeMain(Model model){
//        ResponseEntity<?> res1 = commonApiClient.callSellPossibleStockAmount("43119304", "038110");
//        log.info(res1.toString());
//
//        OrderStockReqDTO reqDTO = OrderStockReqDTO.builder()
//                .acno("43119304")
//                .stockShortCode("038110")
//                .orderType(OrderType.SELL)
//                .amount("1")
//                .build();
//        ResponseEntity<?> res2 = commonApiClient.callOrderStock(reqDTO);
//        log.info(res2.toString());
        List<RetrieveStockRtnDTO> response = autoTradeService.retrieveAccount();
//        log.info("res : "+response);
        model.addAttribute("resultList",response);
        return "auto";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<?> saveAutoSellStatus(@RequestBody List<SaveStockStatusReqDTO> input) {
       autoTradeService.saveAccountStatus(input);
        return ResponseEntity.ok().build();
    }
}
