package com.guilherme.desafiointer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
@Validated
public class CacheProperties {
    private Map<String, CacheConfig> config = new HashMap<>();

    @Data
    public static class CacheConfig {
        private Duration expireAfterWrite = Duration.ofHours(1);
        private int initialCapacity = 100;
        private int maximumSize = 1000;
    }
}