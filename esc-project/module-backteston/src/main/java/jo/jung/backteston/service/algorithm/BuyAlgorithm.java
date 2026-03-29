package jo.jung.backteston.service.algorithm;

import jo.jung.backteston.entity.MinuteCandleEntity;
import jo.jung.backteston.service.signal.CustomParam;

import java.util.List;

public interface BuyAlgorithm {
    long evaluate(List<MinuteCandleEntity> candles);
    String getDescription();
}
