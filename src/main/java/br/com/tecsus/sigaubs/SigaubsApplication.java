package br.com.tecsus.sigaubs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry
@EnableScheduling
@EnableCaching
@SpringBootApplication
public class SigaubsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SigaubsApplication.class, args);
    }
}
