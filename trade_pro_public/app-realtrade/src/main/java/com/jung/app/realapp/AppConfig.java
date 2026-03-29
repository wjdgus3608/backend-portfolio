package com.jung.app.realapp;


import com.jung.app.realapp.service.bot.NormalBot;
import com.jung.app.realapp.service.bot.buy.CommonBuyBot;
import com.jung.app.realapp.service.bot.buy.MACDBuyBot;
import com.jung.app.realapp.service.bot.buy.PlusBuyBot;
import com.jung.app.realapp.service.bot.buy.RsiBuyBot;
import com.jung.app.realapp.service.bot.init.InitBot;
import com.jung.app.realapp.service.bot.sell.*;
import com.jung.app.realapp.service.box.RealMyBox;
import com.jung.app.realapp.service.strategy.CommonStrategy;
import com.jung.app.realapp.service.strategy.ShortTermStrategy;
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
    public CommonBuyBot configBuyBot(){
        return new MACDBuyBot(configMyBox(),configApiClient());
//        return new RsiBuyBot(configMyBox(),configApiClient());
//        return new BandBuyBot(configMyBox(),configApiClient());
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
        return new BandUpSellBot(configMyBox(),configApiClient());
        //return new XPercentUpWithLostSmallerSellBot(configMyBox(),configApiClient());
    }

    @Bean
    @Primary
    public LostSellBot configLostSellBot(){
//        return new XPercentDownSellBot(configMyBox(),configApiClient());
//        return new XPercentDownSellBot(configMyBox(),configApiClient(),configPlusBuyBot());
        return new XPercentDownPlusBuyBot(configMyBox(),configApiClient(),configPlusBuyBot());
    }

    @Bean
    @Primary
    public ExitSellBot configExitSellBot(){
//        return new ShortTimeExitSellBot(configMyBox(),configApiClient());
//        return new OneDayExitSellBot(configMyBox(),configApiClient());
        return new OneDayExitWithGainSellBot(configMyBox(),configApiClient());
    }

    @Bean
    @Primary
    public InitBot configInitBot(){
        return new InitBot(configMyBox());
    }


    @Bean
    @Primary
    public CommonStrategy configStrategy(){
        return new ShortTermStrategy(configBuyBot(),configEarnSellBot(),configLostSellBot(),configExitSellBot(),configInitBot());
    }
}
