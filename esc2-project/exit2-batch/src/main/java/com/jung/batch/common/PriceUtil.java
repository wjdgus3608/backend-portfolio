package com.jung.batch.common;


import com.jung.batch.domain.em.LongShortType;

public class PriceUtil {
    private static final double TAX_RATE = 0.0002;

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
        return (value * TAX_RATE);
    }


}
