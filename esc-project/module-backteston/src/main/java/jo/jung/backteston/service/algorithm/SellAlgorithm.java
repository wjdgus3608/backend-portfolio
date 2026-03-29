package jo.jung.backteston.service.algorithm;

import jo.jung.backteston.entity.MinuteCandleEntity;
import jo.jung.backteston.service.signal.CustomParam;

import java.util.List;

public interface SellAlgorithm {
    long evaluate(CustomParam customParam);
    String getDescription();
}
