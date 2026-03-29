package com.jung.app.monitoringapp;

import com.jung.app.monitoringapp.service.bot.NormalBot;
import com.jung.app.monitoringapp.service.bot.buy.*;
import com.jung.app.monitoringapp.service.bot.sell.*;
import com.jung.app.monitoringapp.service.box.FakeMyBox;
import com.jung.app.monitoringapp.service.client.FakeApiClient;
import com.jung.app.monitoringapp.service.strategy.CommonStrategy;
import com.jung.app.monitoringapp.service.strategy.ShortTermStrategy;
import com.jung.client.api.ApiClient;
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
        FakeMyBox fb = new FakeMyBox();
        fb.setMoney(12000000);
        return fb;
    }

    @Bean
    @Primary
    public ApiClient configApiClient(){
        return new FakeApiClient(configMyBox());
    }

    @Bean
    @Primary
    public CommonBuyBot configBuyBot(){
        //test2
//        return new RsiBuyBot(configMyBox(),configApiClient());
        //test1
        return new MACDBuyBot(configMyBox(),configApiClient());
//        return new ShortTimeBuyBot(configMyBox(),configApiClient());
//        return new GraphScanBuyBot(configMyBox(),configApiClient());
//        return new TradeAmountUpBuyBot(configMyBox(),configApiClient());
    }

    @Bean
    public PlusBuyBot configPlusBuyBot(){
        return new PlusBuyBot(configMyBox(),configApiClient());
    }

    @Bean
    @Primary
    public NormalBot configNormalBot(){
        return new NormalBot(configMyBox(),configApiClient());
    }


    @Bean
    @Primary
    public EarnSellBot configEarnSellBot(){
        //test1,test2
        return new BandUpSellBot(configMyBox(),configApiClient());
//        return new WatchDownSellBot(configMyBox(),configApiClient());
    }

    @Bean
    @Primary
    public LostSellBot configLostSellBot(){
        //test2
//        return new XPercentDownWithPlusSellBot(configMyBox(),configApiClient(),configPlusBuyBot());
        //test1
//        return new XPercentDownSellBot(configMyBox(),configApiClient());
        return new XPercentDownPlusBuyBot(configMyBox(),configApiClient(),configPlusBuyBot());
    }

    @Bean
    @Primary
    public ExitSellBot configExitSellBot(){
        //test1,test2
//        return new OneDayExitSellBot(configMyBox(),configApiClient());
        //이득만 정리
        return new OneDayExitWithGainSellBot(configMyBox(),configApiClient());
//        return new ShortTimeExitSellBot(configMyBox(),configApiClient());
    }

    @Bean
    @Primary
    public CommonStrategy configStrategy(){
        //혼합용
//        return new ShortTermStrategy(configBuyBot(),configEarnSellBot(),configLostSellBot(),configExitSellBot(),configMACDBuyBot());
        return new ShortTermStrategy(configBuyBot(),configEarnSellBot(),configLostSellBot(),configExitSellBot());
    }

/*    @Bean
    @Primary
    public MACDBuyBot configMACDBuyBot(){
        return new MACDBuyBot(configMyBox(),configApiClient());
    }*/
}
