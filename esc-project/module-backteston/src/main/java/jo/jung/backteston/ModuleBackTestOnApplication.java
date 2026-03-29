package jo.jung.backteston;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(basePackages = {
        "jo.jung.backteston",
        "jo.jung.domain",
        "jo.jung.common"
})
@EnableScheduling
@EnableJpaAuditing
@EnableTransactionManagement
public class ModuleBackTestOnApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModuleBackTestOnApplication.class, args);
    }

}