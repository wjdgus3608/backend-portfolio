package com.jung.app.monitoringapp.service.bot.sell;

import com.jung.app.monitoringapp.service.bot.GlobalBotControll;
import com.jung.app.monitoringapp.service.bot.NormalBot;
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
            double nowPrice = (double) apiClient.getNowPrice(coin.getCName());

            LogUtil.printLog("OneDayExitSellBot sell "+coin.getCName()+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
            LogUtil.saveLog("OneDayExitSellBot sell "+coin.getCName()+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
            sellCoin(coin.getCName(), coin.getCAmount(), nowPrice);
            GlobalBotControll.removeCoinFromMap(coin.getCName());
        }
    }


}
