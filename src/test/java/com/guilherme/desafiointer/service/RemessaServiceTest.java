package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.config.TestConfig;
import com.guilherme.desafiointer.domain.Carteira;
import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.TipoUsuario;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.RemessaDTO;
import com.guilherme.desafiointer.exception.LimiteDiarioExcedidoException;
import com.guilherme.desafiointer.exception.SaldoInsuficienteException;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("Testes do RemessaService")
class RemessaServiceTest extends TestBase {

    private static final BigDecimal COTACAO_MOCK = new BigDecimal("5.00");
    private static final String MOEDA_DESTINO = "USD";
    private static final BigDecimal VALOR_REMESSA_PADRAO = new BigDecimal("100.00");
    private static final BigDecimal SALDO_ALTO = new BigDecimal("20000.00");
    private static final BigDecimal VALOR_LIMITE_EXCEDIDO = new BigDecimal("10001.00");

    @Autowired
    private RemessaService remessaService;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @MockBean
    private CotacaoService cotacaoService;

    private Usuario usuarioPF;

    @BeforeEach
    void setUp() {
        limparBancoDados();
        when(cotacaoService.obterCotacao(anyString())).thenReturn(COTACAO_MOCK);
        usuarioPF = criarEPersistirUsuarioPF();
    }

    @Nested
    @DisplayName("Testes de realização de remessa")
    class RealizacaoRemessaTests {

        @Test
        @DisplayName("Deve realizar remessa com sucesso")
        void deveRealizarRemessaComSucesso() {
            // Arrange
            RemessaDTO remessaDTO = criarRemessaDTO(usuarioPF.getId(), VALOR_REMESSA_PADRAO, MOEDA_DESTINO);

            // Act
            Remessa remessa = remessaService.realizarRemessa(remessaDTO);

            // Assert
            assertRemessaCriada(remessa, VALOR_REMESSA_PADRAO);
        }

        @Test
        @DisplayName("Deve falhar quando usuário não existe")
        void deveFalharQuandoUsuarioNaoExiste() {
            // Arrange
            RemessaDTO remessaDTO = criarRemessaDTO(999L, VALOR_REMESSA_PADRAO, MOEDA_DESTINO);

            // Act & Assert
            assertThrows(EntityNotFoundException.class,
                    () -> remessaService.realizarRemessa(remessaDTO),
                    "Deve lançar exceção quando usuário não existe");
        }

        @Test
        @DisplayName("Deve falhar quando saldo é insuficiente")
        void deveFalharQuandoSaldoInsuficiente() {
            // Arrange
            BigDecimal valorAlto = new BigDecimal("1500.00");
            RemessaDTO remessaDTO = criarRemessaDTO(usuarioPF.getId(), valorAlto, MOEDA_DESTINO);

            // Act & Assert
            var exception = assertThrows(
                    SaldoInsuficienteException.class,
                    () -> remessaService.realizarRemessa(remessaDTO)
            );
            assertEquals("Saldo insuficiente para realizar a remessa", exception.getMessage());
        }

        @Test
        @DisplayName("Deve falhar quando excede limite diário")
        void deveFalharQuandoExcedeLimiteDiario() {
            // Arrange
            atualizarSaldoCarteira(usuarioPF, SALDO_ALTO);
            RemessaDTO remessaDTO = criarRemessaDTO(usuarioPF.getId(), VALOR_LIMITE_EXCEDIDO, MOEDA_DESTINO);

            // Act & Assert
            assertThrows(LimiteDiarioExcedidoException.class,
                    () -> remessaService.realizarRemessa(remessaDTO),
                    "Deve lançar exceção quando o valor excede o limite diário");
        }
    }

    @Nested
    @DisplayName("Testes de consulta de histórico")
    class ConsultaHistoricoTests {

        private LocalDateTime inicio;
        private LocalDateTime fim;

        @BeforeEach
        void prepararDados() {
            inicio = LocalDateTime.now().minusDays(1);
            fim = LocalDateTime.now().plusDays(1);
            realizarRemessasTeste();
        }

        @Test
        @DisplayName("Deve retornar histórico de transações")
        void deveRetornarHistoricoTransacoes() {
            // Arrange
            PageRequest pageRequest = PageRequest.of(0, 10);

            // Act
            Page<Remessa> historico = remessaService.buscarHistoricoTransacoes(
                    usuarioPF,
                    inicio,
                    fim,
                    pageRequest
            );

            // Assert
            assertHistoricoTransacoes(historico);
        }

        @Test
        @DisplayName("Deve calcular total enviado no período")
        void deveCalcularTotalEnviado() {
            // Act
            BigDecimal totalEnviado = remessaService.calcularTotalEnviado(usuarioPF, inicio, fim);

            // Assert
            assertEquals(new BigDecimal("300.00"), totalEnviado);
        }

        @Test
        @DisplayName("Deve calcular total de taxas no período")
        void deveCalcularTotalTaxas() {
            // Act
            BigDecimal totalTaxas = remessaService.calcularTotalTaxas(usuarioPF, inicio, fim);

            // Assert
            assertEquals(new BigDecimal("6.00"), totalTaxas);
        }
    }

    // Métodos auxiliares
    private void assertRemessaCriada(Remessa remessa, BigDecimal valorRemessa) {
        BigDecimal taxaEsperada = TipoUsuario.PF.calcularTaxa(valorRemessa);
        BigDecimal saldoEsperado = SALDO_PADRAO.subtract(valorRemessa.add(taxaEsperada));

        assertAll("Verificações da remessa",
                () -> assertNotNull(remessa.getId(), "ID da remessa deve ser gerado"),
                () -> assertEquals(usuarioPF, remessa.getUsuario(), "Usuário da remessa deve ser correto"),
                () -> assertEquals(valorRemessa, remessa.getValor(), "Valor da remessa deve ser correto"),
                () -> assertEquals(taxaEsperada, remessa.getTaxa(), "Taxa calculada deve ser correta"),
                () -> assertEquals(COTACAO_MOCK, remessa.getCotacao(), "Cotação deve ser correta"),
                () -> assertEquals(MOEDA_DESTINO, remessa.getMoedaDestino(), "Moeda de destino deve ser correta"),
                () -> assertEquals(saldoEsperado, remessa.getUsuario().getCarteira().getSaldo(), "Saldo final deve ser correto"),
                () -> assertNotNull(remessa.getDataCriacao(), "Data de criação deve ser preenchida")
        );
    }

    private void assertHistoricoTransacoes(Page<Remessa> historico) {
        assertAll("Verificações do histórico",
                () -> assertFalse(historico.isEmpty(), "Histórico não deve estar vazio"),
                () -> assertEquals(2, historico.getTotalElements(), "Deve conter duas transações")
        );
    }

    private void atualizarSaldoCarteira(Usuario usuario, BigDecimal novoSaldo) {
        Carteira carteira = carteiraRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Carteira não encontrada"));
        carteira.definirSaldo(novoSaldo);
        carteiraRepository.save(carteira);
    }

    private void realizarRemessasTeste() {
        remessaService.realizarRemessa(criarRemessaDTO(usuarioPF.getId(), VALOR_REMESSA_PADRAO, MOEDA_DESTINO));
        remessaService.realizarRemessa(criarRemessaDTO(usuarioPF.getId(), new BigDecimal("200.00"), "EUR"));
    }
}