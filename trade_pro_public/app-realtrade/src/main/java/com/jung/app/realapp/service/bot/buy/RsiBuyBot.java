package com.jung.app.realapp.service.bot.buy;

import com.jung.app.realapp.service.bot.GlobalBotControll;
import com.jung.app.realapp.service.bot.NormalBot;
import com.jung.client.api.ApiClient;
import com.jung.common.box.MyBox;
import com.jung.common.log.LogUtil;
import com.jung.domain.coin.Coin;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RsiBuyBot extends NormalBot implements CommonBuyBot {

    private static int TOP_COIN_LIMIT=0;
    private static int CANDLE_CNT=100;

    private static List coinList;

    public RsiBuyBot(MyBox myBox, ApiClient apiClient) {
        super(myBox, apiClient);
    }

    @Override
    public int buyFilter(String cName) {
        List<JSONObject> candles = apiClient.searchMinuteCandle(5, cName, null, CANDLE_CNT);
        JSONObject nowCandle = candles.get(0);
        double nowPrice = (double) nowCandle.get("trade_price");

        double downDeviation = getDownDeviation(candles);
        double movePoint20 = getMovePoint20(candles);
        double deviation = getDeviation(candles,movePoint20);
        double limit = 33;
        double rsi = getRsi(candles);


        if((movePoint20-downDeviation)<=getPriceLevel(nowPrice)*3) return 0;
        if(rsi<=limit){
            return 5;
        }
        return 0;
    }

    @Override
    public void buy() {
        if(!isMoneyRemain()){
            LogUtil.printLog(" no money... ");
            LogUtil.saveLog(" no money... ");
            return;
        }

        if(coinList == null){
            LogUtil.printLog("get coinList from api!!");
            LogUtil.saveLog("get coinList from api!!");
            coinList = apiClient.searchAllCoins();
            LogUtil.saveLog(" coinList size : "+coinList.size());
        }
        if(GlobalBotControll.getCheckTimeCnt()>10){
            int cnt = 0;
            for(Object obj:coinList) {
                JSONObject coin = (JSONObject) obj;
                String cName = (String) coin.get("market");
                JSONObject json = (JSONObject) apiClient.getNowInfo(cName).get(0);
                String ret = (String) json.get("change");
                if(ret.equals("FALL")){
                    cnt++;
                }
                if(cnt>(coinList.size()/11*9)) break;
            }
            if(cnt>(coinList.size()/11*9)){
                LogUtil.printLog("down market... take a rest!!");
                LogUtil.saveLog("down market... take a rest!!");
                GlobalBotControll.setIsDownMarket(true);
                return;
            }
            GlobalBotControll.setIsDownMarket(false);
            GlobalBotControll.setCheckTimeCnt(0);
        }

        TOP_COIN_LIMIT = coinList.size();

        for(int i=0; i<TOP_COIN_LIMIT; i++){
            Object obj = coinList.get(i);
            JSONObject coin = (JSONObject) obj;
            String cName = (String) coin.get("market");
            Coin tmp = Coin.builder().cName(cName).build();
            int buyFilterResult = buyFilter(cName);
            if(!myBox.getCoins().contains(tmp) &&  buyFilterResult > buyLimit){
                if(GlobalBotControll.isContainsLostCoinMap(cName)){
                    LogUtil.printLog("this coin BAD!! "+cName);
                    LogUtil.saveLog("this coin BAD!! "+cName);
                    continue;
                }
                buyCoin(cName,ticket);
                LogUtil.printLog("RsiBuyBot buy "+cName);
                LogUtil.saveLog("RsiBuyBot buy "+cName);
                GlobalBotControll.addCoinToMap(cName,1);
            }
            if(!isMoneyRemain())
                break;
        }
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

    protected double getDownDeviation(List<JSONObject> candles){
        double movePoint20 = getMovePoint20(candles);
        double deviation = getDeviation(candles,movePoint20);
        return movePoint20-(deviation*2);
    }

    protected double getRsi(List<JSONObject> candles){
        double[] arr = new double[14];
        int index = 0;
        for(int i=CANDLE_CNT-15; i<CANDLE_CNT-1; i++){
            double value = (double) candles.get(i).get("trade_price");
            double value2 = (double) candles.get(i+1).get("trade_price");
            arr[index++]=value-value2;
        }
        double posSum = 0;
        double NegSum = 0;
        for(double num:arr){
            if(num<0){
                NegSum+=Math.abs(num);
            }
            else posSum+=num;
        }
        double AU = posSum/14;
        double AD = NegSum/14;
        for(int i=CANDLE_CNT-14; i>0; i--){
            double value = (double) candles.get(i).get("trade_price");
            double value2 = (double) candles.get(i-1).get("trade_price");
            double diff = value2-value;
            if(diff<0){
                AD=((13*AD)+Math.abs(diff))/14;
                AU=((13*AU)+0)/14;
            }
            else if(diff==0){
                AU=((13*AU)+0)/14;
                AD=((13*AD)+0)/14;
            }
            else {
                AU=((13*AU)+diff)/14;
                AD=((13*AD)+0)/14;
            }
        }

        double RS = AU/AD;
        double RSI = RS/(1+RS);
        return RSI*100;
    }

}
