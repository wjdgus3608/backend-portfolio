package com.jung.selladmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.jung.kisclient","com.jung.selladmin"})
@EnableJpaRepositories
@EnableScheduling
public class SellAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(SellAdminApplication.class, args);
    }
}
