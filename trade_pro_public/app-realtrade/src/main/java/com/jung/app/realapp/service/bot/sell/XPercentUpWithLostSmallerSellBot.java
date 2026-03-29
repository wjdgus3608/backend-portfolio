package com.jung.app.realapp.service.bot.sell;

import com.jung.app.realapp.service.bot.GlobalBotControll;
import com.jung.client.api.ApiClient;
import com.jung.common.box.MyBox;
import com.jung.common.log.LogUtil;
import com.jung.domain.coin.Coin;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class XPercentUpWithLostSmallerSellBot implements EarnSellBot{
    private final MyBox myBox;
    private static List coinList;
    private final ApiClient apiClient;

    @Value("${earnSellLimit}")
    private int sellLimit;
    @Value("${xPercentUpLimit}")
    private double xPercent;

    @Override
    public int sellFilter(Object coin, double nowPrice) {
        String cName = ((Coin)coin).getCName();
        double myPrice = ((Coin) coin).getBuyAvgPrice();
        double percent = calculatePercent(myPrice,nowPrice);
        if(percent<0) return 0;
        List<JSONObject> candles = apiClient.searchMinuteCandle(5, cName, null, 20);
        double upDeviation = getUpDeviation(candles);

        if((upDeviation<=nowPrice && (myPrice+getPriceLevel(myPrice)*3)<=nowPrice)
                || calculatePercent(myPrice,nowPrice)>=(xPercent*0.01)){
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
            if(nowPrice == -1) continue;
            if(sellFilter(coin,nowPrice)>sellLimit){
                LogUtil.printLog("XPercentUpSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                LogUtil.saveLog("XPercentUpSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                sellCoin(cName, coin.getCAmount(), nowPrice);
                GlobalBotControll.removeCoinFromMap(cName);
                String str = myBox.getBoxNow();
                LogUtil.printLog(str);
                LogUtil.saveLog(str);
            }
        }
    }



    private Double calculatePercent(double myPrice, double nowPrice){
        return (nowPrice/myPrice)-1;
    }
    private Double calculateTick(double myPrice, double nowPrice){
        double tick = getPriceLevel(nowPrice);
        return (nowPrice-myPrice)/tick;
    }

    private JSONObject makeBillToJson(String cName, double cAmount, double nowPrice){
        JSONObject json = new JSONObject();
        json.put("market",cName);
        json.put("volume",Double.toString(cAmount));
        json.put("plus",true);
        json.put("nowPrice",nowPrice);
        return json;
    }

    private void sellCoin(String cName, double cAmount, double nowPrice){
        JSONObject json = makeBillToJson(cName, cAmount, nowPrice);
        apiClient.sell(json);
    }
    protected double getPriceLevel(double price){
        if(price>=1000000){
            return 1000;
        }
        else if(price>=500000){
            return 100;
        }
        else if(price>=100000){
            return 50;
        }
        else if(price>=10000){
            return 10;
        }
        else if(price>=1000){
            return 5;
        }
        else if(price>=100){
            return 1;
        }
        else if(price>=10){
            return 0.1;
        }
        else if(price>=1){
            return 0.01;
        }
        return 0.001;
    }

    protected double getMovePoint20(List<JSONObject> candles){
        double sum = 0;
        for(int i=0; i<20; i++){
            double value = (double) candles.get(i).get("trade_price");
            sum+= value;
        }
        sum/=20;
        return sum;
    }

    protected double getDeviation(List<JSONObject> candles, double movePoint20){
        double sum = 0;
        for(int i=0; i<20; i++){
            double value = (double) candles.get(i).get("trade_price");
            sum+=(Math.pow((value-movePoint20),2));
        }
        sum/=20;
        sum = Math.sqrt(sum);
        return sum;
    }

    protected double getUpDeviation(List<JSONObject> candles){
        double movePoint20 = getMovePoint20(candles);
        double deviation = getDeviation(candles,movePoint20);
        return movePoint20+(deviation*2);
    }
}
