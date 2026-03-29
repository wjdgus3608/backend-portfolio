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
public class XPercentDownSellBot extends NormalBot implements LostSellBot{

    private static List coinList;

    public XPercentDownSellBot(MyBox myBox, ApiClient apiClient) {
        super(myBox, apiClient);
    }

    @Override
    public int sellFilter(Object coin, double nowPrice) {
        String cName = ((Coin)coin).getCName();
        double myPrice = ((Coin) coin).getBuyAvgPrice();
        double percent = calculatePercent(myPrice,nowPrice);
        if(percent < 0 && (Math.abs(percent*100)>=(xPercentDownLimit))){
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
            if(sellFilter(coin,nowPrice)>lostSellLimit){
                LogUtil.printLog("XPercentDownSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                LogUtil.saveLog("XPercentDownSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                sellCoin(cName, coin.getCAmount(),nowPrice);
                GlobalBotControll.removeCoinFromMap(cName);
                GlobalBotControll.removeCoinFromShortMap(cName);
            }
        }
    }

}
