package com.guilherme.desafiointer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CotacaoService {

    private final RestTemplate restTemplate;

    @Value("${api.cotacao.url}")
    private String API_URL;

    @Value("${cotacao.default:5.00}")
    private String defaultCotacao;

    public BigDecimal obterCotacao(String moeda) {
        try {
            String url = API_URL + "/" + moeda;
            CotacaoResponse response = restTemplate.getForObject(url, CotacaoResponse.class);
            if (response != null && response.getCotacao() != null) {
                return new BigDecimal(response.getCotacao())
                        .setScale(4, RoundingMode.HALF_UP);
            }
            return getDefaultCotacao();
        } catch (Exception e) {
            return getDefaultCotacao();
        }
    }

    public BigDecimal obterCotacaoAtual() {
        try {
            CotacaoResponse response = restTemplate.getForObject(API_URL, CotacaoResponse.class);
            if (response != null && response.getUsdbrl() != null) {
                return new BigDecimal(response.getUsdbrl().getBid())
                        .setScale(4, RoundingMode.HALF_UP);
            }
            return getDefaultCotacao();
        } catch (Exception e) {
            return getDefaultCotacao();
        }
    }

    private BigDecimal getDefaultCotacao() {
        return new BigDecimal(defaultCotacao).setScale(4, RoundingMode.HALF_UP);
    }

    @lombok.Data
    private static class CotacaoResponse {
        private USDBRL usdbrl;
        private String cotacao;

        @lombok.Data
        public static class USDBRL {
            private String bid;
        }
    }
}