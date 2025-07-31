package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.config.TestConfig;
import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.domain.CotacaoHistorico;
import com.guilherme.desafiointer.dto.integration.bcb.PTAXResponse;
import com.guilherme.desafiointer.repository.CotacaoHistoricoRepository;
import com.guilherme.desafiointer.service.impl.CotacaoServiceImpl;
import com.guilherme.desafiointer.service.testdata.TestDataBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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
import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("Testes do CotacaoService")
class CotacaoServiceImplTest {

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private CotacaoHistoricoRepository cotacaoHistoricoRepository;

    @Autowired
    private CotacaoServiceImpl cotacaoService;

    @Autowired
    private CacheManager cacheManager;

    @Captor
    private ArgumentCaptor<CotacaoHistorico> cotacaoHistoricoCaptor;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2024-02-14T10:00:00Z"), // Quarta-feira
            ZoneId.systemDefault()
    );

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cotacaoService, "defaultCotacaoStr", TestDataBuilder.COTACAO_PADRAO.toString());
        cacheManager.getCache(AppConstants.CACHE_COTACOES).clear();
    }

    @Nested
    @DisplayName("Testes de Cotação em Fins de Semana")
    class CotacaoFimDeSemanaTests {

        @Test
        @DisplayName("Deve usar última cotação útil do dia anterior no domingo")
        void deveUsarUltimaCotacaoUtilDiaAnteriorNoDomingo() {
            LocalDateTime domingo = LocalDateTime.of(2024, 2, 18, 10, 0);

            try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class)) {
                mockedStatic.when(LocalDateTime::now).thenReturn(domingo);

                CotacaoHistorico ultimaCotacao = TestDataBuilder.criarCotacaoHistorico(
                        TestDataBuilder.MOEDA_PADRAO,
                        TestDataBuilder.COTACAO_PADRAO,
                        false,
                        domingo.minusDays(2) // cotação de sexta-feira
                );

                when(cotacaoHistoricoRepository.findUltimaCotacaoUtil(anyString()))
                        .thenReturn(Optional.of(ultimaCotacao));

                BigDecimal cotacao = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

                assertAll(
                        () -> verify(restTemplate, never()).getForObject(anyString(), any()),
                        () -> assertEquals(TestDataBuilder.COTACAO_PADRAO, cotacao),
                        () -> verify(cotacaoHistoricoRepository).findUltimaCotacaoUtil(TestDataBuilder.MOEDA_PADRAO)
                );
            }
        }

        @Test
        @DisplayName("Deve manter consistência de cotações durante todo o fim de semana")
        void deveManterConsistenciaCotacoesDuranteFimDeSemana() {
            LocalDateTime sabado = LocalDateTime.of(2024, 2, 17, 10, 0);
            LocalDateTime domingo = sabado.plusDays(1);

            CotacaoHistorico ultimaCotacao = TestDataBuilder.criarCotacaoHistorico(
                    TestDataBuilder.MOEDA_PADRAO,
                    TestDataBuilder.COTACAO_PADRAO,
                    false,
                    sabado.minusDays(1) // cotação de sexta-feira
            );

            when(cotacaoHistoricoRepository.findUltimaCotacaoUtil(anyString()))
                    .thenReturn(Optional.of(ultimaCotacao));

            // Testa sábado
            try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class)) {
                mockedStatic.when(LocalDateTime::now).thenReturn(sabado);
                BigDecimal cotacaoSabado = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

                // Testa domingo
                mockedStatic.when(LocalDateTime::now).thenReturn(domingo);
                BigDecimal cotacaoDomingo = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

                assertEquals(cotacaoSabado, cotacaoDomingo, "Cotações devem ser iguais no fim de semana");
            }
        }

        @Test
        @DisplayName("Deve usar cache em finais de semana para evitar consultas repetidas")
        void deveUsarCacheEmFinaisDeSemana() {
            LocalDateTime sabado = LocalDateTime.of(2024, 2, 17, 10, 0); // Sábado
            LocalDateTime domingo = sabado.plusDays(1); // Domingo

            CotacaoHistorico ultimaCotacao = TestDataBuilder.criarCotacaoHistorico(
                    TestDataBuilder.MOEDA_PADRAO,
                    TestDataBuilder.COTACAO_PADRAO,
                    false
            );

            when(cotacaoHistoricoRepository.findUltimaCotacaoUtil(anyString()))
                    .thenReturn(Optional.of(ultimaCotacao));

            // Testando cache no sábado
            try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class)) {
                mockedStatic.when(LocalDateTime::now).thenReturn(sabado);
                BigDecimal cotacaoSabado = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

                // Verifica que não busca novamente o histórico no domingo, usando o cache
                mockedStatic.when(LocalDateTime::now).thenReturn(domingo);
                BigDecimal cotacaoDomingo = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

                // Assertivas
                assertAll(
                        () -> assertEquals(cotacaoSabado, cotacaoDomingo, "Cotação deve ser reutilizada do cache"),
                        () -> verify(cotacaoHistoricoRepository, times(1))
                                .findUltimaCotacaoUtil(anyString())
                );
            }
        }

        @Test
        @DisplayName("Deve retornar cotação padrão após falhas continuadas na API e ausência de histórico")
        void deveRetornarCotacaoPadraoAposFalhasContinuadas() {
            String moeda = TestDataBuilder.MOEDA_PADRAO;

            // Simula falha contínua da API
            when(restTemplate.getForObject(anyString(), any()))
                    .thenThrow(new RestClientException("Erro na API"));

            // Simula ausência de histórico
            when(cotacaoHistoricoRepository.findUltimaCotacaoUtil(anyString()))
                    .thenReturn(Optional.empty());

            // Quando
            BigDecimal cotacao = cotacaoService.obterCotacao(moeda);

            // Então
            assertAll(
                    () -> verify(restTemplate, times(1)).getForObject(anyString(), any()),
                    () -> verify(cotacaoHistoricoRepository, times(1)).findUltimaCotacaoUtil(moeda),
                    () -> assertEquals(TestDataBuilder.COTACAO_PADRAO, cotacao, "Deve usar cotação padrão após falhas")
            );
        }

        @Test
        @DisplayName("Deve falhar ao consultar cotação de moeda inválida durante final de semana")
        void deveFalharParaMoedaInvalidaDuranteFimDeSemana() {
            LocalDateTime sabado = LocalDateTime.of(2024, 2, 17, 10, 0);

            try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class)) {
                mockedStatic.when(LocalDateTime::now).thenReturn(sabado);

                assertThrows(IllegalArgumentException.class,
                        () -> cotacaoService.obterCotacao("INVALID"),
                        "Moeda não suportada deve lançar exceção mesmo em finais de semana");
            }
        }
    }

    @Nested
    @DisplayName("Testes de Cotação em Dias Úteis")
    class CotacaoDiaUtilTests {

        @Test
        @DisplayName("Deve obter e salvar nova cotação em dia útil")
        void deveObterESalvarNovaCotacaoEmDiaUtil() {
            // given
            when(restTemplate.getForObject(anyString(), eq(PTAXResponse.class)))
                    .thenReturn(TestDataBuilder.criarPTAXResponsePadrao());
            when(cotacaoHistoricoRepository.save(any())).thenReturn(new CotacaoHistorico());

            // when
            BigDecimal cotacao = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

            // then
            verify(cotacaoHistoricoRepository).save(cotacaoHistoricoCaptor.capture());
            CotacaoHistorico historico = cotacaoHistoricoCaptor.getValue();

            assertAll(
                    () -> assertEquals(TestDataBuilder.MOEDA_PADRAO, historico.getMoeda()),
                    () -> assertEquals(TestDataBuilder.COTACAO_PADRAO, historico.getValor()),
                    () -> assertFalse(historico.isFimDeSemana()),
                    () -> assertNotNull(historico.getUltimaAtualizacao())
            );
        }

        @Test
        @DisplayName("Deve usar cache na segunda chamada em dia útil")
        void deveUsarCacheNaSegundaChamadaEmDiaUtil() {
            // given
            when(restTemplate.getForObject(anyString(), eq(PTAXResponse.class)))
                    .thenReturn(TestDataBuilder.criarPTAXResponsePadrao());

            // when
            cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);
            cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

            // then
            verify(restTemplate, times(1)).getForObject(anyString(), any());
            verify(cotacaoHistoricoRepository, times(1)).save(any());
        }
    }

    @Test
    @DisplayName("Deve usar última cotação útil no sábado")
    void deveUsarUltimaCotacaoUtilNoSabado() {
        // given
        LocalDateTime sabado = LocalDateTime.of(2024, 2, 17, 10, 0); // Sábado

        try (MockedStatic<LocalDateTime> mockedStatic = Mockito.mockStatic(LocalDateTime.class)) {
            mockedStatic.when(LocalDateTime::now).thenReturn(sabado);

            CotacaoHistorico ultimaCotacao = TestDataBuilder.criarCotacaoHistorico(
                    TestDataBuilder.MOEDA_PADRAO,
                    TestDataBuilder.COTACAO_PADRAO,
                    false
            );

            when(cotacaoHistoricoRepository.findUltimaCotacaoUtil(TestDataBuilder.MOEDA_PADRAO))
                    .thenReturn(Optional.of(ultimaCotacao));

            // when
            BigDecimal cotacao = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

            // then
            verify(restTemplate, never()).getForObject(anyString(), any());
            verify(cotacaoHistoricoRepository).findUltimaCotacaoUtil(TestDataBuilder.MOEDA_PADRAO);
            assertEquals(TestDataBuilder.COTACAO_PADRAO, cotacao);
        }
    }

    @Nested
    @DisplayName("Testes de Fallback")
    class FallbackTests {

        @Test
        @DisplayName("Deve usar cotação padrão quando não houver histórico")
        void deveUsarCotacaoPadraoQuandoNaoHouverHistorico() {
            // given
            when(cotacaoHistoricoRepository.findUltimaCotacaoUtil(anyString()))
                    .thenReturn(Optional.empty());
            when(restTemplate.getForObject(anyString(), any()))
                    .thenThrow(new RestClientException("Erro na API"));

            // when
            BigDecimal cotacao = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

            // then
            assertEquals(TestDataBuilder.COTACAO_PADRAO, cotacao);
        }

        @Test
        @DisplayName("Deve usar última cotação útil quando API falhar")
        void deveUsarUltimaCotacaoUtilQuandoApiFalhar() {
            // given
            CotacaoHistorico ultimaCotacao = TestDataBuilder.criarCotacaoHistorico(
                    TestDataBuilder.MOEDA_PADRAO,
                    new BigDecimal("4.50"),
                    false
            );

            when(cotacaoHistoricoRepository.findUltimaCotacaoUtil(TestDataBuilder.MOEDA_PADRAO))
                    .thenReturn(Optional.of(ultimaCotacao));
            when(restTemplate.getForObject(anyString(), any()))
                    .thenThrow(new RestClientException("Erro na API"));

            // when
            BigDecimal cotacao = cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

            // then
            assertEquals(ultimaCotacao.getValor(), cotacao);
        }
    }

    @Nested
    @DisplayName("Testes de Validação")
    class ValidationTests {

        @Test
        @DisplayName("Deve falhar com moeda não suportada")
        void deveFalharComMoedaNaoSuportada() {
            assertThrows(IllegalArgumentException.class,
                    () -> cotacaoService.obterCotacao("EUR"));
        }

        @Test
        @DisplayName("Deve falhar com moeda nula")
        void deveFalharComMoedaNula() {
            assertThrows(IllegalArgumentException.class,
                    () -> cotacaoService.obterCotacao(null));
        }
    }

    @Test
    @DisplayName("Deve limpar cache corretamente")
    void deveLimparCacheCorretamente() {
        // given
        when(restTemplate.getForObject(anyString(), eq(PTAXResponse.class)))
                .thenReturn(TestDataBuilder.criarPTAXResponsePadrao());

        // when
        cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);
        cotacaoService.limparCache();
        cotacaoService.obterCotacao(TestDataBuilder.MOEDA_PADRAO);

        // then
        verify(restTemplate, times(2)).getForObject(anyString(), any());
    }
}