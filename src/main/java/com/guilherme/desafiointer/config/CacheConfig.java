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

/**
 * Configuração do sistema de cache da aplicação usando Caffeine.
 *
 * <p>Esta classe é responsável por:
 * <ul>
 *   <li>Configurar o CacheManager com Caffeine como provedor</li>
 *   <li>Definir configurações específicas para cada tipo de cache</li>
 *   <li>Aplicar configurações de TTL, capacidade e tamanho máximo</li>
 *   <li>Habilitar estatísticas de cache para monitoramento</li>
 * </ul>
 *
 * <p>Os caches configurados incluem:
 * <ul>
 *   <li><strong>cotacoes</strong>: Cache de cotações de moedas do BCB</li>
 *   <li><strong>historico</strong>: Cache de consultas de histórico de transações</li>
 *   <li><strong>totais</strong>: Cache de totais agregados por usuário</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    /**
     * Lista dos nomes de cache registrados na aplicação.
     * Estes caches são criados automaticamente na inicialização.
     */
    private static final List<String> CACHE_NAMES = Arrays.asList(
            AppConstants.CACHE_COTACOES,
            AppConstants.CACHE_HISTORICO,
            AppConstants.CACHE_TOTAIS
    );

    /**
     * Propriedades de configuração dos caches injetadas do application.yml.
     */
    private final CacheProperties cacheProperties;

    /**
     * Método executado após a construção do bean para logging da configuração inicial.
     * Registra quantos caches personalizados foram configurados.
     */
    @PostConstruct
    public void logCacheConfiguration() {
        log.info("Cache configurado com {} caches registrados",
                cacheProperties.getConfig().size());
    }

    /**
     * Cria e configura o CacheManager principal da aplicação.
     *
     * <p>Este método:
     * <ul>
     *   <li>Instancia um CaffeineCacheManager</li>
     *   <li>Aplica configurações padrão para todos os caches</li>
     *   <li>Configura caches personalizados conforme propriedades</li>
     *   <li>Habilita coleta de estatísticas para monitoramento</li>
     * </ul>
     *
     * @return CacheManager configurado e pronto para uso
     * @see CaffeineCacheManager
     * @see #createDefaultCaffeine()
     * @see #configurarCachesCustomizados(CaffeineCacheManager)
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(CACHE_NAMES);

        Caffeine<Object, Object> defaultCaffeine = createDefaultCaffeine();
        cacheManager.setCaffeine(defaultCaffeine);

        configurarCachesCustomizados(cacheManager);

        return cacheManager;
    }

    /**
     * Cria a configuração padrão do Caffeine aplicada a todos os caches.
     *
     * <p>Configurações aplicadas:
     * <ul>
     *   <li><strong>expireAfterWrite</strong>: TTL após escrita no cache</li>
     *   <li><strong>initialCapacity</strong>: Capacidade inicial do cache</li>
     *   <li><strong>maximumSize</strong>: Tamanho máximo do cache</li>
     *   <li><strong>recordStats</strong>: Habilitação de estatísticas</li>
     * </ul>
     *
     * @return instância de Caffeine com configurações padrão
     * @see #getDefaultConfig()
     */
    private Caffeine<Object, Object> createDefaultCaffeine() {
        CacheProperties.CacheConfig defaultConfig = getDefaultConfig();
        return Caffeine.newBuilder()
                .expireAfterWrite(defaultConfig.getExpireAfterWrite())
                .initialCapacity(defaultConfig.getInitialCapacity())
                .maximumSize(defaultConfig.getMaximumSize())
                .recordStats();
    }

    /**
     * Configura caches personalizados com configurações específicas por nome.
     *
     * <p>Para cada cache na lista CACHE_NAMES, verifica se existe uma configuração
     * personalizada no CacheProperties. Se existir, cria um cache customizado
     * com os parâmetros específicos definidos no application.yml.
     *
     * <p>Exemplo de configuração personalizada:
     * <pre>
     * cache:
     *   config:
     *     cotacoes:
     *       expireAfterWrite: PT30M  # 30 minutos
     *       maximumSize: 500
     * </pre>
     *
     * @param cacheManager o CacheManager onde os caches personalizados serão registrados
     * @see CacheProperties.CacheConfig
     */
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

    /**
     * Obtém a configuração padrão de cache.
     *
     * <p>Se não existir uma configuração "default" explícita no CacheProperties,
     * retorna uma nova instância com valores padrão do CacheConfig.
     *
     * @return configuração padrão do cache
     * @see CacheProperties.CacheConfig
     */
    private CacheProperties.CacheConfig getDefaultConfig() {
        return cacheProperties.getConfig().getOrDefault("default",
                new CacheProperties.CacheConfig());
    }
}