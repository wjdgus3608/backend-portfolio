package jo.jung.backteston.service.signal;

import jo.jung.backteston.entity.MinuteCandleEntity;
import jo.jung.domain.trade.Trade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CustomParam {
    private List<MinuteCandleEntity> candles;
    private Trade myStock;
    private float n;
    private float painN;
}
