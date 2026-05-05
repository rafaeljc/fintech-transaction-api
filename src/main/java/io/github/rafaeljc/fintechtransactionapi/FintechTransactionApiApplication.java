package io.github.rafaeljc.fintechtransactionapi;

import io.github.rafaeljc.fintechtransactionapi.config.StatisticsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StatisticsProperties.class)
public class FintechTransactionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FintechTransactionApiApplication.class, args);
    }

}
