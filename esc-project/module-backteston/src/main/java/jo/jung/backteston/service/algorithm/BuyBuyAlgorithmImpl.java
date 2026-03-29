package jo.jung.backteston.service.algorithm;

import jo.jung.backteston.entity.MinuteCandleEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Slf4j
@AllArgsConstructor
@Getter
public class BuyBuyAlgorithmImpl implements BuyAlgorithm {
    private List<Function<List<MinuteCandleEntity>, Long>> signalFunctions;
    private String description; // 조합 정보

    @Override
    public long evaluate(List<MinuteCandleEntity> candles) {
        long result = 0L;
        List<Long> prices = new ArrayList<>();
        for (Function<List<MinuteCandleEntity>, Long> func : signalFunctions) {
            result = func.apply(candles);
            if(result==-1) return -1;
            prices.add(result);
        }
        Collections.sort(prices,Collections.reverseOrder());

        return prices.getFirst();
    }

    @Override
    public String getDescription(){
        return description;
    }
}
