package com.guilherme.desafiointer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;

/**
 * Configuração para registrar o bean do RestTemplate
 * usado para chamadas HTTP externas.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5)) // Define o tempo máximo de conexão
                .setReadTimeout(Duration.ofSeconds(5))   // Define o tempo máximo de leitura
                .build();
    }
}