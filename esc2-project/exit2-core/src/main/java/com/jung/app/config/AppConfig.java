package com.jung.app.config;

import com.jung.app.service.account.AccountService;
import com.jung.app.service.account.FakeAccountService;
import com.jung.app.service.api.ApiClientService;
import com.jung.app.service.api.BinanceApiService;
import com.jung.app.service.chart.ChartService;
import com.jung.app.service.chart.ChartServiceImpl;
import com.jung.app.service.order.FakeOrderService;
import com.jung.app.service.order.OrderService;
import com.jung.app.service.trade.FakeTradeService;
import com.jung.app.service.trade.TradeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AppConfig {

    @Bean
    public ApiClientService apiClientService(){
        return new BinanceApiService();
    }

    @Bean
    public ChartService chartService(){
        return new ChartServiceImpl();
    }

    @Bean
    public OrderService orderService(){
        return new FakeOrderService();
    }

    @Bean
    public AccountService accountService(){return new FakeAccountService();}

    @Bean
    @Primary
    public TradeService tradeService(){
        return new FakeTradeService(apiClientService(), chartService(), orderService(), accountService());
    }


}
