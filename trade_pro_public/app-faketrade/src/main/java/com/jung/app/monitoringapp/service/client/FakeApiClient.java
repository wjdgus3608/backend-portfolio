package com.jung.app.monitoringapp.service.client;

import com.jung.app.monitoringapp.service.box.FakeMyBox;
import com.jung.client.api.UpbitApiClient;
import com.jung.common.box.MyBox;
import com.jung.common.log.LogUtil;
import com.jung.domain.coin.Coin;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FakeApiClient extends UpbitApiClient {
    private final MyBox myBox;

    @Value("${ticket}")
    private String ticket;

    @Override
    public ResponseEntity buy(JSONObject json) {
        String cName = (String) json.get("market");
        long price = Long.parseLong((String) json.get("price"));
        double nowPrice = getNowPrice(cName);


        FakeMyBox fakeMyBox = (FakeMyBox) myBox;
        Coin coin = null;
        if(fakeMyBox.isCoinExist(cName)){
            Coin preCoin = fakeMyBox.getCoinIfExist(cName);
            coin = Coin.builder()
                    .cName(cName)
                    .earnAmount(0l)
                    .earnRate(0d)
                    .cAmount(preCoin.getCAmount()+price/nowPrice)
                    //시장가 매수시 1틱 비싸게 삼
                    .buyAvgPrice((preCoin.getBuyAvgPrice()+nowPrice+getPriceLevel(nowPrice))/2)
                    .valuePrice((long)nowPrice)
                    .buyAmount(preCoin.getBuyAmount()+price)
                    .buyTime(LocalDateTime.now())
                    .build();
            fakeMyBox.removeCoinFromList(cName);
        }
        else{
            coin = Coin.builder()
                    .cName(cName)
                    .earnAmount(0l)
                    .earnRate(0d)
                    .cAmount(price/nowPrice)
                    //시장가 매수시 1틱 비싸게 삼
                    .buyAvgPrice(nowPrice+getPriceLevel(nowPrice))
                    .valuePrice((long)nowPrice)
                    .buyAmount(price)
                    .buyTime(LocalDateTime.now())
                    .build();
        }


        fakeMyBox.addCoinToList(coin);
        fakeMyBox.setMoney(fakeMyBox.getMoney()-price);

        LogUtil.printLog("buy at "+nowPrice);
        LogUtil.saveLog("buy at "+nowPrice);
        LogUtil.printLog(fakeMyBox.getBoxNow());
        LogUtil.saveLog(fakeMyBox.getBoxNow());
        upAndCheckRequestCnt();
        return null;
    }

    @Override
    public ResponseEntity sell(JSONObject json) {
        String cName = (String) json.get("market");
        double volume = Double.parseDouble((String) json.get("volume"));
        double nowPrice = (double) json.get("nowPrice");

        FakeMyBox fakeMyBox = (FakeMyBox) myBox;
        //시장가 매도시 1틱 싸게팜
        double earnMoney = Math.floor(calculateEarnMoney(volume,nowPrice-getPriceLevel(nowPrice))-Double.parseDouble(ticket)
                -(Double.parseDouble(ticket)*0.001));
        DecimalFormat decimalFormat = new DecimalFormat("###,###");
        String money = decimalFormat.format(earnMoney);
        fakeMyBox.removeCoinFromList(cName);
        if(earnMoney>=0){
            LogUtil.saveLog("earn money "+money);
        }
        else{
            LogUtil.saveLog("lost money "+money);
        }

        fakeMyBox.setMoney(fakeMyBox.getMoney()+
                Double.parseDouble(ticket)+Double.parseDouble(money));

        LogUtil.printLog(fakeMyBox.getBoxNow());
        LogUtil.saveLog(fakeMyBox.getBoxNow());
        upAndCheckRequestCnt();
        return null;
    }

    private double calculateEarnMoney(double volume, double nowPrice){
        return volume*nowPrice;
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

}
