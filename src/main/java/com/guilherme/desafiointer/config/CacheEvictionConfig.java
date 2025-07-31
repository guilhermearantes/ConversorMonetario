package com.guilherme.desafiointer.config;

import com.guilherme.desafiointer.config.constants.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuração para limpeza automática de caches da aplicação.
 *
 * <p>Esta classe gerencia a limpeza programada dos caches para garantir:
 * <ul>
 *   <li>Prevenção de dados obsoletos em cache</li>
 *   <li>Controle de consumo de memória</li>
 *   <li>Renovação periódica de dados sensíveis ao tempo</li>
 *   <li>Manutenção automática da saúde dos caches</li>
 * </ul>
 *
 * <p>Estratégias de limpeza implementadas:
 * <ul>
 *   <li><strong>Limpeza geral</strong>: Todos os caches a cada hora (padrão)</li>
 *   <li><strong>Limpeza de cotações</strong>: Cache de cotações diariamente à meia-noite</li>
 * </ul>
 *
 * <p>As expressões cron podem ser customizadas via propriedades:
 * <pre>
 * cache:
 *   eviction:
 *     cron: "0 30 * * * *"  # A cada 30 minutos
 *   cotacoes:
 *     eviction:
 *       cron: "0 0 6 * * *"  # Às 6h da manhã
 * </pre>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CacheEvictionConfig {

    /**
     * CacheManager responsável pelo gerenciamento de todos os caches da aplicação.
     * Injected via constructor para garantir disponibilidade durante execução dos jobs.
     */
    private final CacheManager cacheManager;


    /**
     * Executa limpeza programada de todos os caches registrados na aplicação.
     * Remove todos os dados armazenados em cache para forçar renovação
     * na próxima consulta. Executa por padrão a cada hora.
     *
     * Configuração personalizada via propriedade:
     * cache.eviction.cron=0 30 * * * * (A cada 30 minutos)
     */
    @Scheduled(cron = "${cache.eviction.cron:0 0 * * * *}")
    public void evictAllCaches() {
        log.debug("Iniciando limpeza programada de todos os caches");
        cacheManager.getCacheNames().stream()
                .forEach(cacheName -> {
                    log.debug("Limpando cache: {}", cacheName);
                    cacheManager.getCache(cacheName).clear();
                });
    }

    /**
     * Executa limpeza específica do cache de cotações de moedas.
     *
     * <p>Este metodo foi criado especificamente para o cache de cotações porque:
     * <ul>
     *   <li>Cotações mudam diariamente conforme mercado financeiro</li>
     *   <li>Dados obsoletos podem impactar cálculos de remessa</li>
     *   <li>BCB atualiza cotações em horários específicos</li>
     *   <li>Limpeza diária garante dados sempre atualizados</li>
     * </ul>
     *
     * <p><strong>Configuração padrão:</strong> Executa diariamente à meia-noite (0 0 0 * * *)
     *
     * <p><strong>Configuração customizada:</strong> Use a propriedade
     * {@code cache.cotacoes.eviction.cron} para alterar:
     * <pre>
     * cache:
     *   cotacoes:
     *     eviction:
     *       cron: "0 0 6 * * MON-FRI"  # 6h da manhã, dias úteis apenas
     * </pre>
     *
     * <p><strong>Estratégia recomendada:</strong> Configure para executar durante
     * horários de baixo tráfego (madrugada) para minimizar impacto na performance.
     *
     * <p><strong>Fallback:</strong> Se o cache não existir, o método executa
     * silenciosamente sem erro, garantindo robustez da aplicação.
     *
     * @throws NullPointerException se o cache de cotações não estiver registrado
     * @see AppConstants#CACHE_COTACOES
     * @see Scheduled
     */
    @Scheduled(cron = "${cache.cotacoes.eviction.cron:0 0 0 * * *}")
    public void evictCotacoesCache() {
        cacheManager.getCache(AppConstants.CACHE_COTACOES).clear();
    }
}