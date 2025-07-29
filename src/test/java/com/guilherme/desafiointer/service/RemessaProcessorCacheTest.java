package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.domain.TipoUsuario;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.repository.RemessaRepository;
import com.guilherme.desafiointer.service.impl.CotacaoServiceImpl;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import com.guilherme.desafiointer.service.processor.RemessaProcessorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.cache.type=simple",
        "spring.main.allow-bean-definition-overriding=true"
})
@ActiveProfiles("test")
class RemessaProcessorCacheTest {

    @Autowired
    private RemessaProcessorImpl remessaProcessor;

    @MockBean
    private CotacaoServiceImpl cotacaoService;

    @MockBean
    private RemessaRepository remessaRepository;

    @Autowired
    private CacheManager cacheManager;

    @Nested
    @DisplayName("Testes de Cache de Cotações")
    class CacheCotacoesTests {

        @BeforeEach
        void setUp() {
            // Limpa o cache antes de cada teste
            cacheManager.getCache(AppConstants.CACHE_COTACOES).clear();
            // Reseta e configura o mock
            reset(cotacaoService);
            when(cotacaoService.obterCotacao(anyString()))
                    .thenReturn(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("Deve usar cache ao obter cotação da mesma moeda")
        void deveUsarCacheAoObterCotacaoDaMesmaMoeda() {
            // when
            BigDecimal primeiroResultado = remessaProcessor.obterCotacao("USD");
            BigDecimal segundoResultado = remessaProcessor.obterCotacao("USD");

            // then
            assertAll(
                    () -> assertEquals(primeiroResultado, segundoResultado),
                    () -> verify(cotacaoService, times(1)).obterCotacao(anyString())
            );
        }

        @Test
        @DisplayName("Deve chamar serviço duas vezes quando cache é limpo")
        void deveChamarServicoDuasVezesQuandoCacheLimpo() {
            // when
            remessaProcessor.obterCotacao("USD");
            cacheManager.getCache(AppConstants.CACHE_COTACOES).clear();
            remessaProcessor.obterCotacao("USD");

            // then
            verify(cotacaoService, times(2)).obterCotacao(anyString());
        }
    }


    @Nested
    @DisplayName("Testes de Cache do Histórico")
    class CacheHistoricoTests {

        private Usuario usuario;
        private LocalDateTime inicio;
        private LocalDateTime fim;
        private PageRequest pageable;

        @BeforeEach
        void setUp() {
            reset(remessaRepository);

            usuario = Usuario.builder()
                    .id(1L)
                    .nomeCompleto("Teste")
                    .email("teste@teste.com")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("123.456.789-00")
                    .build();

            inicio = LocalDateTime.now().minusDays(7);
            fim = LocalDateTime.now();
            pageable = PageRequest.of(0, 10);

            when(remessaRepository.buscarHistoricoTransacoes(any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
        }

        @Test
        @DisplayName("Deve usar cache ao buscar histórico com mesmos parâmetros")
        void deveUsarCacheAoBuscarHistoricoComMesmosParametros() {
            remessaProcessor.buscarHistorico(usuario, inicio, fim, pageable);
            remessaProcessor.buscarHistorico(usuario, inicio, fim, pageable);

            verify(remessaRepository, times(1))
                    .buscarHistoricoTransacoes(usuario, inicio, fim, pageable);
        }

        @Test
        @DisplayName("Deve chamar repositório para períodos diferentes")
        void deveChamarRepositorioParaPeriodosDiferentes() {
            LocalDateTime novoFim = fim.plusDays(1);

            remessaProcessor.buscarHistorico(usuario, inicio, fim, pageable);
            remessaProcessor.buscarHistorico(usuario, inicio, novoFim, pageable);

            verify(remessaRepository, times(1))
                    .buscarHistoricoTransacoes(usuario, inicio, fim, pageable);
            verify(remessaRepository, times(1))
                    .buscarHistoricoTransacoes(usuario, inicio, novoFim, pageable);
        }
    }
}