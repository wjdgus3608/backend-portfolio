package com.jung.app.realapp.service.bot.sell;

import com.jung.app.realapp.service.bot.GlobalBotControll;
import com.jung.app.realapp.service.bot.NormalBot;
import com.jung.app.realapp.service.bot.buy.PlusBuyBot;
import com.jung.client.api.ApiClient;
import com.jung.common.box.MyBox;
import com.jung.common.log.LogUtil;
import com.jung.domain.coin.Coin;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class XPercentDownWithPlusSellBot extends NormalBot implements LostSellBot{

    private PlusBuyBot plusBuyBot;
    private static List coinList;

    public XPercentDownWithPlusSellBot(MyBox myBox, ApiClient apiClient, PlusBuyBot plusBuyBot) {
        super(myBox, apiClient);
        this.plusBuyBot = plusBuyBot;
    }

    @Override
    public int sellFilter(Object coin, double nowPrice) {
        String cName = ((Coin)coin).getCName();
        double myPrice = ((Coin) coin).getBuyAvgPrice();
        if(calculatePercent(myPrice,nowPrice)>=(xPercentDownLimit*0.01)){
            return 5;
        }
        return 0;
    }

    public int sellFilterLostSmaller(Object coin, double nowPrice) {
        String cName = ((Coin)coin).getCName();
        double myPrice = ((Coin) coin).getBuyAvgPrice();
        if(calculatePercent(myPrice,nowPrice)<=(xPercentDownLimit/4*0.01)){
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

            if(GlobalBotControll.isAvoidCoin(cName)){
                continue;
            }
            double nowPrice = (double) apiClient.getNowPrice(cName);
            if(nowPrice == -1) continue;
            int sellFilterResult = sellFilter(coin,nowPrice);
            int sellFilterSmallerResult = sellFilterLostSmaller(coin,nowPrice);
            boolean isSmallerResult = (isCoinHasTwo(cName) && (sellFilterSmallerResult>lostSellLimit));
            if(sellFilterResult>lostSellLimit && isCoinHasOne(cName)){
//                plusBuyBot.plusBuy(cName);
            }
            else if(isSmallerResult
                    || sellFilterResult>lostSellLimit){
                if(isSmallerResult){
                    LogUtil.printLog("SmallerDownSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                    LogUtil.saveLog("SmallerDownSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                }
                LogUtil.printLog("XPercentDownSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                LogUtil.saveLog("XPercentDownSellBot sell "+cName+" "+coin.getBuyAvgPrice()+"->"+nowPrice);
                sellCoin(cName, coin.getCAmount(),nowPrice);

                GlobalBotControll.removeCoinFromMap(cName);
                GlobalBotControll.addCoinToLostMap(cName, System.currentTimeMillis());

                String str = myBox.getBoxNow();
                LogUtil.printLog(str);
                LogUtil.saveLog(str);
            }
        }
    }
    protected boolean isCoinHasOne(String cName){
        return GlobalBotControll.getCoinMap().containsKey(cName) &&
                GlobalBotControll.getCoinMap().get(cName)==1;
    }

    protected boolean isCoinHasTwo(String cName){
        return GlobalBotControll.getCoinMap().containsKey(cName) &&
                GlobalBotControll.getCoinMap().get(cName)==2;
    }

}
