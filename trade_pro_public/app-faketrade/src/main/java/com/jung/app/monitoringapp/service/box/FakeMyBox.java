package com.jung.app.monitoringapp.service.box;

import com.jung.common.box.MyBox;
import com.jung.domain.coin.Coin;
import lombok.ToString;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ToString
public class FakeMyBox implements MyBox {

    private double money;
    private List<Coin> coinList = new CopyOnWriteArrayList<>();
    public void setMoney(double money){
        this.money=money;
    }

    public void addCoinToList(Coin coin){
        coinList.add(coin);
    }

    public void removeCoinFromList(String cName){
        for(Coin coin:coinList){
            if(coin.getCName().equals(cName)){
                coinList.remove(coin);
                break;
            }
        }
    }

    public boolean isCoinExist(String cName){
        for(Coin coin:coinList){
            if(coin.getCName().equals(cName)){
                return true;
            }
        }
        return false;
    }

    public Coin getCoinIfExist(String cName){
        for(Coin coin:coinList){
            if(coin.getCName().equals(cName)){
                return coin;
            }
        }
        return null;
    }

    @Override
    public double getMoney() {
        return money;
    }

    @Override
    public List<Coin> getCoins() {
        return coinList;
    }

    @Override
    public String getBoxNow() {
        double sum=0;
        for(Coin coin:coinList){
            sum+=coin.getBuyAmount();
        }
        DecimalFormat decimalFormat = new DecimalFormat("###,###");
        return "total : "+decimalFormat.format(getMoney()+sum)
                +" coin cnt : "+getCoins().size()
                + " money left : "+decimalFormat.format(getMoney());
    }

}
