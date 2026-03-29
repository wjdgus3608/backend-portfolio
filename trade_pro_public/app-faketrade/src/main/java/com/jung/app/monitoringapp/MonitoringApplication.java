package com.jung.app.monitoringapp;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
@ComponentScan("com.jung.common.*")
@ComponentScan("com.jung.app.monitoringapp.*")
public class MonitoringApplication {
    public static void main(String[] args) {

        SpringApplication.run(MonitoringApplication.class, args);
    }
}
