package com.guilherme.desafiointer.service.impl;

import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.domain.CotacaoHistorico;
import com.guilherme.desafiointer.dto.integration.bcb.PTAXResponse;
import com.guilherme.desafiointer.repository.CotacaoHistoricoRepository;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Serviço de cotações com suporte à cache e fallback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = AppConstants.CACHE_COTACOES)
public class CotacaoServiceImpl implements CotacaoServiceInterface {

    private static final String BCB_API_BASE_URL =
            "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarDia(dataCotacao=@dataCotacao)";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM-dd-yyyy");

    @Value("${cotacao.default-value:5.00}")
    private String defaultCotacaoStr;

    private final RestTemplate restTemplate;
    private final CotacaoHistoricoRepository cotacaoHistoricoRepository;

    /**
     * Obtém cotação atual da moeda com fallback para histórico.
     *
     * @param moeda Código da moeda (ex: USD)
     * @return Valor da cotação
     * @throws IllegalArgumentException se moeda não suportada
     */
    @Override
    @Transactional
    @Cacheable(
            key = "#moeda + '_' + T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ISO_DATE)",
            unless = "#result == null"
    )
    @Retry(name = "cotacaoRetry")
    public BigDecimal obterCotacao(String moeda) {
        validarMoeda(moeda);
        LocalDateTime agora = LocalDateTime.now();
        boolean isFimDeSemana = isFimDeSemana(agora.toLocalDate());

        if (isFimDeSemana) {
            return obterUltimaCotacaoUtil(moeda);
        }

        try {
            LocalDate dataConsulta = obterDataConsultaValida();
            String url = construirUrl(dataConsulta);
            log.debug("Buscando cotação na API externa para moeda: {} na data: {}", moeda, dataConsulta);

            PTAXResponse response = restTemplate.getForObject(url, PTAXResponse.class);
            BigDecimal cotacaoAtual = processarResposta(response);

            if (cotacaoAtual != null) {
                salvarHistoricoCotacao(moeda, cotacaoAtual, agora, isFimDeSemana);
                return cotacaoAtual;
            }

            return obterUltimaCotacaoUtil(moeda);
        } catch (Exception e) {
            log.error("Erro ao obter cotação da API do BCB: {}", e.getMessage());
            return obterUltimaCotacaoUtil(moeda);
        }
    }

    /**
     * Limpa cache de cotações.
     * Útil para forçar atualização.
     */
    @CacheEvict(allEntries = true)
    public void limparCache() {
        log.info("Cache de cotações limpo");
    }

    /**
     * Busca última cotação em dia útil.
     * Fallback quando API indisponível.
     */
    private BigDecimal obterUltimaCotacaoUtil(String moeda) {
        return cotacaoHistoricoRepository.findUltimaCotacaoUtil(moeda)
                .map(CotacaoHistorico::getValor)
                .orElseGet(() -> {
                    log.warn("Nenhuma cotação útil encontrada. Usando valor padrão.");
                    return getDefaultCotacao();
                });
    }

    /**
     * Salva nova cotação no histórico.
     * Registra data/hora e flag de fim de semana.
     */
    private void salvarHistoricoCotacao(String moeda, BigDecimal valor,
                                        LocalDateTime dataHora, boolean isFimDeSemana) {
        try {
            CotacaoHistorico historico = CotacaoHistorico.builder()
                    .moeda(moeda)
                    .valor(valor)
                    .dataHora(dataHora)
                    .isFimDeSemana(isFimDeSemana)
                    .ultimaAtualizacao(LocalDateTime.now())
                    .build();

            cotacaoHistoricoRepository.save(historico);
            log.debug("Cotação salva no histórico: {}", historico);
        } catch (Exception e) {
            log.error("Erro ao salvar histórico de cotação: {}", e.getMessage());
            throw new RuntimeException("Falha ao salvar histórico de cotação", e);
        }
    }

    /**
     * Verifica se data é fim de semana.
     */
    private boolean isFimDeSemana(LocalDate data) {
        return data.getDayOfWeek() == DayOfWeek.SATURDAY ||
                data.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /**
     * Ajusta data para consulta em dia útil.
     * Retrocede para sexta em fins de semana.
     */
    private LocalDate obterDataConsultaValida() {
        LocalDate hoje = LocalDate.now();
        if (hoje.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return hoje.minusDays(1);
        } else if (hoje.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return hoje.minusDays(2);
        }
        return hoje;
    }

    /**
     * Formata URL da API do BCB.
     */
    private String construirUrl(LocalDate data) {
        String dataFormatada = data.format(DATE_FORMATTER);
        return BCB_API_BASE_URL + "?@dataCotacao='" + dataFormatada + "'&$top=1&$format=json";
    }

    /**
     * Processa resposta da API e extrai cotação.
     */
    private BigDecimal processarResposta(PTAXResponse response) {
        return Optional.ofNullable(response)
                .map(PTAXResponse::getValue)
                .filter(values -> !values.isEmpty())
                .map(values -> values.get(0))
                .map(value -> new BigDecimal(value.getCotacaoCompra())
                        .setScale(AppConstants.DECIMAL_SCALE, RoundingMode.HALF_UP))
                .orElse(null);
    }

    /**
     * Valida se moeda é suportada.
     * @throws IllegalArgumentException se inválida
     */
    private void validarMoeda(String moeda) {
        if (moeda == null || !AppConstants.MOEDAS_SUPORTADAS.contains(moeda.toUpperCase())) {
            throw new IllegalArgumentException(
                    String.format("Moeda %s não é suportada. Moedas suportadas: %s",
                            moeda, AppConstants.MOEDAS_SUPORTADAS)
            );
        }
    }

    /**
     * Retorna cotação padrão configurada.
     * Último recurso de fallback.
     */
    private BigDecimal getDefaultCotacao() {
        return new BigDecimal(defaultCotacaoStr)
                .setScale(AppConstants.DECIMAL_SCALE, RoundingMode.HALF_UP);
    }
}