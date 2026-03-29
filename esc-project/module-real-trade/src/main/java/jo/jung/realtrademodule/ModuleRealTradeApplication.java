package jo.jung.realtrademodule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {
        "jo.jung.realtrademodule",
        "jo.jung.logic",
        "jo.jung.domain",
        "jo.jung.common"
})
@EnableScheduling
public class ModuleRealTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModuleRealTradeApplication.class, args);
    }

}