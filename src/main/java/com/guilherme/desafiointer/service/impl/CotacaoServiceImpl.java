package com.guilherme.desafiointer.service.impl;

import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.dto.PTAXResponse;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

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

    @Override
    @Cacheable(
            key = "#moeda + '_' + T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ISO_DATE)",
            unless = "#result == null"
    )
    public BigDecimal obterCotacao(String moeda) {
        validarMoeda(moeda);

        try {
            LocalDate dataConsulta = obterDataConsultaValida();
            String url = construirUrl(dataConsulta);

            log.debug("Buscando cotação na API externa para moeda: {} na data: {}", moeda, dataConsulta);
            PTAXResponse response = restTemplate.getForObject(url, PTAXResponse.class);

            return Optional.ofNullable(processarResposta(response))
                    .orElseGet(() -> {
                        log.info("Usando cotação padrão devido à resposta inválida da API");
                        return getDefaultCotacao();
                    });

        } catch (Exception e) {
            log.warn("Falha ao obter cotação da API externa. Erro: {}. Usando valor padrão.",
                    e.getMessage());
            return getDefaultCotacao();
        }
    }

    // Adicione este metodo para limpar o cache quando necessário
    @CacheEvict(allEntries = true)
    public void limparCache() {
        log.info("Cache de cotações limpo");
    }


    private LocalDate obterDataConsultaValida() {
        LocalDate hoje = LocalDate.now();

        // Se for fim de semana, retorna a última cotação de sexta-feira
        if (hoje.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return hoje.minusDays(1);
        } else if (hoje.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return hoje.minusDays(2);
        }

        return hoje;
    }

    private String construirUrl(LocalDate data) {
        String dataFormatada = data.format(DATE_FORMATTER);
        return BCB_API_BASE_URL + "?@dataCotacao='" + dataFormatada + "'&$top=1&$format=json";
    }

    private BigDecimal processarResposta(PTAXResponse response) {
        if (response == null || response.getValue() == null || response.getValue().isEmpty()) {
            log.warn("Resposta da API externa vazia ou inválida.");
            return null;
        }

        return response.getValue().stream()
                .findFirst()
                .map(value -> new BigDecimal(value.getCotacaoCompra())
                        .setScale(AppConstants.DECIMAL_SCALE, RoundingMode.HALF_UP))
                .orElse(null);
    }

    private void validarMoeda(String moeda) {
        if (moeda == null || !AppConstants.MOEDAS_SUPORTADAS.contains(moeda.toUpperCase())) {
            throw new IllegalArgumentException(
                    String.format("Moeda %s não é suportada. Moedas suportadas: %s",
                            moeda, AppConstants.MOEDAS_SUPORTADAS)
            );
        }
    }

    private BigDecimal getDefaultCotacao() {
        return new BigDecimal(defaultCotacaoStr)
                .setScale(AppConstants.DECIMAL_SCALE, RoundingMode.HALF_UP);
    }
}