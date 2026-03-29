package jo.jung.backteston.service.algorithm;

import jo.jung.backteston.service.signal.CustomParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Function;

@Slf4j
@AllArgsConstructor
@Getter
public class SellBuyAlgorithmImpl implements SellAlgorithm {
    private List<Function<CustomParam, Long>> signalFunctions;
    private String description; // 조합 정보

    @Override
    public long evaluate(CustomParam customParam) {
        long result = 0L;
        for (Function<CustomParam, Long> func : signalFunctions) {
            if(result != 0) break;
            result = func.apply(customParam);
        }

        return result;
    }

    @Override
    public String getDescription(){
        return description;
    }
}
