package com.guilherme.desafiointer.controller;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheAdminController {

    private final CacheManager cacheManager;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        cacheManager.getCacheNames().forEach(cacheName -> {
            CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (caffeineCache != null) {
                CacheStats cacheStats = caffeineCache.getNativeCache().stats();
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("hitRate", cacheStats.hitRate());
                cacheInfo.put("missRate", cacheStats.missRate());
                cacheInfo.put("loadSuccessCount", cacheStats.loadSuccessCount());
                cacheInfo.put("loadFailureCount", cacheStats.loadFailureCount());
                cacheInfo.put("totalLoadTime", cacheStats.totalLoadTime());
                cacheInfo.put("evictionCount", cacheStats.evictionCount());
                stats.put(cacheName, cacheInfo);
            }
        });

        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCache(
            @RequestParam(required = false) String cacheName) {

        Map<String, String> result = new HashMap<>();

        if (cacheName != null) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                result.put("message", "Cache '" + cacheName + "' limpo com sucesso");
            } else {
                result.put("message", "Cache '" + cacheName + "' não encontrado");
            }
        } else {
            cacheManager.getCacheNames().forEach(name ->
                    cacheManager.getCache(name).clear());
            result.put("message", "Todos os caches foram limpos");
        }

        return ResponseEntity.ok(result);
    }
}