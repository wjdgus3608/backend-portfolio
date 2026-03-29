package jo.jung.common.priceutil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

public class PriceUtil {

    private static final float TAX_RATE = 0.0025f;

    public static int getTick(long pricePerStock){
        if(pricePerStock < 2000)
            return 1;
        else if(pricePerStock < 5000)
            return 5;
        else if(pricePerStock < 20000)
            return 10;
        else if(pricePerStock < 50000)
            return 50;
        else if(pricePerStock < 200000)
            return 100;
        else if(pricePerStock < 500000)
            return 500;
        else
            return 1000;
    }

    public static long getEarnMoney(long buyPrice, long sellPrice, long hasAmount){
        long buyValue = buyPrice * hasAmount;
        long sellValue = applyTax(sellPrice * hasAmount);
        return sellValue - buyValue;
    }

    public static long applyTax(long price){
        return (long)(price * (1f - TAX_RATE));
    }

}
