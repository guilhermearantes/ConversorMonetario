
package com.guilherme.desafiointer.integration;

import com.guilherme.desafiointer.config.TestConfig;
import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.exception.domain.LimiteDiarioExcedidoException;
import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import com.guilherme.desafiointer.repository.RemessaRepository;
import com.guilherme.desafiointer.repository.TransacaoDiariaRepository;
import com.guilherme.desafiointer.repository.UsuarioRepository;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import com.guilherme.desafiointer.service.interfaces.RemessaServiceInterface;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import com.guilherme.desafiointer.domain.*;
import org.junit.jupiter.api.*;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("Testes de Integração - Remessa com Saldos BRL e USD")
@Transactional
class RemessaIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RemessaServiceInterface remessaService;

    @MockBean
    private CotacaoServiceInterface cotacaoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private RemessaRepository remessaRepository;

    @Autowired
    private TransacaoDiariaRepository transacaoDiariaRepository;

    private Usuario usuarioRemetentePF;
    private Usuario usuarioRemetentePJ;
    private Usuario usuarioDestinatarioPF;
    private Usuario usuarioDestinatarioPJ;

    private static final BigDecimal SALDO_INICIAL_BRL_PF = new BigDecimal("10000.00");
    private static final BigDecimal SALDO_INICIAL_USD_PF = new BigDecimal("2000.00");
    private static final BigDecimal SALDO_INICIAL_BRL_PJ = new BigDecimal("50000.00");
    private static final BigDecimal SALDO_INICIAL_USD_PJ = new BigDecimal("10000.00");
    private static final BigDecimal COTACAO_PADRAO = new BigDecimal("5.00");

    @BeforeEach
    void setUp() {
        limparDados();
        criarUsuarios();
        configurarMockCotacao();
    }

    private void limparDados() {
        remessaRepository.deleteAllInBatch();
        transacaoDiariaRepository.deleteAllInBatch();
        carteiraRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();
    }

    private void criarUsuarios() {
        // Usuário PF remetente
        usuarioRemetentePF = criarUsuario("João Remetente PF", "remetente.pf@teste.com",
                "529.982.247-25", TipoUsuario.PF, SALDO_INICIAL_BRL_PF, SALDO_INICIAL_USD_PF);

        // Usuário PJ remetente
        usuarioRemetentePJ = criarUsuario("Empresa Remetente PJ", "remetente.pj@teste.com",
                "45.997.418/0001-53", TipoUsuario.PJ, SALDO_INICIAL_BRL_PJ, SALDO_INICIAL_USD_PJ);

        // Usuário PF destinatário
        usuarioDestinatarioPF = criarUsuario("Maria Destinatária PF", "destinatario.pf@teste.com",
                "248.438.034-80", TipoUsuario.PF, new BigDecimal("5000.00"), new BigDecimal("1000.00"));

        // Usuário PJ destinatário
        usuarioDestinatarioPJ = criarUsuario("Empresa Destinatária PJ", "destinatario.pj@teste.com",
                "12.345.678/0001-90", TipoUsuario.PJ, new BigDecimal("20000.00"), new BigDecimal("4000.00"));
    }

    private Usuario criarUsuario(String nome, String email, String documento,
                                 TipoUsuario tipo, BigDecimal saldoBRL, BigDecimal saldoUSD) {
        Usuario usuario = Usuario.builder()
                .nomeCompleto(nome)
                .email(email)
                .senha("Senha@123")
                .tipoUsuario(tipo)
                .documento(documento)
                .build();

        usuario = usuarioRepository.save(usuario);

        Carteira carteira = Carteira.builder()
                .saldoBRL(saldoBRL)
                .saldoUSD(saldoUSD)
                .usuario(usuario)
                .build();

        carteira = carteiraRepository.save(carteira);
        usuario.setCarteira(carteira);

        return usuarioRepository.save(usuario);
    }

    private void configurarMockCotacao() {
        when(cotacaoService.obterCotacao(anyString())).thenReturn(COTACAO_PADRAO);
    }

    @Nested
    @DisplayName("Testes de Remessa BRL para USD")
    class RemessaBRLParaUSDTests {

        @Test
        @DisplayName("Deve realizar remessa BRL para USD - PF para PF")
        void deveRealizarRemessaBRLParaUSDPFParaPF() {
            // Given
            BigDecimal valorRemessa = new BigDecimal("1000.00");
            RemessaRequestDTO remessaDTO = criarRemessaDTO(
                    usuarioRemetentePF.getId(),
                    usuarioDestinatarioPF.getId(),
                    valorRemessa,
                    "USD"
            );

            // When
            Remessa remessaRealizada = remessaService.realizarRemessa(remessaDTO);

            // Then
            assertAll("Validações da remessa BRL para USD",
                    () -> assertNotNull(remessaRealizada, "A remessa não deve ser nula"),
                    () -> assertEquals(valorRemessa, remessaRealizada.getValor(), "Valor da remessa deve estar correto"),
                    () -> assertEquals("USD", remessaRealizada.getMoedaDestino(), "Moeda de destino deve ser USD"),
                    () -> assertEquals(COTACAO_PADRAO, remessaRealizada.getCotacao(), "Cotação deve estar correta")
            );

            verificarSaldosAposRemessaBRLParaUSD(
                    usuarioRemetentePF,
                    usuarioDestinatarioPF,
                    valorRemessa,
                    remessaRealizada.getTaxa()
            );
        }

        @Test
        @DisplayName("Deve realizar remessa BRL para USD - PJ para PF")
        void deveRealizarRemessaBRLParaUSDPJParaPF() {
            // Given
            BigDecimal valorRemessa = new BigDecimal("5000.00");
            RemessaRequestDTO remessaDTO = criarRemessaDTO(
                    usuarioRemetentePJ.getId(),
                    usuarioDestinatarioPF.getId(),
                    valorRemessa,
                    "USD"
            );

            // When
            Remessa remessaRealizada = remessaService.realizarRemessa(remessaDTO);

            // Then
            verificarSaldosAposRemessaBRLParaUSD(
                    usuarioRemetentePJ,
                    usuarioDestinatarioPF,
                    valorRemessa,
                    remessaRealizada.getTaxa()
            );
        }

        private void verificarSaldosAposRemessaBRLParaUSD(Usuario remetente, Usuario destinatario,
                                                          BigDecimal valorRemessa, BigDecimal taxa) {
            // Buscar carteiras atualizadas
            Carteira carteiraRemetente = carteiraRepository.findByUsuarioId(remetente.getId())
                    .orElseThrow(() -> new AssertionError("Carteira remetente não encontrada"));

            Carteira carteiraDestinatario = carteiraRepository.findByUsuarioId(destinatario.getId())
                    .orElseThrow(() -> new AssertionError("Carteira destinatário não encontrada"));

            // Calcular valores esperados
            BigDecimal saldoBRLRemetenteEsperado = getSaldoInicialBRL(remetente)
                    .subtract(valorRemessa)
                    .subtract(taxa);

            BigDecimal valorConvertidoUSD = valorRemessa.divide(COTACAO_PADRAO, 2, RoundingMode.HALF_UP);
            BigDecimal saldoUSDDestinatarioEsperado = getSaldoInicialUSD(destinatario)
                    .add(valorConvertidoUSD);

            // Verificar saldos
            assertAll("Verificação dos saldos após remessa BRL para USD",
                    () -> assertEquals(saldoBRLRemetenteEsperado.setScale(2, RoundingMode.HALF_UP),
                            carteiraRemetente.getSaldoBRL().setScale(2, RoundingMode.HALF_UP),
                            "Saldo BRL do remetente incorreto"),
                    () -> assertEquals(saldoUSDDestinatarioEsperado.setScale(2, RoundingMode.HALF_UP),
                            carteiraDestinatario.getSaldoUSD().setScale(2, RoundingMode.HALF_UP),
                            "Saldo USD do destinatário incorreto")
            );
        }
    }

    @Nested
    @DisplayName("Testes de Remessa USD para BRL")
    class RemessaUSDParaBRLTests {


        @Test
        @DisplayName("Deve realizar remessa USD para BRL - PF para PF")
        void deveRealizarRemessaUSDParaBRLPFParaPF() {
            // Given - Valor em USD que será convertido para BRL
            BigDecimal valorRemessaUSD = new BigDecimal("200.00");

            RemessaRequestDTO remessaDTO = criarRemessaDTO(
                    usuarioRemetentePF.getId(),
                    usuarioDestinatarioPF.getId(),
                    valorRemessaUSD,
                    "BRL"  // CORRETO: Destino BRL = origem será USD
            );

            // When
            Remessa remessaRealizada = assertDoesNotThrow(() ->
                            remessaService.realizarRemessa(remessaDTO),
                    "Remessa USD para BRL deve ser executada com sucesso"
            );

            // Then - Validações da remessa
            assertAll("Validações da remessa USD para BRL",
                    () -> assertNotNull(remessaRealizada, "A remessa não deve ser nula"),
                    () -> assertEquals(valorRemessaUSD, remessaRealizada.getValor(),
                            "Valor da remessa deve estar em USD (moeda origem)"),
                    () -> assertEquals("BRL", remessaRealizada.getMoedaDestino(),
                            "Moeda de destino deve ser BRL"),
                    () -> assertEquals(COTACAO_PADRAO, remessaRealizada.getCotacao(),
                            "Cotação deve estar correta"),
                    () -> assertEquals(valorRemessaUSD.multiply(COTACAO_PADRAO).setScale(2, RoundingMode.HALF_UP),
                            remessaRealizada.getValorConvertido().setScale(2, RoundingMode.HALF_UP),
                            "Valor convertido deve ser valor USD × cotação")
            );

            // Verificar saldos alterados
            verificarSaldosAposRemessaUSDParaBRL(
                    usuarioRemetentePF,
                    usuarioDestinatarioPF,
                    valorRemessaUSD,
                    remessaRealizada.getTaxa()
            );
        }

        @Test
        @DisplayName("Deve realizar remessa USD para BRL - PJ para PF")
        void deveRealizarRemessaUSDParaBRLPJParaPF() {
            // Given - Valor em USD que será convertido para BRL
            BigDecimal valorRemessaUSD = new BigDecimal("500.00");

            RemessaRequestDTO remessaDTO = criarRemessaDTO(
                    usuarioRemetentePJ.getId(),
                    usuarioDestinatarioPF.getId(),
                    valorRemessaUSD,
                    "BRL"  // CORRETO: Destino BRL = origem será USD
            );

            // When
            Remessa remessaRealizada = assertDoesNotThrow(() ->
                            remessaService.realizarRemessa(remessaDTO),
                    "Remessa USD para BRL deve ser executada com sucesso"
            );

            // Then - Validações da remessa
            assertAll("Validações da remessa USD para BRL - PJ para PF",
                    () -> assertNotNull(remessaRealizada, "A remessa não deve ser nula"),
                    () -> assertEquals(valorRemessaUSD, remessaRealizada.getValor(),
                            "Valor da remessa deve estar em USD (moeda origem)"),
                    () -> assertEquals("BRL", remessaRealizada.getMoedaDestino(),
                            "Moeda de destino deve ser BRL"),
                    () -> assertEquals(COTACAO_PADRAO, remessaRealizada.getCotacao(),
                            "Cotação deve estar correta"),
                    () -> assertEquals(valorRemessaUSD.multiply(COTACAO_PADRAO).setScale(2, RoundingMode.HALF_UP),
                            remessaRealizada.getValorConvertido().setScale(2, RoundingMode.HALF_UP),
                            "Valor convertido deve ser valor USD × cotação")
            );

            // Verificar saldos alterados
            verificarSaldosAposRemessaUSDParaBRL(
                    usuarioRemetentePJ,
                    usuarioDestinatarioPF,
                    valorRemessaUSD,
                    remessaRealizada.getTaxa()
            );
        }

        private void verificarSaldosAposRemessaUSDParaBRL(Usuario remetente, Usuario destinatario,
                                                          BigDecimal valorRemessa, BigDecimal taxa) {
            // Buscar carteiras atualizadas do banco
            Carteira carteiraRemetente = carteiraRepository.findByUsuarioId(remetente.getId())
                    .orElseThrow(() -> new AssertionError("Carteira remetente não encontrada"));

            Carteira carteiraDestinatario = carteiraRepository.findByUsuarioId(destinatario.getId())
                    .orElseThrow(() -> new AssertionError("Carteira destinatário não encontrada"));

            // LÓGICA CORRETA: USD→BRL
            // Remetente: Debita USD (valor + taxa em USD)
            // Destinatário: Credita BRL (valor × cotação)

            BigDecimal saldoUSDRemetenteEsperado = getSaldoInicialUSD(remetente)
                    .subtract(valorRemessa)    // Debita valor em USD
                    .subtract(taxa);           // Debita taxa em USD

            BigDecimal valorConvertidoBRL = valorRemessa.multiply(COTACAO_PADRAO);
            BigDecimal saldoBRLDestinatarioEsperado = getSaldoInicialBRL(destinatario)
                    .add(valorConvertidoBRL);  // Credita em BRL

            // Verificações detalhadas
            assertAll("Verificação dos saldos após remessa USD→BRL",
                    () -> assertEquals(saldoUSDRemetenteEsperado.setScale(2, RoundingMode.HALF_UP),
                            carteiraRemetente.getSaldoUSD().setScale(2, RoundingMode.HALF_UP),
                            String.format("Saldo USD remetente incorreto!\n" +
                                            "   • Inicial USD: %s\n" +
                                            "   • Valor remessa: %s\n" +
                                            "   • Taxa: %s\n" +
                                            "   • Esperado: %s\n" +
                                            "   • Atual: %s",
                                    getSaldoInicialUSD(remetente), valorRemessa, taxa,
                                    saldoUSDRemetenteEsperado, carteiraRemetente.getSaldoUSD())),

                    () -> assertEquals(saldoBRLDestinatarioEsperado.setScale(2, RoundingMode.HALF_UP),
                            carteiraDestinatario.getSaldoBRL().setScale(2, RoundingMode.HALF_UP),
                            String.format("Saldo BRL destinatário incorreto!\n" +
                                            "   • Inicial BRL: %s\n" +
                                            "   • Valor convertido (USD×cotação): %s\n" +
                                            "   • Esperado: %s\n" +
                                            "   • Atual: %s",
                                    getSaldoInicialBRL(destinatario), valorConvertidoBRL,
                                    saldoBRLDestinatarioEsperado, carteiraDestinatario.getSaldoBRL())),

                    () -> assertEquals(getSaldoInicialBRL(remetente).setScale(2, RoundingMode.HALF_UP),
                            carteiraRemetente.getSaldoBRL().setScale(2, RoundingMode.HALF_UP),
                            "Saldo BRL do remetente deve permanecer inalterado"),

                    () -> assertEquals(getSaldoInicialUSD(destinatario).setScale(2, RoundingMode.HALF_UP),
                            carteiraDestinatario.getSaldoUSD().setScale(2, RoundingMode.HALF_UP),
                            "Saldo USD do destinatário deve permanecer inalterado")
            );
        }
    }

    @Nested
    @DisplayName("Testes de Validação de Saldo")
    class ValidacaoSaldoTests {

        @Test
        @DisplayName("Deve falhar quando saldo BRL insuficiente")
        void deveFalharQuandoSaldoBRLInsuficiente() {
            // Given - Criar usuário com saldo BRL MUITO baixo
            Usuario usuarioSaldoBaixo = criarUsuario(
                    "Usuario Saldo Baixo",
                    "saldo.baixo@teste.com",
                    "111.222.333-44",
                    TipoUsuario.PF,
                    new BigDecimal("5.00"),     // BRL muito baixo - menor que valor + taxa
                    new BigDecimal("5000.00")   // USD alto (não importa)
            );

            // Valor que excederá saldo BRL (10.00 + taxa ≈ 10.15 > 5.00)
            BigDecimal valorRemessa = new BigDecimal("10.00");

            RemessaRequestDTO remessaDTO = criarRemessaDTO(
                    usuarioSaldoBaixo.getId(),
                    usuarioDestinatarioPF.getId(),
                    valorRemessa,
                    "USD"  // A aplicação sempre debita BRL mesmo quando destino é USD
            );

            // When & Then
            assertThrows(SaldoInsuficienteException.class, () -> {
                remessaService.realizarRemessa(remessaDTO);
            }, "Deve lançar exceção de saldo insuficiente para BRL");
        }

        @Test
        @DisplayName("Deve falhar quando saldo USD insuficiente - Conversão USD para BRL")
        void deveFalharQuandoSaldoUSDInsuficiente() {
            // Given - Usuário com saldo USD muito baixo
            Usuario usuarioUSDaBaixo = criarUsuario(
                    "Usuario USD Baixo",
                    "usd.baixo@teste.com",
                    "555.666.777-88",
                    TipoUsuario.PF,
                    new BigDecimal("50000.00"), // BRL alto
                    new BigDecimal("1.00")      // USD muito baixo
            );

            // IMPORTANTE: Configurar remessa USD → BRL (não BRL → USD)
            BigDecimal valorRemessa = new BigDecimal("10.00"); // USD

            RemessaRequestDTO remessaDTO = criarRemessaDTO(
                    usuarioUSDaBaixo.getId(),
                    usuarioDestinatarioPF.getId(),
                    valorRemessa,
                    "BRL"  // ← CHAVE: Destino BRL força origem USD
            );

            // When & Then
            assertThrows(SaldoInsuficienteException.class, () -> {
                remessaService.realizarRemessa(remessaDTO);
            }, "Deve lançar exceção de saldo insuficiente para USD");
        }
    }

    @Nested
    @DisplayName("Testes de Limites Diários")
    class LimitesDiariosTests {

        @Test
        @DisplayName("Deve respeitar limite diário de PF (10.000 BRL)")
        void deveRespeitarLimiteDiarioPF() {
            // Given - Garantir saldo suficiente aumentando o saldo inicial do usuário PF
            Usuario usuarioComSaldoAlto = criarUsuario(
                    "João Grande Remetente",
                    "grande.remetente@teste.com",
                    "123.654.987-10",
                    TipoUsuario.PF,
                    new BigDecimal("25000.00"), // Saldo BRL alto o suficiente
                    new BigDecimal("5000.00")   // Saldo USD também alto
            );

            // Primeira remessa dentro do limite
            BigDecimal primeiraRemessa = new BigDecimal("6000.00");
            RemessaRequestDTO primeiraRemessaDTO = criarRemessaDTO(
                    usuarioComSaldoAlto.getId(),
                    usuarioDestinatarioPF.getId(),
                    primeiraRemessa,
                    "USD"
            );

            // When - Primeira remessa deve passar
            assertDoesNotThrow(() -> remessaService.realizarRemessa(primeiraRemessaDTO),
                    "Primeira remessa deve passar");

            // Given - Segunda remessa que estoura o limite diário
            // Total: 6000 + 5000 = 11000 > 10000 (limite PF)
            BigDecimal segundaRemessa = new BigDecimal("5000.00");
            RemessaRequestDTO segundaRemessaDTO = criarRemessaDTO(
                    usuarioComSaldoAlto.getId(),
                    usuarioDestinatarioPF.getId(),
                    segundaRemessa,
                    "USD"
            );

            // When & Then - Segunda remessa deve falhar por limite diário excedido
            LimiteDiarioExcedidoException exception = assertThrows(
                    LimiteDiarioExcedidoException.class,
                    () -> remessaService.realizarRemessa(segundaRemessaDTO),
                    "Deve lançar exceção de limite diário excedido para PF"
            );

            // Verificações adicionais
            assertAll("Validação da exceção de limite diário",
                    () -> assertTrue(exception.getMessage().contains("Limite diário") ||
                                    exception.getMessage().contains("10.000") ||
                                    exception.getMessage().contains("10000"),
                            "Mensagem deve mencionar limite diário ou valor do limite"),
                    () -> assertTrue(exception.getMessage().contains("PF") ||
                                    exception.getMessage().contains("Pessoa Física"),
                            "Mensagem deve mencionar tipo de usuário PF")
            );
        }

        @Test
        @DisplayName("Deve respeitar limite diário de PJ (50.000 BRL)")
        void deveRespeitarLimiteDiarioPJ() {
            // Given - Garantir saldo suficiente aumentando o saldo inicial do usuário PJ
            Usuario usuarioComSaldoAlto = criarUsuario(
                    "Empresa Grande Remetente",
                    "empresa.grande@teste.com",
                    "12.345.678/0001-99",
                    TipoUsuario.PJ,
                    new BigDecimal("120000.00"), // Saldo BRL alto o suficiente
                    new BigDecimal("25000.00")   // Saldo USD também alto
            );

            // Primeira remessa dentro do limite
            BigDecimal primeiraRemessa = new BigDecimal("30000.00");
            RemessaRequestDTO primeiraRemessaDTO = criarRemessaDTO(
                    usuarioComSaldoAlto.getId(),
                    usuarioDestinatarioPJ.getId(),
                    primeiraRemessa,
                    "USD"
            );

            // When - Primeira remessa deve passar
            assertDoesNotThrow(() -> remessaService.realizarRemessa(primeiraRemessaDTO),
                    "Primeira remessa deve passar");

            // Given - Segunda remessa que estoura o limite diário
            // Total: 30000 + 25000 = 55000 > 50000 (limite PJ)
            BigDecimal segundaRemessa = new BigDecimal("25000.00");
            RemessaRequestDTO segundaRemessaDTO = criarRemessaDTO(
                    usuarioComSaldoAlto.getId(),
                    usuarioDestinatarioPJ.getId(),
                    segundaRemessa,
                    "USD"
            );

            // When & Then - Segunda remessa deve falhar por limite diário excedido
            LimiteDiarioExcedidoException exception = assertThrows(
                    LimiteDiarioExcedidoException.class,
                    () -> remessaService.realizarRemessa(segundaRemessaDTO),
                    "Deve lançar exceção de limite diário excedido para PJ"
            );

            // Verificações adicionais
            assertAll("Validação da exceção de limite diário PJ",
                    () -> assertTrue(exception.getMessage().contains("Limite diário") ||
                                    exception.getMessage().contains("50.000") ||
                                    exception.getMessage().contains("50000"),
                            "Mensagem deve mencionar limite diário ou valor do limite"),
                    () -> assertTrue(exception.getMessage().contains("PJ") ||
                                    exception.getMessage().contains("Pessoa Jurídica"),
                            "Mensagem deve mencionar tipo de usuário PJ")
            );
        }
    }

    @Nested
    @DisplayName("Testes de Consistência Transacional")
    class ConsistenciaTransacionalTests {

        @Test
        @DisplayName("Deve manter consistência em operações concorrentes")
        void deveManterConsistenciaEmOperacoesConcorrentes() throws Exception {
            // Given
            int numThreads = 5;
            int operacoesPorThread = 2;
            BigDecimal valorPorOperacao = new BigDecimal("500.00");

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads * operacoesPorThread);

            // When - Executar operações concorrentes
            for (int i = 0; i < numThreads; i++) {
                for (int j = 0; j < operacoesPorThread; j++) {
                    executor.submit(() -> {
                        try {
                            RemessaRequestDTO remessaDTO = criarRemessaDTO(
                                    usuarioRemetentePF.getId(),
                                    usuarioDestinatarioPF.getId(),
                                    valorPorOperacao,
                                    "USD"
                            );
                            remessaService.realizarRemessa(remessaDTO);
                        } catch (Exception e) {
                            log.warn("Operação concorrente falhou: {}", e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }

            // Then - Aguardar conclusão
            assertTrue(latch.await(30, TimeUnit.SECONDS), "Operações devem terminar em 30 segundos");

            // Verificar consistência final
            Carteira carteira = carteiraRepository.findByUsuarioId(usuarioRemetentePF.getId())
                    .orElseThrow(() -> new AssertionError("Carteira não encontrada"));

            assertTrue(carteira.getSaldoBRL().compareTo(BigDecimal.ZERO) >= 0,
                    "Saldo BRL não deve ser negativo");
            assertTrue(carteira.getSaldoUSD().compareTo(BigDecimal.ZERO) >= 0,
                    "Saldo USD não deve ser negativo");
        }

        @Test
        @DisplayName("Deve fazer rollback em caso de erro")
        void deveFazerRollbackEmCasoDeErro() {
            // Given - Buscar saldos iniciais reais do banco
            Carteira carteiraInicial = carteiraRepository.findByUsuarioId(usuarioRemetentePF.getId())
                    .orElseThrow(() -> new AssertionError("Carteira não encontrada"));

            BigDecimal saldoBRLInicial = carteiraInicial.getSaldoBRL();
            BigDecimal saldoUSDInicial = carteiraInicial.getSaldoUSD();

            // When - Tentar remessa BRL->USD com valor que excede o saldo BRL disponível
            // O usuário PF tem 10.000 BRL inicial, então vamos usar 15.000 para garantir saldo insuficiente
            BigDecimal valorRemessaExcessivo = new BigDecimal("15000.00");
            RemessaRequestDTO remessaDTO = criarRemessaDTO(
                    usuarioRemetentePF.getId(),
                    usuarioDestinatarioPF.getId(),
                    valorRemessaExcessivo,
                    "USD"
            );

            // Then - Deve falhar por saldo insuficiente e manter saldos originais
            Exception exception = assertThrows(Exception.class, () ->
                            remessaService.realizarRemessa(remessaDTO),
                    "Deve lançar exceção por saldo insuficiente ou limite excedido"
            );

            // Verificar se a exceção é do tipo esperado
            assertTrue(
                    exception instanceof SaldoInsuficienteException ||
                            exception instanceof LimiteDiarioExcedidoException,
                    "Exceção deve ser de saldo insuficiente ou limite excedido, mas foi: " + exception.getClass().getSimpleName()
            );

            // Verificar que os saldos permanecem inalterados após o rollback
            Carteira carteiraFinal = carteiraRepository.findByUsuarioId(usuarioRemetentePF.getId())
                    .orElseThrow(() -> new AssertionError("Carteira não encontrada após erro"));

            assertAll("Verificação do rollback - saldos devem permanecer inalterados",
                    () -> assertEquals(saldoBRLInicial.setScale(2, RoundingMode.HALF_UP),
                            carteiraFinal.getSaldoBRL().setScale(2, RoundingMode.HALF_UP),
                            "Saldo BRL deve permanecer inalterado após erro"),
                    () -> assertEquals(saldoUSDInicial.setScale(2, RoundingMode.HALF_UP),
                            carteiraFinal.getSaldoUSD().setScale(2, RoundingMode.HALF_UP),
                            "Saldo USD deve permanecer inalterado após erro")
            );

            // Verificar também que nenhuma transação foi registrada
            Page<Remessa> historico = remessaRepository.buscarHistoricoTransacoes(
                    usuarioRemetentePF,
                    LocalDateTime.now().minusMinutes(5),
                    LocalDateTime.now().plusMinutes(1),
                    PageRequest.of(0, 10)
            );

            assertTrue(historico.isEmpty(),
                    "Não deve haver transações registradas após rollback");
        }
    }

    @Nested
    @DisplayName("Testes de Histórico de Transações")
    class HistoricoTransacoesTests {

        @Test
        @DisplayName("Deve registrar remessa no histórico")
        void deveRegistrarRemessaNoHistorico() {
            // Given
            BigDecimal valorRemessa = new BigDecimal("1000.00");
            RemessaRequestDTO remessaDTO = criarRemessaDTO(
                    usuarioRemetentePF.getId(),
                    usuarioDestinatarioPF.getId(),
                    valorRemessa,
                    "USD"
            );

            // When
            Remessa remessaRealizada = remessaService.realizarRemessa(remessaDTO);

            // Then
            Page<Remessa> historico = remessaRepository.buscarHistoricoTransacoes(
                    usuarioRemetentePF,
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusMinutes(1),
                    PageRequest.of(0, 10)
            );

            assertAll("Verificação do histórico",
                    () -> assertFalse(historico.isEmpty(), "Histórico não deve estar vazio"),
                    () -> assertTrue(
                            historico.getContent().stream()
                                    .anyMatch(r -> r.getId().equals(remessaRealizada.getId())),
                            "Remessa deve estar no histórico"
                    )
            );
        }
    }

    // Métodos auxiliares
    private RemessaRequestDTO criarRemessaDTO(Long usuarioId, Long destinatarioId,
                                              BigDecimal valor, String moedaDestino) {
        return RemessaRequestDTO.builder()
                .usuarioId(usuarioId)
                .destinatarioId(destinatarioId)
                .valor(valor)
                .moedaDestino(moedaDestino)
                .build();
    }

    private BigDecimal getSaldoInicialBRL(Usuario usuario) {
        // Usar os saldos iniciais baseados no tipo e se é o usuário criado no setUp
        if (usuario.getTipoUsuario() == TipoUsuario.PF) {
            if (usuario.equals(usuarioRemetentePF)) {
                return SALDO_INICIAL_BRL_PF; // 10.000
            } else if (usuario.equals(usuarioDestinatarioPF)) {
                return new BigDecimal("5000.00"); // Destinatário PF criado no setUp
            } else {
                // Para usuários criados dinamicamente nos testes, buscar saldo real
                return carteiraRepository.findByUsuarioId(usuario.getId())
                        .map(Carteira::getSaldoBRL)
                        .orElse(new BigDecimal("5000.00"));
            }
        } else { // PJ
            if (usuario.equals(usuarioRemetentePJ)) {
                return SALDO_INICIAL_BRL_PJ; // 50.000
            } else if (usuario.equals(usuarioDestinatarioPJ)) {
                return new BigDecimal("20000.00"); // Destinatário PJ criado no setUp
            } else {
                // Para usuários criados dinamicamente nos testes, buscar saldo real
                return carteiraRepository.findByUsuarioId(usuario.getId())
                        .map(Carteira::getSaldoBRL)
                        .orElse(new BigDecimal("20000.00"));
            }
        }
    }

    private BigDecimal getSaldoInicialUSD(Usuario usuario) {
        // Usar os saldos iniciais baseados no tipo e se é o usuário criado no setUp
        if (usuario.getTipoUsuario() == TipoUsuario.PF) {
            if (usuario.equals(usuarioRemetentePF)) {
                return SALDO_INICIAL_USD_PF; // 2.000
            } else if (usuario.equals(usuarioDestinatarioPF)) {
                return new BigDecimal("1000.00"); // Destinatário PF criado no setUp
            } else {
                // Para usuários criados dinamicamente nos testes, buscar saldo real
                return carteiraRepository.findByUsuarioId(usuario.getId())
                        .map(Carteira::getSaldoUSD)
                        .orElse(new BigDecimal("1000.00"));
            }
        } else { // PJ
            if (usuario.equals(usuarioRemetentePJ)) {
                return SALDO_INICIAL_USD_PJ; // 10.000
            } else if (usuario.equals(usuarioDestinatarioPJ)) {
                return new BigDecimal("4000.00"); // Destinatário PJ criado no setUp
            } else {
                // Para usuários criados dinamicamente nos testes, buscar saldo real
                return carteiraRepository.findByUsuarioId(usuario.getId())
                        .map(Carteira::getSaldoUSD)
                        .orElse(new BigDecimal("4000.00"));
            }
        }
    }
}