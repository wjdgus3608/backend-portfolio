package jo.jung.realtrademodule.service.account;

import jo.jung.common.apiclient.ApiUtil;
import jo.jung.domain.stock.Stock;
import jo.jung.logic.service.account.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final ApiUtil apiUtil;

    @Override
    public Flux<Stock> getAccount() {
        return apiUtil.get("/api/v1/my-box",Stock.class);
    }

    @Override
    public Flux<Long> getMoney() {
        return apiUtil.get("/api/v1/my-money", Long.class);
    }


}
