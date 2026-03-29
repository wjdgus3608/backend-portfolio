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
public class XPercentUpSellBot extends NormalBot implements EarnSellBot{
    private static List coinList;

    public XPercentUpSellBot(MyBox myBox, ApiClient apiClient) {
        super(myBox, apiClient);
    }

    @Override
    public int sellFilter(Object coin, double nowPrice) {
        String cName = ((Coin)coin).getCName();
        double myPrice = ((Coin) coin).getBuyAvgPrice();
        if(calculateTick(myPrice,nowPrice)>=(xPercentUpLimit)){
            return 5;
        }
        return 0;
    }


    @Override
    public void sell() {
        coinList=myBox.getCoins();
        for(Object obj : coinList) {

            Coin coin = (Coin) obj;
            String cName = coin.getCName();
            double nowPrice = (double) apiClient.getNowPrice(cName);
            if(sellFilter(coin,nowPrice)>earnSellLimit){
                LogUtil.printLog("XPercentUpSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                LogUtil.saveLog("XPercentUpSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                sellCoin(cName, coin.getCAmount(), nowPrice);
                GlobalBotControll.removeCoinFromMap(cName);
                GlobalBotControll.removeCoinFromShortMap(cName);
            }
        }
    }
}
