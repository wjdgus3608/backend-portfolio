package com.jung.app.realapp;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
@ComponentScan("com.jung.common.*")
@ComponentScan("com.jung.client.*")
@ComponentScan("com.jung.app.realapp.*")
public class RealApplication {
    public static void main(String[] args) {

        SpringApplication.run(RealApplication.class, args);
    }
}
