package com.jung.app.realapp.service.bot.init;

import com.jung.app.realapp.service.bot.GlobalBotControll;
import com.jung.client.api.ApiClient;
import com.jung.common.box.MyBox;
import com.jung.common.log.LogUtil;
import com.jung.domain.coin.Coin;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InitBot {
    @Value("${ticket}")
    protected long ticket;

    private final MyBox myBox;

    public void initCoinMap(){

        GlobalBotControll.resetCoinMap();

        List<Coin> coinList = myBox.getCoins();
        for(Coin coin:coinList){
            String key = coin.getCName();
            int value = (int)(Math.round(coin.getCAmount()*coin.getBuyAvgPrice()/ticket));
            int[] arr = {1,2,4,8};
            int ret = -1;
            for(int num:arr){
                if(value/num>0){
                    ret = num;
                }
            }
            if(ret>0)
                GlobalBotControll.addCoinToMap(key,ret);

        }
    }

    public void initAvoidMap(){
        for(String cName:GlobalBotControll.avoidCoinArray)
            GlobalBotControll.addCoinToAvoidSet(cName);
    }
}
