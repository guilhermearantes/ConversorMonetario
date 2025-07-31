package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.config.TestConfig;
import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import com.guilherme.desafiointer.repository.RemessaRepository;
import com.guilherme.desafiointer.repository.TransacaoDiariaRepository;
import com.guilherme.desafiointer.service.impl.RemessaServiceImpl;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import com.guilherme.desafiointer.service.processor.RemessaProcessorImpl;
import com.guilherme.desafiointer.service.strategy.LimiteDiarioValidator;
import com.guilherme.desafiointer.service.strategy.StrategyFactory;
import com.guilherme.desafiointer.service.strategy.TaxaStrategy;
import com.guilherme.desafiointer.service.testdata.TestDataBuilder;
import com.guilherme.desafiointer.service.validator.RemessaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.cache.type=simple",
        "spring.main.allow-bean-definition-overriding=true"
})
@ActiveProfiles("test")
@Import(TestConfig.class)
class RemessaProcessorCacheTest {

    @Autowired
    private RemessaProcessorImpl remessaProcessor;

    @Autowired
    private RemessaServiceImpl remessaService;

    @MockBean
    private CotacaoServiceInterface cotacaoService;

    @MockBean
    private RemessaRepository remessaRepository;

    @MockBean
    private CarteiraRepository carteiraRepository;

    @MockBean
    private TransacaoDiariaRepository transacaoDiariaRepository;

    @MockBean
    private StrategyFactory strategyFactory;

    @MockBean
    private RemessaValidator remessaValidator;

    @Autowired
    private CacheManager cacheManager;

    private Usuario remetente;
    private Usuario destinatario;
    private RemessaRequestDTO remessaPadrao;
    private Carteira carteiraRemetente;
    private Carteira carteiraDestinatario;

    @BeforeEach
    void setUp() {
        // Configurar usuários e carteiras
        remetente = TestDataBuilder.criarRemetentePadrao();
        destinatario = TestDataBuilder.criarDestinatarioPadrao();
        remessaPadrao = TestDataBuilder.criarRemessaPadrao(remetente.getId(), destinatario.getId());

        carteiraRemetente = Carteira.builder()
                .id(1L)
                .usuario(remetente)
                .saldoBRL(new BigDecimal("1000.00"))
                .saldoUSD(new BigDecimal("500.00"))
                .build();

        carteiraDestinatario = Carteira.builder()
                .id(2L)
                .usuario(destinatario)
                .saldoBRL(new BigDecimal("2000.00"))
                .saldoUSD(new BigDecimal("1000.00"))
                .build();

        // Configurar mocks para carteiras
        when(carteiraRepository.findByUsuarioIdWithPessimisticLock(remetente.getId()))
                .thenReturn(Optional.of(carteiraRemetente));
        when(carteiraRepository.findByUsuarioIdWithPessimisticLock(destinatario.getId()))
                .thenReturn(Optional.of(carteiraDestinatario));

        // Configurar mocks para cotação
        when(cotacaoService.obterCotacao(anyString()))
                .thenReturn(new BigDecimal("5.00"));

        // Configurar mocks para transações diárias
        TransacaoDiaria transacaoDiaria = TransacaoDiaria.builder()
                .usuario(remetente)
                .data(LocalDate.now())
                .valorTotal(BigDecimal.ZERO)
                .build();
        when(transacaoDiariaRepository.findByUsuarioAndData(any(), any()))
                .thenReturn(Optional.of(transacaoDiaria));

        // Configurar mocks para estratégias de taxa e limite
        TaxaStrategy taxaStrategy = mock(TaxaStrategy.class);
        when(taxaStrategy.calcularTaxa(any()))
                .thenReturn(new BigDecimal("2.00"));
        when(strategyFactory.getTaxaStrategy(any()))
                .thenReturn(taxaStrategy);

        LimiteDiarioValidator limiteValidator = mock(LimiteDiarioValidator.class);
        doNothing().when(limiteValidator).validar(any(), any(), any());
        when(strategyFactory.getLimiteValidator(any()))
                .thenReturn(limiteValidator);

        // Limpar caches antes de cada teste
        cacheManager.getCacheNames()
                .forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    }

    @Nested
    @DisplayName("Testes de Cache de Cotações")
    class CacheCotacoesTests {

        @Test
        @DisplayName("Deve usar cache ao obter cotação da mesma moeda")
        void deveUsarCacheAoObterCotacaoDaMesmaMoeda() {
            // given
            String moeda = AppConstants.MOEDA_PADRAO;
            BigDecimal cotacaoEsperada = new BigDecimal("5.00");
            when(cotacaoService.obterCotacao(moeda)).thenReturn(cotacaoEsperada);

            // when - primeira chamada
            BigDecimal primeiraCotacao = remessaProcessor.obterCotacao(moeda);

            // then - primeira chamada deve retornar valor correto
            assertEquals(cotacaoEsperada, primeiraCotacao);
            verify(cotacaoService, times(1)).obterCotacao(moeda);

            // when - segunda chamada (deve usar cache)
            BigDecimal segundaCotacao = remessaProcessor.obterCotacao(moeda);

            // then - segunda chamada deve retornar mesmo valor sem chamar serviço
            assertAll(
                    () -> assertEquals(cotacaoEsperada, segundaCotacao),
                    () -> verify(cotacaoService, times(1)).obterCotacao(moeda)
            );
        }

