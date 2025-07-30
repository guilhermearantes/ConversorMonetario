package com.guilherme.desafiointer.config;

import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@TestConfiguration
@EnableCaching
public class TestConfig {

    @Bean
    @Primary
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager(
                AppConstants.CACHE_COTACOES,
                AppConstants.CACHE_HISTORICO,
                AppConstants.CACHE_TOTAIS
        );
    }

    @Bean
    @Primary
    @Profile("integration-test")
    public CotacaoServiceInterface integrationTestCotacaoService() {
        return moeda -> new BigDecimal("5.00"); // Cotação fixa para testes
    }


    @Bean
    @Primary
    @Profile("integration-test")
    public RestTemplate realRestTemplate() {
        return new RestTemplate();
    }
}