package com.jung.app.common;

import com.jung.app.domain.vo.em.LongShortType;

public class PriceUtil {
    private static final double TAX_RATE_MAKER = 0.0002;
    private static final double TAX_RATE_TAKER = 0.0004;

    public static double getEarnMoney(double buyPrice, double sellPrice, double hasAmount, LongShortType longShortType){
        double buyValue = buyPrice * hasAmount;
        double sellValue = sellPrice * hasAmount;

        double buyFee = getTaxValue(buyValue);
        double sellFee = getTaxValue(sellValue);

        if(longShortType.equals(LongShortType.LONG))
            return sellValue - sellFee - (buyValue + buyFee);
        else
            return buyValue - buyFee - (sellValue + sellFee);
    }

    public static double getTaxValue(double value){
        return (value * TAX_RATE_MAKER);
    }
    public static double getTaxValueTaker(double value){
        return (value * TAX_RATE_TAKER);
    }


}
