package com.guilherme.desafiointer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Propriedades de configuração para cache da aplicação.
 *
 * Permite configuração externa via application.properties usando o prefixo 'cache'.
 * Cada cache pode ter configurações específicas de tempo de expiração,
 * capacidade inicial e tamanho máximo.
 *
 * Exemplo de configuração:
 * cache.config.cotacoes.expire-after-write=PT2H
 * cache.config.cotacoes.initial-capacity=50
 * cache.config.cotacoes.maximum-size=500
 *
 * Valores padrão:
 * - Expiração: 1 hora
 * - Capacidade inicial: 100 entradas
 * - Tamanho máximo: 1000 entradas
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
@Validated
public class CacheProperties {
    private Map<String, CacheConfig> config = new HashMap<>();

    /**
     * Configuração específica para cada cache individual.
     * Define comportamento de expiração, capacidade e limites.
     */
    @Data
    public static class CacheConfig {

        /** Tempo para expiração automática das entradas após escrita */
        private Duration expireAfterWrite = Duration.ofHours(1);

        /** Capacidade inicial do cache para otimizar performance */
        private int initialCapacity = 100;

        /** Número máximo de entradas permitidas no cache */
        private int maximumSize = 1000;
    }
}