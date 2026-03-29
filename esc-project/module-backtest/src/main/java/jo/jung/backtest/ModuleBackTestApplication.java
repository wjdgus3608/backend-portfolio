package jo.jung.backtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(basePackages = {
        "jo.jung.backtest",
        "jo.jung.domain",
        "jo.jung.common"
})
@EnableScheduling
@EnableJpaAuditing
@EnableTransactionManagement
public class ModuleBackTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModuleBackTestApplication.class, args);
    }

}