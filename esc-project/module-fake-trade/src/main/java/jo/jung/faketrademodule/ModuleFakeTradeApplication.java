package jo.jung.faketrademodule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {
        "jo.jung.faketrademodule",
        "jo.jung.logic",
        "jo.jung.domain",
        "jo.jung.common"
})
@EnableScheduling
public class ModuleFakeTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModuleFakeTradeApplication.class, args);
    }

}