        @Test
        @DisplayName("Deve chamar serviço novamente quando cache é limpo")
        void deveChamarServicoNovamenteQuandoCacheLimpo() {
            // given
            String moeda = AppConstants.MOEDA_PADRAO;
            when(cotacaoService.obterCotacao(moeda))
                    .thenReturn(new BigDecimal("5.00"));

            // when - primeira chamada
            remessaProcessor.obterCotacao(moeda);

            // when - limpa cache e faz segunda chamada
            cacheManager.getCache(AppConstants.CACHE_COTACOES).clear();
            remessaProcessor.obterCotacao(moeda);

            // then
            verify(cotacaoService, times(2)).obterCotacao(moeda);
        }
    }

    @Nested
    @DisplayName("Testes de Cache do Service")
    class CacheServiceTests {

        @Test
        @DisplayName("Deve verificar presença dos caches após transação")
        void deveVerificarPresencaCachesAposTransacao() {
            // given
            RemessaRequestDTO remessaRequestDTO = TestDataBuilder.criarRemessaPadrao(
                    remetente.getId(),
                    destinatario.getId()
            );

            // when
            remessaProcessor.processarRemessa(remessaRequestDTO);

            // then
            assertAll(
                    () -> assertNotNull(cacheManager.getCache(AppConstants.CACHE_HISTORICO)),
                    () -> assertNotNull(cacheManager.getCache(AppConstants.CACHE_TOTAIS))
            );
        }

        @Test
        @DisplayName("Deve utilizar cache para cotações")
        void deveUtilizarCacheParaCotacoes() {
            // Limpar o cache antes do teste
            cacheManager.getCache(AppConstants.CACHE_COTACOES).clear();

            // given
            String moedaDestino = "USD";
            BigDecimal cotacaoEsperada = new BigDecimal("5.00");
            when(cotacaoService.obterCotacao(moedaDestino)).thenReturn(cotacaoEsperada);

            // when - primeira chamada
            BigDecimal primeiraCotacao = remessaProcessor.obterCotacao(moedaDestino);

            // when - segunda chamada (deve usar cache)
            BigDecimal segundaCotacao = remessaProcessor.obterCotacao(moedaDestino);

            // when - terceira chamada (ainda deve usar cache)
            BigDecimal terceiraCotacao = remessaProcessor.obterCotacao(moedaDestino);

            // then
            assertAll(
                    "Verificando comportamento do cache de cotações",
                    () -> assertEquals(cotacaoEsperada, primeiraCotacao, "Primeira cotação deve ser igual à esperada"),
                    () -> assertEquals(cotacaoEsperada, segundaCotacao, "Segunda cotação deve ser igual à esperada"),
                    () -> assertEquals(cotacaoEsperada, terceiraCotacao, "Terceira cotação deve ser igual à esperada"),
                    () -> verify(cotacaoService, times(1)).obterCotacao(moedaDestino) // Serviço deve ser chamado apenas uma vez
            );

            // when - após limpar o cache
            cacheManager.getCache(AppConstants.CACHE_COTACOES).clear();
            BigDecimal cotacaoAposLimparCache = remessaProcessor.obterCotacao(moedaDestino);

            // then
            assertAll(
                    "Verificando comportamento após limpar cache",
                    () -> assertEquals(cotacaoEsperada, cotacaoAposLimparCache, "Cotação após limpar cache deve ser igual à esperada"),
                    () -> verify(cotacaoService, times(2)).obterCotacao(moedaDestino) // Serviço deve ser chamado mais uma vez
            );
        }
    }

    @Nested
    @DisplayName("Testes de Cache do Histórico")
    class CacheHistoricoTests {

        private LocalDateTime inicio;
        private LocalDateTime fim;
        private PageRequest pageable;

        @BeforeEach
        void setUp() {
            inicio = LocalDateTime.now().minusDays(7);
            fim = LocalDateTime.now();
            pageable = PageRequest.of(0, 10);

            when(remessaRepository.buscarHistoricoTransacoes(any(), any(), any(), any()))
                    .thenReturn(Page.empty());
        }

        @Test
        @DisplayName("Deve usar cache ao buscar histórico com mesmos parâmetros")
        void deveUsarCacheAoBuscarHistoricoComMesmosParametros() {
            // when
            remessaProcessor.buscarHistorico(remetente, inicio, fim, pageable);
            remessaProcessor.buscarHistorico(remetente, inicio, fim, pageable);

            // then
            verify(remessaRepository, times(1))
                    .buscarHistoricoTransacoes(remetente, inicio, fim, pageable);
        }

        @Test
        @DisplayName("Deve ignorar cache para períodos diferentes")
        void deveIgnorarCacheParaPeriodosDiferentes() {
            // given
            LocalDateTime novoFim = fim.plusDays(1);

            // when
            remessaProcessor.buscarHistorico(remetente, inicio, fim, pageable);
            remessaProcessor.buscarHistorico(remetente, inicio, novoFim, pageable);

            // then
            verify(remessaRepository, times(1))
                    .buscarHistoricoTransacoes(remetente, inicio, fim, pageable);
            verify(remessaRepository, times(1))
                    .buscarHistoricoTransacoes(remetente, inicio, novoFim, pageable);
        }
    }
}