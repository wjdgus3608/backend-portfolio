package jo.jung.backteston.service.algorithm;

import jo.jung.backteston.entity.MinuteCandleEntity;
import jo.jung.backteston.service.signal.BuySignal;
import jo.jung.backteston.service.signal.CustomParam;
import jo.jung.backteston.service.signal.SellSignal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class AlgorithmGenerator {

    private final int MAX_SIZE = 1;
    private final int MAX_SELL_SIZE = 2;

    @Autowired
    private BuySignal buySignal;

    @Autowired
    private SellSignal sellSignal;

    public List<BuyAlgorithm> generateAllBuyAlgorithms() {
        Map<String, Function<List<MinuteCandleEntity>, Long>> funcs = buySignal.getAllSignalFunctions();
        List<BuyAlgorithm> algorithms = new ArrayList<>();

        List<String> keys = new ArrayList<>(funcs.keySet());
        int n = keys.size();

        for (int mask = 1; mask < (1 << n); mask++) {
            List<Function<List<MinuteCandleEntity>, Long>> selectedFuncs = new ArrayList<>();
            List<String> selectedNames = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    String key = keys.get(i);
                    selectedFuncs.add(funcs.get(key));
                    selectedNames.add(key);
                }
            }

            int size = selectedFuncs.size();
            if(size == MAX_SIZE) {
                String description = String.join(" + ", selectedNames);
                algorithms.add(new BuyBuyAlgorithmImpl(selectedFuncs, description));
            }
        }

        return algorithms;
    }


    public List<SellAlgorithm> generateAllSellAlgorithms() {
        Map<String, Function<CustomParam, Long>> funcs = sellSignal.getAllSignalFunctions();
        List<SellAlgorithm> algorithms = new ArrayList<>();

        List<String> keys = new ArrayList<>(funcs.keySet());
        int n = keys.size();

        for (int mask = 1; mask < (1 << n); mask++) {
            List<Function<CustomParam, Long>> selectedFuncs = new ArrayList<>();
            List<String> selectedNames = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    String key = keys.get(i);
                    selectedFuncs.add(funcs.get(key));
                    selectedNames.add(key);
                }
            }

            int size = selectedFuncs.size();
            if(size == MAX_SELL_SIZE){
                String description = String.join(" + ", selectedNames);
                algorithms.add(new SellBuyAlgorithmImpl(selectedFuncs, description));
            }
        }

        return algorithms;
    }
}
