package com.jung.kisclient;

import com.jung.domain.order.OrderStockReqDTO;
import com.jung.domain.stock.Stock;
import org.springframework.http.ResponseEntity;

public interface CommonApiClient {
    ResponseEntity<?> callMarketDayInfo();
    ResponseEntity<?> callOAuth();
    ResponseEntity<?> callStockNowPrice(Stock input);
    ResponseEntity<?> callStockDailyPrice(Stock input);
    ResponseEntity<?> callStockBuyPerson(Stock input);
    ResponseEntity<?> callStockNowPriceOver30(Stock input);
    ResponseEntity<?> callMyBox(String acno);
    ResponseEntity<?> callOrderStock(OrderStockReqDTO input);
    ResponseEntity<?> callSellPossibleStockAmount(String acno, String input);
}
