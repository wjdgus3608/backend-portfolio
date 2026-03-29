package com.jung.batch.config;

import com.jung.batch.api.ApiClientService;
import com.jung.batch.api.BinanceApiService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AppConfig {

    @Bean
    @Primary
    public ApiClientService apiClientService(){
        return new BinanceApiService();
    }

}
