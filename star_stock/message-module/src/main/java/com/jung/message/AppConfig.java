package com.jung.message;

import com.jung.message.api.KakaoMessageApi;
import com.jung.message.api.MessageApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public MessageApi configMessageApi(){
        return new KakaoMessageApi();
    }
}
