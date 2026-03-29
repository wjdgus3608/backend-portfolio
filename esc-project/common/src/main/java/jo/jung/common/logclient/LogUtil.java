package jo.jung.common.logclient;

import jo.jung.domain.log.LogInsertReqDTO;
import jo.jung.domain.log.LogSelectResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class LogUtil {

    private final WebClient webClient;

    // baseUrl을 application.yml에서 주입받음
    public LogUtil(@Value("${my.db-client-server}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void saveLog(String type, String message){
        webClient.post()
                .uri("/log")
                .bodyValue(LogInsertReqDTO.builder()
                        .type(type)
                        .message(message)
                        .build())
                .retrieve()
                .bodyToMono(LogSelectResDTO.class)
                .subscribe();
    }

    public void saveAndPrintLog(String type, String message){
        log.info(message);
//        saveLog(type, message);
    }

    public Flux<LogSelectResDTO> getLogs(String type, String message){
        return webClient.get()
                .uri("/log")
                .retrieve()
                .bodyToFlux(LogSelectResDTO.class);
    }


}
