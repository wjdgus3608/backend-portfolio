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
public class ShortTimeExitSellBot extends NormalBot implements ExitSellBot{

    private static List coinList;
    private final static int SHORT_TIME_TYPE = 3;

    public ShortTimeExitSellBot(MyBox myBox, ApiClient apiClient) {
        super(myBox, apiClient);
    }

    @Override
    public int sellFilter(Object coin, double nowPrice) {
        String cName = ((Coin)coin).getCName();
        if(!GlobalBotControll.getCoinMap().containsKey(cName) ||
                GlobalBotControll.getCoinMap().get(cName)!=SHORT_TIME_TYPE)
            return 0;
        if(GlobalBotControll.isPassedShortTimeCoinMap(cName)){
            GlobalBotControll.removeCoinFromShortMap(cName);
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
            double nowPrice = (double) apiClient.getNowPrice(coin.getCName());

            if(GlobalBotControll.isAvoidCoin(cName)){
                continue;
            }
            //미상장 제외
            if(nowPrice == -1) continue;
            if(sellFilter(coin,nowPrice)>=lostSellLimit){
                LogUtil.printLog("ShortTimeExitSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                LogUtil.saveLog("ShortTimeExitSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                sellCoin(cName, coin.getCAmount(), nowPrice);
                LogUtil.printLog(myBox.getBoxNow());
                LogUtil.saveLog(myBox.getBoxNow());
                GlobalBotControll.removeCoinFromMap(cName);
            }
        }
    }

}
