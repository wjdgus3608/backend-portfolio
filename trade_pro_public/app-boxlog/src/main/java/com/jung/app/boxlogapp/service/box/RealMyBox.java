package com.jung.app.boxlogapp.service.box;

import com.jung.client.api.ApiClient;
import com.jung.common.box.MyBox;
import com.jung.domain.coin.Coin;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@ToString
@Component
@RequiredArgsConstructor
public class RealMyBox implements MyBox {

    private final ApiClient apiClient;
    //임시
    @Value("${ticket}")
    private long ticket;

    @Override
    public double getMoney() {
        List<JSONObject> list = (List<JSONObject>) apiClient.getMyBox();
        double money = 0;
        for(Object obj:list){
            JSONObject json = (JSONObject) obj;
            if(json.get("currency").equals("KRW")){
                money = Double.parseDouble((String) json.get("balance"));
                break;
            }
        }
        return money;
    }

    @Override
    public List<Coin> getCoins() {
        List<JSONObject> list = (List<JSONObject>) apiClient.getMyBox();
        List<Coin> coinList = new ArrayList<>();
        for(Object obj:list){
            JSONObject json = (JSONObject) obj;
            String cName = (String) json.get("currency");
            if(cName.equals("KRW")) continue;
            cName = "KRW-"+cName;
            double buyAvgPrice = Double.parseDouble((String) json.get("avg_buy_price"));
            double cAmount = Double.parseDouble((String) json.get("balance"));
            Coin coin = Coin.builder()
                    .cName(cName)
                    .buyAvgPrice(buyAvgPrice)
                    .cAmount(cAmount)
                    .build();
            coinList.add(coin);
        }
        return coinList;
    }

    @Override
    public String getBoxNow() {
        List<Coin> coinList = getCoins();
        //운영
//        double coinValueSum = 0;
//        for(Coin coin: coinList){
//            coinValueSum+= Math.ceil(coin.getBuyAvgPrice()*coin.getCAmount());
//        }
//        DecimalFormat decimalFormat = new DecimalFormat("###,###");
//        return "total : "+decimalFormat.format((long)(getMoney()+(coinValueSum)))+
//                " coin cnt : "+getCoins().size() +
//                " money left : "+decimalFormat.format((long)getMoney());
        //임시
        int coinCnt = 0;
        long sum = 0;
        for(Coin coin: coinList){
            String cName = coin.getCName();
            if(GlobalBotControll.isAvoidCoin(cName) || coin.getBuyAvgPrice()<=0)
                continue;
            coinCnt++;
            sum+=Math.round(coin.getBuyAvgPrice()*coin.getCAmount());
        }
        DecimalFormat decimalFormat = new DecimalFormat("###,###");
        double nowMoney = getMoney();
        String total = decimalFormat.format(nowMoney+sum);

        return "total : "+total+
                " coin cnt : "+coinCnt+
                " money left : "+decimalFormat.format(nowMoney);
    }

}



//package com.jung.app.boxlogapp.service.box;
//
//import com.jung.client.api.ApiClient;
//import com.jung.common.box.MyBox;
//import com.jung.domain.coin.Coin;
//import lombok.RequiredArgsConstructor;
//import lombok.ToString;
//import org.json.simple.JSONObject;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.text.DecimalFormat;
//import java.util.ArrayList;
//import java.util.List;
//
//@ToString
//@Component
//@RequiredArgsConstructor
//public class RealMyBox implements MyBox {
//
//    private final ApiClient apiClient;
//    //임시
//    @Value("${ticket}")
//    private long ticket;
//
//    @Override
//    public double getMoney() {
//        List<JSONObject> list = (List<JSONObject>) apiClient.getMyBox();
//        double money = 0;
//        for(Object obj:list){
//            JSONObject json = (JSONObject) obj;
//            if(json.get("currency").equals("KRW")){
//                money = Double.parseDouble((String) json.get("balance"));
//                break;
//            }
//        }
//        return money;
//    }
//
//    @Override
//    public List<Coin> getCoins() {
//        List<JSONObject> list = (List<JSONObject>) apiClient.getMyBox();
//        List<Coin> coinList = new ArrayList<>();
//        for(Object obj:list){
//            JSONObject json = (JSONObject) obj;
//            String cName = (String) json.get("currency");
//            if(cName.equals("KRW")) continue;
//            cName = "KRW-"+cName;
//            double buyAvgPrice = Double.parseDouble((String) json.get("avg_buy_price"));
//            double cAmount = Double.parseDouble((String) json.get("balance"));
//            Coin coin = Coin.builder()
//                    .cName(cName)
//                    .buyAvgPrice(buyAvgPrice)
//                    .cAmount(cAmount)
//                    .build();
//            coinList.add(coin);
//        }
//        return coinList;
//    }
//
//    @Override
//    public String getBoxNow() {
//        List<Coin> coinList = getCoins();
//        //운영
////        double coinValueSum = 0;
////        for(Coin coin: coinList){
////            coinValueSum+= Math.round(coin.getBuyAvgPrice()*coin.getCAmount());
////        }
////        DecimalFormat decimalFormat = new DecimalFormat("###,###");
////        return "total : "+decimalFormat.format((long)(getMoney()+(coinValueSum)))+
////                " coin cnt : "+getCoins().size() +
////                " money left : "+decimalFormat.format((long)getMoney());
//
//        //임시
//        int coinCnt = 0;
//        for(Coin coin: coinList){
//            String cName = coin.getCName();
//            if(coin.getBuyAvgPrice()<=0)
//                continue;
//            coinCnt++;
//        }
//        coinCnt-=27;
//        DecimalFormat decimalFormat = new DecimalFormat("###,###");
//        String total = decimalFormat.format(getMoney()+coinCnt*ticket);
//
//        return "total : "+total+
//                " coin cnt : "+coinCnt+
//                " money left : "+decimalFormat.format(getMoney());
//    }
//
//}
