package jo.jung.common.apiclient;

import jo.jung.common.logclient.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@Slf4j
public class ApiUtil {

    private final WebClient webClient;

    // baseUrl을 application.yml에서 주입받음
    public ApiUtil(@Value("${my.api-client-server}") String baseUrl) {
//        log.info("baseUrl :"+baseUrl);
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 비동기 POST 요청 (JSON)
     */
    public <T> Flux<T> post(String uri, Object requestBody, Class<T> responseType) {
//        log.info("POST req : "+uri);
        return webClient.post()
                .uri(uri)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(responseType)
                .retryWhen(Retry
                        .fixedDelay(3, Duration.ofSeconds(2)) // 최대 3번, 2초 간격
                        .filter(throwable -> throwable instanceof WebClientResponseException)
                );
//                .doOnNext(response -> log.info("post res : "+response));
    }

    /**
     * 비동기 GET 요청
     */
    public <T> Flux<T> get(String uri, Class<T> responseType) {
//        log.info("GET 호출 : "+uri);
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(responseType);
    }
}
