package com.jung.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {


    @Value("${my.log-module.url}")
    private String URL1;

    public ResponseEntity<?> getLogList(){
        WebClient webClient = WebClient.builder()
                .baseUrl(URL1)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set("content-type","application/json; charset=utf-8");
                })
                .build();

        List response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/logs")
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        log.info("getLogList : "+response);

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getLogDetail(String searchTime){

        WebClient webClient = WebClient.builder()
                .baseUrl(URL1)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set("content-type","application/json; charset=utf-8");
                })
                .build();

        List response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/log/" + searchTime)
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        return ResponseEntity.ok(response);
    }
}
