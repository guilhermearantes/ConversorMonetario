package com.guilherme.desafiointer.service;
import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.dto.RemessaDTO;
import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import com.guilherme.desafiointer.repository.RemessaRepository;
import com.guilherme.desafiointer.repository.TransacaoDiariaRepository;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import com.guilherme.desafiointer.service.processor.RemessaProcessorImpl;
import com.guilherme.desafiointer.service.strategy.LimiteDiarioValidator;
import com.guilherme.desafiointer.service.strategy.StrategyFactory;
import com.guilherme.desafiointer.service.strategy.TaxaStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do RemessaProcessor")
class RemessaProcessorImplTest {

    @Mock
    private CotacaoServiceInterface cotacaoService;
    @Mock
    private RemessaRepository remessaRepository;
    @Mock
    private CarteiraRepository carteiraRepository;
    @Mock
    private TransacaoDiariaRepository transacaoDiariaRepository;
    @Mock
    private StrategyFactory strategyFactory;
    @Mock
    private TaxaStrategy taxaStrategy;
    @Mock
    private LimiteDiarioValidator limiteDiarioValidator;

    @InjectMocks
    private RemessaProcessorImpl remessaProcessor;

    private static final BigDecimal SALDO_INICIAL = new BigDecimal("1000.00");
    private static final BigDecimal VALOR_REMESSA = new BigDecimal("100.00");
    private static final BigDecimal TAXA = new BigDecimal("2.00");
    private static final BigDecimal COTACAO = new BigDecimal("5.00");
    private static final String MOEDA_DESTINO = "USD";

    private Usuario usuarioRemetente;
    private Usuario usuarioDestinatario;
    private Carteira carteiraRemetente;
    private Carteira carteiraDestinatario;
    private RemessaDTO remessaDTO;

    @BeforeEach
    void setUp() {
        usuarioRemetente = criarUsuario(1L, "Remetente");
        usuarioDestinatario = criarUsuario(2L, "Destinatario");
        carteiraRemetente = criarCarteira(usuarioRemetente, SALDO_INICIAL);
        carteiraDestinatario = criarCarteira(usuarioDestinatario, BigDecimal.ZERO);
        remessaDTO = criarRemessaDTO();
    }

    @Nested
    @DisplayName("Testes de Processamento de Remessa")
    class ProcessamentoRemessaTests {
        @Test
        @DisplayName("Deve processar remessa com sucesso")
        void deveProcessarRemessaComSucesso() {
            // Configurar apenas os mocks necessários para este teste específico
            configurarMocksProcessamentoSucesso();

            Remessa remessa = remessaProcessor.processarRemessa(remessaDTO);

            assertNotNull(remessa);
            assertEquals(VALOR_REMESSA, remessa.getValor());
            assertEquals(usuarioRemetente, remessa.getUsuario());
            assertEquals(usuarioDestinatario, remessa.getDestinatario());
            verify(carteiraRepository, times(2)).save(any(Carteira.class));
        }

        @Test
        @DisplayName("Deve falhar quando saldo é insuficiente")
        void deveFalharQuandoSaldoInsuficiente() {
            // Arrange
            carteiraRemetente = criarCarteira(usuarioRemetente, new BigDecimal("50.00"));

            // Configurar todos os mocks necessários
            when(carteiraRepository.findByUsuarioIdWithPessimisticLock(1L))
                    .thenReturn(Optional.of(carteiraRemetente));
            when(carteiraRepository.findByUsuarioIdWithPessimisticLock(2L))
                    .thenReturn(Optional.of(carteiraDestinatario));
            when(strategyFactory.getTaxaStrategy(any())).thenReturn(taxaStrategy);
            when(taxaStrategy.calcularTaxa(any())).thenReturn(TAXA);

            // Configurar mock para transação diária
            TransacaoDiaria transacaoDiaria = TransacaoDiaria.builder()
                    .usuario(usuarioRemetente)
                    .data(LocalDate.now())
                    .valorTotal(BigDecimal.ZERO)
                    .build();

            when(transacaoDiariaRepository.findByUsuarioAndData(any(), any()))
                    .thenReturn(Optional.of(transacaoDiaria));
            when(strategyFactory.getLimiteValidator(any())).thenReturn(limiteDiarioValidator);

            // Act & Assert
            assertThrows(SaldoInsuficienteException.class,
                    () -> remessaProcessor.processarRemessa(remessaDTO));
        }

    }

    @Nested
    @DisplayName("Testes de Cálculos")
    class CalculosTests {

        @BeforeEach
        void setup() {
            configurarMocksProcessamentoSucesso();
        }

