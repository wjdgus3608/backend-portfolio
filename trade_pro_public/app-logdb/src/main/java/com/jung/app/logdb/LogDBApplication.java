package com.jung.app.logdb;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EntityScan("com.jung.domain.*")
//@EnableJpaRepositories("com.jung.domain.*")
@EnableBatchProcessing
@EnableScheduling
@ComponentScan("com.jung.common.*")
@ComponentScan("com.jung.app.logdb.*")
public class LogDBApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogDBApplication.class, args);
    }

}
