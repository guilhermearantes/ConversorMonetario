package com.guilherme.desafiointer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.guilherme.desafiointer.config.constants.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private static final List<String> CACHE_NAMES = Arrays.asList(
            AppConstants.CACHE_COTACOES,
            AppConstants.CACHE_HISTORICO,
            AppConstants.CACHE_TOTAIS
    );

    private final CacheProperties cacheProperties;

    @PostConstruct
    public void logCacheConfiguration() {
        log.info("Cache configurado com {} caches registrados",
                cacheProperties.getConfig().size());
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(CACHE_NAMES);

        Caffeine<Object, Object> defaultCaffeine = createDefaultCaffeine();
        cacheManager.setCaffeine(defaultCaffeine);

        configurarCachesCustomizados(cacheManager);

        return cacheManager;
    }

    private Caffeine<Object, Object> createDefaultCaffeine() {
        CacheProperties.CacheConfig defaultConfig = getDefaultConfig();
        return Caffeine.newBuilder()
                .expireAfterWrite(defaultConfig.getExpireAfterWrite())
                .initialCapacity(defaultConfig.getInitialCapacity())
                .maximumSize(defaultConfig.getMaximumSize())
                .recordStats();
    }

    private void configurarCachesCustomizados(CaffeineCacheManager cacheManager) {
        CACHE_NAMES.forEach(cacheName -> {
            if (cacheProperties.getConfig().containsKey(cacheName)) {
                CacheProperties.CacheConfig config = cacheProperties.getConfig().get(cacheName);
                Caffeine<Object, Object> customCaffeine = Caffeine.newBuilder()
                        .expireAfterWrite(config.getExpireAfterWrite())
                        .initialCapacity(config.getInitialCapacity())
                        .maximumSize(config.getMaximumSize())
                        .recordStats();

                cacheManager.registerCustomCache(cacheName, customCaffeine.build());
            }
        });
    }

    private CacheProperties.CacheConfig getDefaultConfig() {
        return cacheProperties.getConfig().getOrDefault("default",
                new CacheProperties.CacheConfig());
    }
}