        @Test
        @DisplayName("Deve calcular valor convertido corretamente")
        void deveCalcularValorConvertidoCorretamente() {
            // Act
            Remessa remessa = remessaProcessor.processarRemessa(remessaDTO);

            // Assert
            BigDecimal valorConvertidoEsperado = VALOR_REMESSA.divide(COTACAO, 2, RoundingMode.HALF_UP);
            assertEquals(valorConvertidoEsperado, remessa.getValorConvertido());
        }

        @Test
        @DisplayName("Deve calcular taxa corretamente")
        void deveCalcularTaxaCorretamente() {
            // Act
            Remessa remessa = remessaProcessor.processarRemessa(remessaDTO);

            // Assert
            assertEquals(TAXA, remessa.getTaxa());
            verify(taxaStrategy).calcularTaxa(VALOR_REMESSA);
        }
    }

    @Nested
    @DisplayName("Testes de Validação")
    class ValidacaoTests {

        @Test
        @DisplayName("Deve falhar quando carteira remetente não existe")
        void deveFalharQuandoCarteiraRemetenteNaoExiste() {
            // Arrange
            when(carteiraRepository.findByUsuarioIdWithPessimisticLock(1L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            RemessaException exception = assertThrows(RemessaException.class,
                    () -> remessaProcessor.processarRemessa(remessaDTO));
            assertEquals(RemessaErrorType.CARTEIRA_NAO_ENCONTRADA, exception.getErrorType());
        }

        @Test
        @DisplayName("Deve falhar quando cotação é inválida")
        void deveFalharQuandoCotacaoInvalida() {
            // Arrange
            configurarMocksBasicos();
            when(cotacaoService.obterCotacao(MOEDA_DESTINO))
                    .thenReturn(BigDecimal.ZERO);

            // Act & Assert
            RemessaException exception = assertThrows(RemessaException.class,
                    () -> remessaProcessor.processarRemessa(remessaDTO));
            assertEquals(RemessaErrorType.ERRO_COTACAO, exception.getErrorType());
        }
    }

    private void configurarMocksProcessamentoSucesso() {
        when(carteiraRepository.findByUsuarioIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(carteiraRemetente));
        when(carteiraRepository.findByUsuarioIdWithPessimisticLock(2L))
                .thenReturn(Optional.of(carteiraDestinatario));
        when(strategyFactory.getTaxaStrategy(any())).thenReturn(taxaStrategy);
        when(taxaStrategy.calcularTaxa(any())).thenReturn(TAXA);
        when(cotacaoService.obterCotacao(MOEDA_DESTINO)).thenReturn(COTACAO);
        when(remessaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Configuração necessária para o limite diário
        TransacaoDiaria transacaoDiaria = TransacaoDiaria.builder()
                .usuario(usuarioRemetente)
                .data(LocalDate.now())
                .valorTotal(BigDecimal.ZERO)
                .build();

        when(transacaoDiariaRepository.findByUsuarioAndData(any(), any()))
                .thenReturn(Optional.of(transacaoDiaria));
        when(strategyFactory.getLimiteValidator(any())).thenReturn(limiteDiarioValidator);
    }


    private void configurarMocksBasicos() {
        when(carteiraRepository.findByUsuarioIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(carteiraRemetente));
        when(carteiraRepository.findByUsuarioIdWithPessimisticLock(2L))
                .thenReturn(Optional.of(carteiraDestinatario));
        when(strategyFactory.getTaxaStrategy(any())).thenReturn(taxaStrategy);
        when(strategyFactory.getLimiteValidator(any())).thenReturn(limiteDiarioValidator);
        when(taxaStrategy.calcularTaxa(any())).thenReturn(TAXA);

        TransacaoDiaria transacaoDiaria = TransacaoDiaria.builder()
                .usuario(usuarioRemetente)
                .data(LocalDate.now())
                .valorTotal(BigDecimal.ZERO)
                .build();

        when(transacaoDiariaRepository.findByUsuarioAndData(any(), any()))
                .thenReturn(Optional.of(transacaoDiaria));
    }

    private Usuario criarUsuario(Long id, String nome) {
        return Usuario.builder()
                .id(id)
                .nomeCompleto(nome)
                .email(nome.toLowerCase() + "@teste.com")
                .tipoUsuario(TipoUsuario.PF)
                .documento("123.456.789-00")
                .build();
    }

    private Carteira criarCarteira(Usuario usuario, BigDecimal saldo) {
        return Carteira.builder()
                .id(usuario.getId())
                .usuario(usuario)
                .saldo(saldo)
                .build();
    }

    private RemessaDTO criarRemessaDTO() {
        return RemessaDTO.builder()
                .usuarioId(1L)
                .destinatarioId(2L)
                .valor(VALOR_REMESSA)
                .moedaDestino(MOEDA_DESTINO)
                .build();
    }
}