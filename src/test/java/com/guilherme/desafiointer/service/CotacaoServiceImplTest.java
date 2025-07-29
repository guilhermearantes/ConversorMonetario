package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.config.TestConfig;
import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.dto.PTAXResponse;
import com.guilherme.desafiointer.service.impl.CotacaoServiceImpl;
import com.guilherme.desafiointer.service.testdata.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("Testes do CotacaoService")
class CotacaoServiceImplTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private CotacaoServiceImpl cotacaoService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cotacaoService, "defaultCotacaoStr", TestDataBuilder.COTACAO_PADRAO.toString());
        cacheManager.getCache(AppConstants.CACHE_COTACOES).clear();
    }

    @Test
    @DisplayName("Deve usar cache na segunda chamada")
    void deveUsarCacheNaSegundaChamada() {
        // given
        when(restTemplate.getForObject(anyString(), eq(PTAXResponse.class)))
                .thenReturn(TestDataBuilder.criarPTAXResponsePadrao());

        // when
        BigDecimal primeiraChamada = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);
        BigDecimal segundaChamada = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

        // then
        assertAll("Validação do cache",
                () -> assertEquals(primeiraChamada, segundaChamada, "As cotações devem ser iguais"),
                () -> verify(restTemplate, times(1)).getForObject(anyString(), eq(PTAXResponse.class)),
                () -> assertEquals(
                        TestDataBuilder.COTACAO_PADRAO.setScale(2, RoundingMode.HALF_UP),
                        primeiraChamada.setScale(2, RoundingMode.HALF_UP),
                        "A cotação deve ter o valor esperado"
                )
        );
    }

    @Test
    @DisplayName("Deve obter cotação com sucesso quando API retornar resposta válida")
    void deveObterCotacaoComSucesso() {
        // given
        when(restTemplate.getForObject(anyString(), eq(PTAXResponse.class)))
                .thenReturn(TestDataBuilder.criarPTAXResponsePadrao());

        // when
        BigDecimal cotacao = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

        // then
        assertEquals(
                TestDataBuilder.COTACAO_PADRAO.setScale(2, RoundingMode.HALF_UP),
                cotacao.setScale(2, RoundingMode.HALF_UP)
        );
    }

    @Test
    @DisplayName("Deve retornar cotação padrão quando API falhar")
    void deveRetornarCotacaoPadraoQuandoApiFalhar() {
        // given
        when(restTemplate.getForObject(any(String.class), eq(PTAXResponse.class)))
                .thenThrow(new RestClientException("API error"));

        // when
        BigDecimal cotacao = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

        // then
        assertEquals(
                TestDataBuilder.COTACAO_PADRAO.setScale(2, RoundingMode.HALF_UP),
                cotacao.setScale(2, RoundingMode.HALF_UP)
        );
    }

    @Test
    @DisplayName("Deve falhar ao buscar moeda não suportada")
    void deveFalharComMoedaNaoSuportada() {
        // when/then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cotacaoService.obterCotacao("EUR")
        );

        assertEquals(
                String.format("Moeda EUR não é suportada. Moedas suportadas: %s",
                        AppConstants.MOEDAS_SUPORTADAS),
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve retornar cotação padrão quando resposta da API for nula")
    void deveRetornarCotacaoPadraoQuandoRespostaNula() {
        // given
        when(restTemplate.getForObject(any(String.class), eq(PTAXResponse.class)))
                .thenReturn(null);

        // when
        BigDecimal cotacao = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

        // then
        assertEquals(
                TestDataBuilder.COTACAO_PADRAO.setScale(2, RoundingMode.HALF_UP),
                cotacao.setScale(2, RoundingMode.HALF_UP)
        );
    }


    @Test
    @DisplayName("Deve retornar cotação padrão quando lista de valores for vazia")
    void deveRetornarCotacaoPadraoQuandoListaVazia() {
        // given
        when(restTemplate.getForObject(any(String.class), eq(PTAXResponse.class)))
                .thenReturn(TestDataBuilder.criarPTAXResponseVazio());

        // when
        BigDecimal cotacao = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

        // then
        assertEquals(
                TestDataBuilder.COTACAO_PADRAO.setScale(2, RoundingMode.HALF_UP),
                cotacao.setScale(2, RoundingMode.HALF_UP)
        );
    }

}