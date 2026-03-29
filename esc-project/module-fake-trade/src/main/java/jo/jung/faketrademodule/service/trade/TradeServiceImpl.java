package jo.jung.faketrademodule.service.trade;

import jo.jung.domain.stock.Stock;
import jo.jung.faketrademodule.service.account.MyBoxUtil;
import jo.jung.logic.service.trade.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeServiceImpl implements TradeService {

    private final MyBoxUtil myBoxUtil;

    @Override
    public Mono<Boolean> callBuy(Stock stock) {
        return myBoxUtil.buyStock(stock);
    }

    @Override
    public Mono<Boolean> callSell(Stock stock) {
        return myBoxUtil.sellStock(stock);
    }

}
