package jo.jung.faketrademodule.service.account;

import jo.jung.common.kafka.KafkaProducerFactory;
import jo.jung.domain.stock.Stock;
import jo.jung.logic.service.account.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {



    @Override
    public Flux<Stock> getAccount() {
        return Flux.empty();
    }

    @Override
    public Flux<Long> getMoney() {
        return null;
    }
}
