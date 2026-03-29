package com.jung.app.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.jung.common.log.*")
@ComponentScan("com.jung.app.webapp.*")
public class MonitorWebApplication {
    public static void main(String[] args) {

        SpringApplication.run(MonitorWebApplication.class, args);
    }
}
