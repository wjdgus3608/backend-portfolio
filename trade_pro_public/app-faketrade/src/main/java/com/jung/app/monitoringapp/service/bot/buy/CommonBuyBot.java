package com.jung.app.monitoringapp.service.bot.buy;

import org.springframework.stereotype.Component;

@Component
public interface CommonBuyBot {
    int buyFilter(String cName);
    void buy();
}
