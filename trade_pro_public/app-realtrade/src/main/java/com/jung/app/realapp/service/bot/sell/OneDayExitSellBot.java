package com.jung.app.realapp.service.bot.sell;

import com.jung.app.realapp.service.bot.GlobalBotControll;
import com.jung.app.realapp.service.bot.NormalBot;
import com.jung.client.api.ApiClient;
import com.jung.common.box.MyBox;
import com.jung.common.log.LogUtil;
import com.jung.domain.coin.Coin;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OneDayExitSellBot extends NormalBot implements ExitSellBot{

    private static List coinList;

    public OneDayExitSellBot(MyBox myBox, ApiClient apiClient) {
        super(myBox, apiClient);
    }

    @Override
    public int sellFilter(Object coin, double nowPrice) {
        return 5;
    }

    @Override
    public void sell() {
        coinList=myBox.getCoins();
        for(Object obj : coinList) {
            Coin coin = (Coin) obj;
            String cName = coin.getCName();

            if(GlobalBotControll.isAvoidCoin(cName)){
                continue;
            }
            double nowPrice = (double) apiClient.getNowPrice(cName);
            //미상장 제외
            if(nowPrice == -1) continue;
            LogUtil.printLog("OneDayExitSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
            LogUtil.saveLog("OneDayExitSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
            sellCoin(cName, coin.getCAmount(), nowPrice);
        }
    }


}
