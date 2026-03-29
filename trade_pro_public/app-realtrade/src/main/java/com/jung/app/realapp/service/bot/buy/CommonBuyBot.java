package com.jung.app.realapp.service.bot.buy;

import org.springframework.stereotype.Component;

@Component
public interface CommonBuyBot {
    int buyFilter(String cName);
    void buy();
}
