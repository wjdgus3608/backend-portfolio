package com.jung.app.realapp.service.bot.sell;

import org.springframework.stereotype.Component;

@Component
public interface CommonSellBot <T>{
    int sellFilter(T target, double price);
    void sell();
}
