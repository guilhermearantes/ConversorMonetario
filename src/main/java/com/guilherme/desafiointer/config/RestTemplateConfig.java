package com.guilherme.desafiointer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;

/**
 * Configuração do cliente HTTP RestTemplate para chamadas de APIs externas.
 *
 * Define timeouts otimizados para chamadas ao Banco Central do Brasil
 * e outras APIs de cotação. Os valores são configurados para equilibrar
 * performance e confiabilidade.
 *
 * Configurações aplicadas:
 * - Connection timeout: 5 segundos
 * - Read timeout: 5 segundos
 *
 * Utilizado principalmente pelo CotacaoService para obter cotações
 * oficiais USD/BRL da API do Banco Central.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Cria uma instância configurada do RestTemplate com timeouts otimizados.
     *
     * @param builder RestTemplateBuilder fornecido pelo Spring Boot
     * @return RestTemplate configurado e pronto para uso
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5)) // Define o tempo máximo de conexão
                .setReadTimeout(Duration.ofSeconds(5))   // Define o tempo máximo de leitura
                .build();
    }
}