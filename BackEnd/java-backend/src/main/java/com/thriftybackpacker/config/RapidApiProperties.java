package com.thriftybackpacker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds RapidAPI configuration from application.properties.
 * rapidapi.key  = ${RAPIDAPI_KEY}  — never hardcoded
 * rapidapi.base-host = booking-com.p.rapidapi.com
 */
@Configuration
@ConfigurationProperties(prefix = "rapidapi")
@Getter
@Setter
public class RapidApiProperties {

    private String key;
    private String baseHost;
    private int timeoutMs = 30000;
}
