package com.jung.app.monitoringapp.service.bot;

import com.jung.client.api.ApiClient;
import com.jung.common.box.MyBox;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NormalBot {

    protected final MyBox myBox;
    protected final ApiClient apiClient;

    @Value("${buyLimit}")
    protected int buyLimit;
    @Value("${tradeAmountUpLimit}")
    protected double tradeAmountUpLimit;
    @Value("${ticket}")
    protected long ticket;
    @Value("${earnSellLimit}")
    protected int earnSellLimit;
    @Value("${lostSellLimit}")
    protected int lostSellLimit;
    @Value("${xPercentUpLimit}")
    protected double xPercentUpLimit;
    @Value("${xPercentDownLimit}")
    protected double xPercentDownLimit;

    protected boolean isMoneyRemain(){
        return myBox.getMoney() >= ticket;
    }

    protected JSONObject makeBillToJson(String cName, long ticket){
        JSONObject json = new JSONObject();
        json.put("market",cName);
        json.put("price",Long.toString(ticket));
        return json;
    }

    protected void buyCoin(String cName, long ticket){
        JSONObject json = makeBillToJson(cName,ticket);
        apiClient.buy(json);
    }

    protected boolean isPriceGoingUp(JSONObject pre, JSONObject now){
        double prePrice = (double) pre.get("trade_price");
        double nowPrice = (double) now.get("trade_price");
        double priceLevel = getPriceLevel(prePrice);
        return nowPrice>=(prePrice+(priceLevel*2));
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


    protected double calculateAmountUpRate(double preAmount, double nowAmount){
        return (nowAmount/preAmount);
    }

    protected int calculateWeight(double upRate, double limitRate){
        double rate = (upRate/limitRate);
        if(rate>=1){
            return 5;
        }
        return (int)Math.round(5*rate);
    }

    protected double getFirstPoint(double[] prices, int period){
        double sum = 0;
        for(int i=prices.length-period; i<prices.length; i++){
            sum+=prices[i];
        }
        sum/=period;
        return sum;
    }
    protected double getEMA(double[] prices, int period, int end){
        double firstPoint = getFirstPoint(prices,period);
        double k = 2.0/(period+1);
        double ema = 0;
        for(int i=0; i<end; i++){
            double price = prices[i];
            if(i==0){
                ema = (price*k)+(firstPoint*(1-k));
            }
            else{
                ema = (price*k)+(ema*(1-k));
            }
        }
        return ema;
    }

    protected double[] getPricesFromCandles(List<JSONObject> candles){
        double[] prices = new double[candles.size()];
        int i=candles.size()-1;
        for(JSONObject candle:candles){
            double price = (double) candle.get("trade_price");
            prices[i--]=price;
        }
        return prices;
    }

    protected double getRsi(List<JSONObject> candles, int candleCnt){
        double[] arr = new double[14];
        int index = 0;
        for(int i=candleCnt-15; i<candleCnt-1; i++){
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
        for(int i=candleCnt-14; i>0; i--){
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

    protected boolean isTradeAmountGrowing(int hour, List<JSONObject> candles, int candleCnt){
        int minute = Integer.parseInt(GlobalBotControll.getNowTime()[1]);
        if((minute%5)<1) return false;
        double topAmount = 0;
        int start = hour*12;
        for(int i=Math.min(start,candleCnt-1); i>0; i--){
            JSONObject candle = candles.get(i);
            double amount = (double) candle.get("candle_acc_trade_volume");
            topAmount = Math.max(topAmount,amount);
        }
        JSONObject candle = candles.get(0);
        double nowAmount = (double) candle.get("candle_acc_trade_volume");


        double rate = (minute%5)/5.0;
        if((topAmount*(rate)*1.1)<=nowAmount){
            return true;
        }
        return false;
    }

    protected boolean isMinusGraphThreeTimes(JSONObject pre, JSONObject now){
        double prePrice = (double) pre.get("trade_price");
        double nowPrice = (double) now.get("trade_price");
        double priceLevel = getPriceLevel(prePrice);
        return (nowPrice+(priceLevel*2))<=prePrice;
    }

    protected boolean isGraphEatPre(JSONObject pre, JSONObject now){
        double preHighPrice = (double) pre.get("high_price");
        double nowStartPrice = (double) now.get("opening_price");
        double prePrice = (double) pre.get("trade_price");
        double nowPrice = (double) now.get("trade_price");
        if(preHighPrice<=prePrice || nowStartPrice>=nowPrice){
            return false;
        }
        double priceLevel = getPriceLevel(prePrice);
        if(preHighPrice<=(prePrice+(priceLevel*2)) || preHighPrice>=nowPrice){
            return false;
        }
        return true;
    }

    protected Double calculatePercent(double myPrice, double nowPrice){
        return (nowPrice/myPrice)-1;
    }
    protected Double calculateTick(double myPrice, double nowPrice){
        double tick = getPriceLevel(myPrice);
        return (nowPrice-myPrice)/tick;
    }

    protected JSONObject makeBillToJson(String cName, double cAmount, double nowPrice){
        JSONObject json = new JSONObject();
        json.put("market",cName);
        json.put("volume",Double.toString(cAmount));
        json.put("nowPrice",nowPrice);
        return json;
    }


    protected void sellCoin(String cName, double cAmount, double nowPrice){
        JSONObject json = makeBillToJson(cName, cAmount, nowPrice);
        apiClient.sell(json);
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

    protected double getMovePoint5(List<JSONObject> candles){
        double sum = 0;
        for(int i=0; i<5; i++){
            double value = (double) candles.get(i).get("trade_price");
            sum+= value;
        }
        sum/=5;
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

    protected double getDeviation10(List<JSONObject> candles, double movePoint10){
        double sum = 0;
        for(int i=0; i<10; i++){
            double value = (double) candles.get(i).get("trade_price");
            sum+=(Math.pow((value-movePoint10),2));
        }
        sum/=10;
        sum = Math.sqrt(sum);
        return sum;
    }

}
