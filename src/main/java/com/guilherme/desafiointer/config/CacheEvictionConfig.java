package com.guilherme.desafiointer.config;

import com.guilherme.desafiointer.config.constants.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CacheEvictionConfig {

    private final CacheManager cacheManager;

    @Scheduled(cron = "${cache.eviction.cron:0 0 * * * *}")
    public void evictAllCaches() {
        log.debug("Iniciando limpeza programada de todos os caches");
        cacheManager.getCacheNames().stream()
                .forEach(cacheName -> {
                    log.debug("Limpando cache: {}", cacheName);
                    cacheManager.getCache(cacheName).clear();
                });
    }

    @Scheduled(cron = "${cache.cotacoes.eviction.cron:0 0 0 * * *}")
    public void evictCotacoesCache() {
        cacheManager.getCache(AppConstants.CACHE_COTACOES).clear();
    }
}