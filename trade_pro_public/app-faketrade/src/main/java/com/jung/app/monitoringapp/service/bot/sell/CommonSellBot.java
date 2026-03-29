package com.jung.app.monitoringapp.service.bot.sell;

import org.springframework.stereotype.Component;

@Component
public interface CommonSellBot <T>{
    int sellFilter(T target, double price);
    void sell();
}
