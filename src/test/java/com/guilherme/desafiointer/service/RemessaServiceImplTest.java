package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.config.TestConfig;
import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.service.impl.RemessaServiceImpl;
import com.guilherme.desafiointer.service.processor.RemessaProcessor;
import com.guilherme.desafiointer.service.testdata.TestDataBuilder;
import com.guilherme.desafiointer.service.validator.RemessaValidator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@DisplayName("Testes do RemessaService")
class RemessaServiceImplTest {

    @Autowired
    private RemessaServiceImpl remessaService;

    @MockBean
    private RemessaProcessor remessaProcessor;

    @MockBean
    private RemessaValidator remessaValidator;

    @Autowired
    private CacheManager cacheManager;

    private Usuario remetente;
    private Usuario destinatario;
    private RemessaRequestDTO remessaPadrao;

    @BeforeEach
    void setUp() {
        remetente = TestDataBuilder.criarRemetentePadrao();
        destinatario = TestDataBuilder.criarDestinatarioPadrao();
        remessaPadrao = TestDataBuilder.criarRemessaPadrao(remetente.getId(), destinatario.getId());

        doNothing().when(remessaValidator).validarDadosRemessa(any(RemessaRequestDTO.class));
    }

    @Nested
    @DisplayName("Testes de realização de remessa")
    class RealizacaoRemessaTests {

        @Test
        @DisplayName("Deve testar valores monetários com diferentes escalas")
        void deveTestarValoresMonetariosComDiferentesEscalas() {
            // given
            RemessaRequestDTO remessaTest = RemessaRequestDTO.builder()
                    .usuarioId(remetente.getId())
                    .destinatarioId(destinatario.getId())
                    .valor(new BigDecimal("100.00"))
                    .moedaDestino(AppConstants.MOEDA_PADRAO)
                    .build();

            Remessa remessaProcessada = TestDataBuilder.criarRemessaProcessada(remessaTest, remetente, destinatario);
            when(remessaProcessor.processarRemessa(remessaTest)).thenReturn(remessaProcessada);

            // when
            Remessa resultado = remessaService.realizarRemessa(remessaTest);

            // then
            assertAll("Validação de cálculos monetários",
                    () -> assertEquals(
                            new BigDecimal("20.00"),
                            resultado.getValorConvertido().setScale(2, RoundingMode.HALF_UP),
                            "Conversão deve manter 2 casas decimais"
                    ),
                    () -> assertEquals(
                            new BigDecimal("2.00"),
                            resultado.getTaxa().setScale(2, RoundingMode.HALF_UP),
                            "Cálculo de taxa deve manter 2 casas decimais"
                    )
            );
        }

        @Test
        @DisplayName("Deve realizar remessa com sucesso")
        void deveRealizarRemessaComSucesso() {
            Remessa remessaEsperada = TestDataBuilder.criarRemessaProcessada(remessaPadrao, remetente, destinatario);
            when(remessaProcessor.processarRemessa(remessaPadrao)).thenReturn(remessaEsperada);

            Remessa resultado = remessaService.realizarRemessa(remessaPadrao);

            assertAll("Validação da remessa processada",
                    () -> assertNotNull(resultado),
                    () -> assertEquals(remessaEsperada, resultado),
                    () -> verify(remessaValidator).validarDadosRemessa(remessaPadrao),
                    () -> verify(remessaProcessor).processarRemessa(remessaPadrao)
            );
        }

        @Test
        @DisplayName("Deve lançar exceção quando validação falhar")
        void deveLancarExcecaoQuandoValidacaoFalhar() {
            RemessaException validationException = TestDataBuilder.criarExcecaoValidacao();
            doThrow(validationException).when(remessaValidator).validarDadosRemessa(remessaPadrao);

            RemessaException exception = assertThrows(RemessaException.class,
                    () -> remessaService.realizarRemessa(remessaPadrao));

            assertExcecaoRemessa(exception, RemessaErrorType.DADOS_INVALIDOS, TestDataBuilder.MENSAGEM_DADOS_INVALIDOS);
        }

        @Test
        @DisplayName("Deve lançar exceção quando destinatário não existir")
        void deveLancarExcecaoQuandoDestinatarioNaoExistir() {
            doThrow(TestDataBuilder.criarExcecaoUsuarioNaoEncontrado())
                    .when(remessaValidator).validarDadosRemessa(any());

            RemessaException exception = assertThrows(RemessaException.class,
                    () -> remessaService.realizarRemessa(remessaPadrao));

            assertExcecaoRemessa(exception, RemessaErrorType.USUARIO_NAO_ENCONTRADO, TestDataBuilder.MENSAGEM_USUARIO_NAO_ENCONTRADO);
        }

        @Test
        @DisplayName("Deve lançar exceção quando carteira não existir")
        void deveLancarExcecaoQuandoCarteiraNaoExistir() {
            when(remessaProcessor.processarRemessa(any()))
                    .thenThrow(TestDataBuilder.criarExcecaoCarteiraNaoEncontrada());

            RemessaException exception = assertThrows(RemessaException.class,
                    () -> remessaService.realizarRemessa(remessaPadrao));

            assertExcecaoRemessa(exception, RemessaErrorType.CARTEIRA_NAO_ENCONTRADA, TestDataBuilder.MENSAGEM_CARTEIRA_NAO_ENCONTRADA);
        }

        @Test
        @DisplayName("Deve propagar exceção de saldo insuficiente")
        void devePropararExcecaoSaldoInsuficiente() {
            when(remessaProcessor.processarRemessa(any()))
                    .thenThrow(new SaldoInsuficienteException(TestDataBuilder.MENSAGEM_SALDO_INSUFICIENTE));

            assertThrows(SaldoInsuficienteException.class,
                    () -> remessaService.realizarRemessa(remessaPadrao));
        }
    }

    @Nested
    @DisplayName("Testes de consulta de histórico")
    class ConsultaHistoricoTests {
        private LocalDateTime inicio;
        private LocalDateTime fim;
        private Pageable pageable;

        @BeforeEach
        void setup() {
            inicio = LocalDateTime.now().minusDays(1);
            fim = LocalDateTime.now();
            pageable = PageRequest.of(0, 10);
        }

        @Test
        @DisplayName("Deve retornar histórico de transações")
        void deveRetornarHistoricoTransacoes() {
            Page<Remessa> paginaEsperada = TestDataBuilder.criarPaginaHistorico(remessaPadrao, remetente, destinatario);
            when(remessaProcessor.buscarHistorico(remetente, inicio, fim, pageable))
                    .thenReturn(paginaEsperada);

            Page<Remessa> resultado = remessaService.buscarHistoricoTransacoes(remetente, inicio, fim, pageable);

            assertEquals(paginaEsperada, resultado);
        }

        @Test
        @DisplayName("Deve validar período de consulta")
        void deveValidarPeriodoConsulta() {
            LocalDateTime inicioPosterior = LocalDateTime.now();
            LocalDateTime fimAnterior = LocalDateTime.now().minusDays(1);

            assertThrows(RemessaException.class,
                    () -> remessaService.buscarHistoricoTransacoes(
                            remetente, inicioPosterior, fimAnterior, pageable));
        }
    }

    private void assertExcecaoRemessa(RemessaException exception, RemessaErrorType tipoEsperado, String mensagemEsperada) {
        assertAll(
                () -> assertEquals(tipoEsperado, exception.getErrorType()),
                () -> assertTrue(exception.getMessage().contains(mensagemEsperada),
                        () -> "A mensagem deveria conter '" + mensagemEsperada + "' mas foi '" + exception.getMessage() + "'")
        );
    }
}