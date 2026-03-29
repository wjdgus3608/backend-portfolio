package jo.jung.realtrademodule.service.trade;

import jo.jung.common.apiclient.ApiUtil;
import jo.jung.domain.api.OrderReqDTO;
import jo.jung.domain.nowprice.NowPrice;
import jo.jung.domain.stock.Stock;
import jo.jung.logic.service.account.AccountService;
import jo.jung.logic.service.trade.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeServiceImpl implements TradeService {

    private final ApiUtil apiUtil;
    private final AccountService accountService;

    @Value("${my.account-front}")
    private String accountFront;

    @Value("${my.account-back}")
    private String accountBack;

    @Value("${my.ticket}")
    private String ticket;

    @Override
    public Mono<Boolean> callBuy(Stock stock) {

        return apiUtil.post("/api/v1/stock-now-price", stock, NowPrice.class)
                .single()
                .flatMap(nowPrice -> {

                    // 예: 현재가 가져오기
                    long price = nowPrice.getNowPrice(); // 실제 필드명에 맞게 수정
                    long buyAmount = (long) Math.floor((Long.parseLong(ticket)*0.99)/price);
                    log.info(stock.getStockName()+" 매수시점 현재가 : "+price+"원");
                    // 1️⃣ 잔액 조회
                    return accountService.getMoney().single() // Flux<Long> -> Mono<Long>
                            .flatMap(balance -> {

                                // 2️⃣ 잔액 부족 시 바로 종료
                                if (balance < buyAmount*price) {
                                    log.info("잔액 부족 종료 잔액/가격 : "+balance+"/"+(buyAmount*price));
                                    return Mono.just(false);
                                }

                                OrderReqDTO reqDTO = OrderReqDTO.builder()
                                        .CANO(accountFront)
                                        .ACNT_PRDT_CD(accountBack)
                                        .PDNO(stock.getStockShortCode())
                                        .ORD_DVSN("01") // 시장가
                                        .ORD_QTY(Long.toString(buyAmount))
                                        .ORD_UNPR("0")
                                        .EXCG_ID_DVSN_CD("KRX")
                                        .build();
                                // 4️⃣ 주문 호출
                                return apiUtil.post("/api/v1/order-buy", reqDTO, JSONObject.class)
                                        .single()
                                        .map(response -> {
                                            log.info("주문 호출 응답:"+response);
                                            return "0".equals((String) response.get("rt_cd"));
                                        })
                                        .onErrorReturn(false);
                            });
                })
                .onErrorReturn(false); // 에러 시 false
    }

    @Override
    public Mono<Boolean> callSell(Stock stock) {
            OrderReqDTO reqDTO = OrderReqDTO.builder()
                    .CANO(accountFront)
                    .ACNT_PRDT_CD(accountBack)
                    .PDNO(stock.getStockShortCode())
                    .SLL_TYPE("01")
                    .ORD_DVSN("01")//시장가
                    .ORD_QTY(Long.toString(stock.getHasAmount()))
                    .ORD_UNPR("0")
                    .EXCG_ID_DVSN_CD("KRX")
                    .build();

            // 4️⃣ 주문 호출
            return apiUtil.post("/api/v1/order-sell", reqDTO, JSONObject.class)
                    .single()
                    .map(response -> "0".equals((String) response.get("rt_cd")))
                    .onErrorReturn(false);
    }

}
