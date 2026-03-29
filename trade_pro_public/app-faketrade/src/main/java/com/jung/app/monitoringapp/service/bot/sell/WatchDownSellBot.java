package com.jung.app.monitoringapp.service.bot.sell;

import com.jung.app.monitoringapp.service.bot.GlobalBotControll;
import com.jung.app.monitoringapp.service.bot.NormalBot;
import com.jung.client.api.ApiClient;
import com.jung.common.box.MyBox;
import com.jung.common.log.LogUtil;
import com.jung.domain.coin.Coin;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WatchDownSellBot extends NormalBot implements EarnSellBot{

    private static List coinList;
    private static int CANDLE_CNT=172;

    public WatchDownSellBot(MyBox myBox, ApiClient apiClient) {
        super(myBox, apiClient);
    }

    @Override
    public int sellFilter(Object coin, double nowPrice) {
        String cName = ((Coin)coin).getCName();
        double myPrice = ((Coin) coin).getBuyAvgPrice();
        List<JSONObject> candles = apiClient.searchMinuteCandle(5, cName, null, CANDLE_CNT);


        double[] prices = getPricesFromCandles(candles);
        double[] macds = new double[100];
        double[] ema26s = new double[100];
        double[] ema12s = new double[100];
        for(int i=0; i<100; i++){
            ema26s[i]=getEMA(prices,26,CANDLE_CNT-99+i);
            ema12s[i]=getEMA(prices,12,CANDLE_CNT-99+i);
            macds[i]=ema12s[i]-ema26s[i];
        }
        double preMACD = macds[97];
        double nowMACD = macds[98];
        double nowMACDSig = getEMA(macds,9,macds.length-1);


        if(nowMACD>=nowMACDSig && preMACD>nowMACD && nowPrice>=(myPrice+getPriceLevel(myPrice)*4)){
            return 5;
        }
        if(calculatePercent(myPrice,nowPrice)>=(xPercentUpLimit*0.01)){
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
                LogUtil.printLog("WatchDownSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                LogUtil.saveLog("WatchDownSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                sellCoin(cName, coin.getCAmount(), nowPrice);
                GlobalBotControll.removeCoinFromMap(cName);
                GlobalBotControll.removeCoinFromShortMap(cName);
            }
        }
    }

}
