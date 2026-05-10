package com.thriftybackpacker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class ThriftyBackpackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThriftyBackpackerApplication.class, args);
    }

    /** Shared RestTemplate used by RapidApiClient for all external HTTP calls. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
