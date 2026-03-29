package jo.jung.backteston.service.signal;

import jo.jung.backteston.entity.MinuteCandleEntity;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface SellSignal {
    Map<String, Function<CustomParam, Long>> getAllSignalFunctions();
}
