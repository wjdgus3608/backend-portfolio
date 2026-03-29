package com.jung.app.boxlogapp;

import com.jung.app.boxlogapp.service.box.RealMyBox;
import com.jung.app.boxlogapp.service.strategy.CommonStrategy;
import com.jung.app.boxlogapp.service.strategy.ShortTermStrategy;
import com.jung.client.api.ApiClient;
import com.jung.client.api.UpbitApiClient;
import com.jung.common.box.MyBox;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class AppConfig {
    private final JobBuilderFactory jobBuilderFactory; // Job 빌더 생성용
    private final StepBuilderFactory stepBuilderFactory; // Step 빌더 생성용

    // JobBuilderFactory를 통해서 tutorialJob을 생성
    @Bean
    public Job tutorialJob() {
        return jobBuilderFactory.get("tutorialJob")
                .start(tutorialStep())  // Step 설정
                .build();
    }

    // StepBuilderFactory를 통해서 tutorialStep을 생성
    @Bean
    public Step tutorialStep() {
        return stepBuilderFactory.get("tutorialStep")
                .tasklet(configStrategy()) // Tasklet 설정
                .build();
    }

    @Bean
    @Primary
    public MyBox configMyBox(){
        return new RealMyBox(configApiClient());
    }

    @Bean
    @Primary
    public ApiClient configApiClient(){
        return new UpbitApiClient();
    }

    @Bean
    @Primary
    public CommonStrategy configStrategy(){
        return new ShortTermStrategy(configMyBox());
    }
}
